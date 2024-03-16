package me.char321.chunkcacher.mixin;

import com.mojang.datafixers.util.Either;
import me.char321.chunkcacher.WorldCache;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.*;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorageMixin {
    @Shadow
    @Final
    ServerWorld world;

    @Inject(method = "method_17225", at = @At("RETURN"), remap = false)
    private void addToCache(CallbackInfoReturnable<CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> cir) {
        if (WorldCache.shouldCache() && cir.getReturnValue().isDone()) {
            cir.getReturnValue().getNow(null).ifLeft((chunk) -> {
                if (!chunk.getStatus().isAtLeast(ChunkStatus.FEATURES)) {
                    WorldCache.addChunk(chunk.getPos(), chunk, world);
                }
            });
        }
    }

    @ModifyVariable(method = "getUpdatedChunkNbt", at = @At("STORE"))
    private NbtCompound loadFromCache(NbtCompound nbtCompound, ChunkPos pos) {
        if (WorldCache.shouldCache() && nbtCompound == null) {
            return WorldCache.getChunkNbt(pos, world);
        }
        return nbtCompound;
    }
}
