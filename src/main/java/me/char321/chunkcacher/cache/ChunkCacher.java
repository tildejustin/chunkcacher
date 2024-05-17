package me.char321.chunkcacher.cache;

import com.google.common.collect.Maps;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.ShortList;
import me.char321.chunkcacher.mixin.access.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.*;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.carver.CarvingMask;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.tick.SimpleTickScheduler;
import net.minecraft.world.tick.Tick;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("UnreachableCode")
public class ChunkCacher {
    public static CachedChunk cache(ProtoChunk chunk, ServerWorld world) {
        /*
        pos immutable
        biomes immutable
        lightingProvider chunks aren't lit yet, is null
        heightmaps copied
        upgradeData should be NO_UPGRADE_DATA
         */
        CachedChunk res = new CachedChunk();

        res.biome = ((ChunkAccessor) chunk).getBiome();

        if (chunk.getUpgradeData() != UpgradeData.NO_UPGRADE_DATA) {
            throw new UnsupportedOperationException("caching upgrade data is not supported");
        }
        res.upgradeData = chunk.getUpgradeData();

        ChunkPos pos = chunk.getPos();
        res.pos = pos;

        CachedChunkSection[] sections = new CachedChunkSection[world.countVerticalSections()];
        for (int i = 0; i < world.countVerticalSections(); i++) {
            sections[i] = cacheChunkSection(chunk.getSectionArray()[i]);
        }
        res.sections = sections;

        res.blockTickScheduler = copy((SimpleTickScheduler<Block>) chunk.getBlockTickScheduler());
        res.fluidTickScheduler = copy((SimpleTickScheduler<Fluid>) chunk.getFluidTickScheduler());

        res.heightmaps = cacheHeightmaps(chunk.getHeightmaps());

        res.status = chunk.getStatus();

        res.blockEntities = cacheBlockEntities(chunk);

        res.entities = copy(chunk.getEntities());

        res.lightSources = chunk.getLightSourcesStream().collect(Collectors.toList());

        res.postProcessingLists = new ShortList[world.countVerticalSections()];
        for (int i = 0; i < world.countVerticalSections(); i++) {
            if (chunk.getPostProcessingLists()[i] != null) {
                Chunk.getList(res.postProcessingLists, i).addAll(chunk.getPostProcessingLists()[i]);
            }
        }

        StructureContext context = StructureContext.from(world);

        res.structureStarts = cacheStructureStarts(chunk.getStructureStarts(), pos, context);
        res.structureReferences = copyStructureReferences(chunk.getStructureReferences());

        res.inhabitedTime = chunk.getInhabitedTime();

        res.carvingMasks = copyCarvingMasks(((ProtoChunkAccessor) chunk).getCarvingMasks());

        res.lightOn = chunk.isLightOn();

        return res;
    }

    public static ProtoChunk retrieve(CachedChunk chunk, ServerWorld world) {
        if (chunk == null) return null;
        ChunkPos pos = chunk.pos;
        ChunkSection[] sections = new ChunkSection[world.countVerticalSections()];
        for (int i = 0; i < world.countVerticalSections(); i++) {
            sections[i] = retrieveChunkSection(chunk.sections[i], world);
        }
        if (chunk.upgradeData != UpgradeData.NO_UPGRADE_DATA) {
            throw new UnsupportedOperationException("caching upgrade data is not supported");
        }
        ProtoChunk res = new ProtoChunk(pos, chunk.upgradeData, sections, copy(chunk.blockTickScheduler), copy(chunk.fluidTickScheduler), world, world.getRegistryManager().get(Registry.BIOME_KEY), null);

        ((ChunkAccessor) res).setBiome(chunk.biome);

        ((ChunkAccessor) res).setHeightmaps(retrieveHeightmaps(chunk.heightmaps, res));

        res.setStatus(chunk.status);

        retrieveBlockEntities(chunk.blockEntities, res);

        res.getEntities().addAll(copy(chunk.entities));

        ((ProtoChunkAccessor) res).getLightSources().addAll(chunk.lightSources);

        for (int i = 0; i < world.countVerticalSections(); i++) {
            if (chunk.postProcessingLists[i] != null) {
                Chunk.getList(res.getPostProcessingLists(), i).addAll(chunk.postProcessingLists[i]);
            }
        }

        StructureContext context = StructureContext.from(world);

        res.setStructureStarts(retrieveStructureStarts(chunk.structureStarts, context, world.getSeed()));
        res.setStructureReferences(copyStructureReferences(chunk.structureReferences));

        res.setInhabitedTime(chunk.inhabitedTime);

        res.setLightOn(chunk.lightOn);

        for (Map.Entry<GenerationStep.Carver, CarvingMask> maskEntry : chunk.carvingMasks.entrySet()) {
            res.setCarvingMask(maskEntry.getKey(), new CarvingMask(maskEntry.getValue().getMask(), ((CarvingMaskAccessor) maskEntry.getValue()).getBottomY()));
        }

        return res;
    }

