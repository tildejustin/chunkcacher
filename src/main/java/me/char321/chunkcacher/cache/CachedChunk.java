package me.char321.chunkcacher.cache;

import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.carver.CarvingMask;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.tick.SimpleTickScheduler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class CachedChunk {
    public ChunkPos pos;
    @Nullable
    public Biome biome;
    public Map<Heightmap.Type, PaletteStorage> heightmaps;
    public ChunkStatus status;
    public NbtList blockEntities;
    public CachedChunkSection[] sections;
    public List<NbtCompound> entities;
    public List<BlockPos> lightSources;
    public ShortList[] postProcessingLists;
    public Map<StructureFeature<?>, NbtCompound> structureStarts;
    public Map<StructureFeature<?>, LongSet> structureReferences;
    public UpgradeData upgradeData;
    public SimpleTickScheduler<Block> blockTickScheduler;
    public SimpleTickScheduler<Fluid> fluidTickScheduler;
    public long inhabitedTime;
    public Map<GenerationStep.Carver, CarvingMask> carvingMasks = new Object2ObjectArrayMap<>();
    public boolean lightOn;

}
