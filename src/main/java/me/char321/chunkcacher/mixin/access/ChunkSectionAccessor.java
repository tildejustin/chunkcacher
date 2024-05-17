package me.char321.chunkcacher.mixin.access;

import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkSection.class)
public interface ChunkSectionAccessor {
    @Accessor short getNonEmptyBlockCount();
    @Accessor void setNonEmptyBlockCount(short nonEmptyBlockCount);
    @Accessor short getRandomTickableBlockCount();
    @Accessor void setRandomTickableBlockCount(short randomTickableBlockCount);
    @Accessor short getNonEmptyFluidCount();
    @Accessor void setNonEmptyFluidCount(short nonEmptyFluidCount);
}