    private static @NotNull NbtList cacheBlockEntities(ProtoChunk chunk) {
        NbtList blockEntityList = new NbtList();
        for(BlockPos blockPos : chunk.getBlockEntityPositions()) {
            NbtCompound nbt = chunk.getPackedBlockEntityNbt(blockPos);
            if (nbt == null) {
                throw new IllegalArgumentException("invalid block entity nbt");
            }
            blockEntityList.add(nbt);
        }
        return blockEntityList;
    }

    private static void retrieveBlockEntities(NbtList blockEntities, ProtoChunk chunk) {
        for(int o = 0; o < blockEntities.size(); ++o) {
            NbtCompound blockEntityNbt = blockEntities.getCompound(o);
            chunk.addPendingBlockEntityNbt(blockEntityNbt);
        }
    }

    private static List<NbtCompound> copy(List<NbtCompound> src) {
        List<NbtCompound> res = new ArrayList<>(src.size());
        for(NbtCompound nbtCompound : src) {
            res.add(nbtCompound.copy());
        }
        return res;
    }

    private static <T> SimpleTickScheduler<T> copy(SimpleTickScheduler<T> src) {
        SimpleTickScheduler<T> res = new SimpleTickScheduler<>();
        // TODO: copy lists
        for (Tick<?> scheduledTick : ((SimpleTickSchedulerAccessor<T>) src).getScheduledTicksSet()) {
            ((SimpleTickSchedulerAccessor<T>) res).getScheduledTicksSet().add(scheduledTick);
        }
        for (Tick<T> scheduledTick : ((SimpleTickSchedulerAccessor<T>) src).getScheduledTicks()) {
            ((SimpleTickSchedulerAccessor<T>) res).getScheduledTicks().add(scheduledTick);
        }
        return res;
    }

    private static Map<Heightmap.Type, PaletteStorage> cacheHeightmaps(Collection<Map.Entry<Heightmap.Type, Heightmap>> heightmaps) {
        EnumMap<Heightmap.Type, PaletteStorage> res = new EnumMap<>(Heightmap.Type.class);
        for (Map.Entry<Heightmap.Type, Heightmap> entry : heightmaps) {
            res.put(entry.getKey(), cacheHeightmap(entry.getValue()));
        }
        return res;
    }

    private static Map<Heightmap.Type, Heightmap> retrieveHeightmaps(Map<Heightmap.Type, PaletteStorage> heightmaps, Chunk chunk) {
        EnumMap<Heightmap.Type, Heightmap> res = new EnumMap<>(Heightmap.Type.class);
        for (Map.Entry<Heightmap.Type, PaletteStorage> entry : heightmaps.entrySet()) {
            res.put(entry.getKey(), retrieveHeightmap(entry.getValue(), entry.getKey(), chunk));
        }
        return res;
    }

    private static PaletteStorage cacheHeightmap(Heightmap heightmap) {
        return copy(((HeightmapAccessor) heightmap).getStorage());
    }

    private static Heightmap retrieveHeightmap(PaletteStorage heightmap, Heightmap.Type type, Chunk chunk) {
        Heightmap res = new Heightmap(chunk, type);
        ((HeightmapAccessor) res).setStorage(copy(heightmap));
        return res;
    }

    private static PaletteStorage copy(PaletteStorage src) {
        int elementBits = src.getElementBits();
        int size = src.getSize();
        long[] storage = src.getData();
        long[] newstorage = Arrays.copyOf(storage, storage.length);
        return new PackedIntegerArray(elementBits, size, newstorage);
    }

