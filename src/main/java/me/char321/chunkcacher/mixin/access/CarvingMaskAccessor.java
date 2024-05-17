package me.char321.chunkcacher.mixin.access;

import net.minecraft.world.gen.carver.CarvingMask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CarvingMask.class)
public interface CarvingMaskAccessor {
    @Accessor
    int getBottomY();
}
