package me.char321.chunkcacher.mixin.access;

import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(Chunk.class)
public interface ChunkAccessor {
    @Mutable
    @Accessor
    void setHeightmaps(Map<Heightmap.Type, Heightmap> heightmaps);

    @Accessor
    Biome getBiome();

    @Accessor
    void setBiome(Biome biome);
}