    private static CachedChunkSection cacheChunkSection(ChunkSection src) {
        if (src == null) return null;
        CachedChunkSection res = new CachedChunkSection();
        res.nonEmptyBlockCount = ((ChunkSectionAccessor) src).getNonEmptyBlockCount();
        res.randomTickableBlockCount = ((ChunkSectionAccessor) src).getRandomTickableBlockCount();
        res.nonEmptyFluidCount = ((ChunkSectionAccessor) src).getNonEmptyFluidCount();
        res.yOffset = src.getYOffset();
        PalettedContainer<BlockState> blockStateContainer = src.getBlockStateContainer();
        PacketByteBuf blockStateBuf = new PacketByteBuf(Unpooled.buffer());
        blockStateContainer.writePacket(blockStateBuf);
        res.blockStateContainer = blockStateBuf;
        PalettedContainer<Biome> biomeContainer = src.getBiomeContainer();
        PacketByteBuf biomeBuf = new PacketByteBuf(Unpooled.buffer());
        biomeContainer.writePacket(biomeBuf);
        res.biomeContainer = biomeBuf;
        return res;
    }

    private static ChunkSection retrieveChunkSection(CachedChunkSection src, ServerWorld world) {
        if (src == null) return null;
        ChunkSection res = new ChunkSection(src.yOffset, world.getRegistryManager().get(Registry.BIOME_KEY));
        ((ChunkSectionAccessor) res).setNonEmptyBlockCount(src.nonEmptyBlockCount);
        ((ChunkSectionAccessor) res).setNonEmptyFluidCount(src.nonEmptyFluidCount);
        ((ChunkSectionAccessor) res).setRandomTickableBlockCount(src.randomTickableBlockCount);
        src.blockStateContainer.resetReaderIndex();
        res.getBlockStateContainer().readPacket(src.blockStateContainer);
        src.biomeContainer.resetReaderIndex();
        res.getBiomeContainer().readPacket(src.biomeContainer);
        return res;
    }

    private static Map<StructureFeature<?>, NbtCompound> cacheStructureStarts(Map<StructureFeature<?>, StructureStart<?>> structureStarts, ChunkPos pos, StructureContext context) {
        Map<StructureFeature<?>, NbtCompound> res = new HashMap<>();
        for (Map.Entry<StructureFeature<?>, StructureStart<?>> entry : structureStarts.entrySet()) {
            res.put(entry.getKey(), entry.getValue().toNbt(context, pos));
        }
        return res;
    }

    private static Map<StructureFeature<?>, StructureStart<?>> retrieveStructureStarts(Map<StructureFeature<?>, NbtCompound> structureStarts, StructureContext context, long worldSeed) {
        Map<StructureFeature<?>, StructureStart<?>> map = Maps.newHashMap();

        for(Map.Entry<StructureFeature<?>, NbtCompound> entry : structureStarts.entrySet()) {
            StructureFeature<?> structureFeature = entry.getKey();
            StructureStart<?> structureStart = StructureFeature.readStructureStart(context, entry.getValue(), worldSeed);
            if (structureStart == null) {
                throw new IllegalArgumentException();
            }
            map.put(structureFeature, structureStart);
        }

        return map;
    }

    private static Map<StructureFeature<?>, LongSet> copyStructureReferences(Map<StructureFeature<?>, LongSet> src) {
        Map<StructureFeature<?>, LongSet> res = new HashMap<>();
        for (Map.Entry<StructureFeature<?>, LongSet> entry : src.entrySet()) {
            LongSet longSet = entry.getValue();
            res.put(entry.getKey(), new LongOpenHashSet(longSet));
        }
        return res;
    }

    private static Map<GenerationStep.Carver, CarvingMask> copyCarvingMasks(Map<GenerationStep.Carver, CarvingMask> src) {
        Map<GenerationStep.Carver, CarvingMask> res = new Object2ObjectArrayMap<>();
        for (Map.Entry<GenerationStep.Carver, CarvingMask> entry : src.entrySet()) {
            GenerationStep.Carver carver = entry.getKey();
            int bottomY = ((CarvingMaskAccessor) entry.getValue()).getBottomY();
            long[] bitSet = entry.getValue().getMask();
            res.put(carver, new CarvingMask(bitSet, bottomY));
        }
        return res;
    }
}
