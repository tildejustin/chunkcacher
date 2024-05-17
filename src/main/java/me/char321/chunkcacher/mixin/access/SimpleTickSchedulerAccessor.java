package me.char321.chunkcacher.mixin.access;

import net.minecraft.world.tick.SimpleTickScheduler;
import net.minecraft.world.tick.Tick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Set;

@Mixin(SimpleTickScheduler.class)
public interface SimpleTickSchedulerAccessor<T> {
    @Accessor
    List<Tick<T>> getScheduledTicks();

    @Accessor
    Set<Tick<?>> getScheduledTicksSet();
}
