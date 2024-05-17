package me.char321.chunkcacher.mixin.access;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.carver.CarvingMask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(ProtoChunk.class)
public interface ProtoChunkAccessor {
    @Accessor
    Map<GenerationStep.Carver, CarvingMask> getCarvingMasks();

    @Accessor
    List<BlockPos> getLightSources();
}
