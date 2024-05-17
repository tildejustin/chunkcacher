package me.char321.chunkcacher.mixin;

import com.mojang.datafixers.util.Either;
import me.char321.chunkcacher.cache.WorldCache;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.structure.StructureManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorageMixin {

    @Shadow @Final ServerWorld world;

    @Shadow @Final private PointOfInterestStorage pointOfInterestStorage;

    @Inject(method = "method_17225", at = @At("RETURN"), remap = false)
    private void addToCache(CallbackInfoReturnable<CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> cir) {
        if (WorldCache.shouldCache()) {
            cir.getReturnValue().thenApply(res -> {
                res.ifLeft((chunk) -> {
                    if (!chunk.getStatus().isAtLeast(ChunkStatus.FEATURES)) {
                        WorldCache.addChunk(chunk.getPos(), chunk.getStatus(), chunk, world);
                    }
                });
                return res;
            });
        }
    }

    @Redirect(method = "method_17225", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkStatus;runGenerationTask(Ljava/util/concurrent/Executor;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Lnet/minecraft/structure/StructureManager;Lnet/minecraft/server/world/ServerLightingProvider;Ljava/util/function/Function;Ljava/util/List;Z)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> loadFromCache(
            ChunkStatus instance,
            Executor executor,
            ServerWorld world,
            ChunkGenerator chunkGenerator,
            StructureManager structureManager,
            ServerLightingProvider lightingProvider,
            Function<Chunk, CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> function,
            List<Chunk> surroundingChunks,
            boolean bl
    ) {
        if (WorldCache.shouldCache() && !instance.isAtLeast(ChunkStatus.FEATURES)) {
            Chunk chunk = surroundingChunks.get(surroundingChunks.size() / 2);
            ProtoChunk cachedChunk = WorldCache.getChunk(chunk.getPos(), instance, world);
            if (cachedChunk != null) {
                return CompletableFuture.completedFuture(Either.left(cachedChunk));
            }
        }
        return instance.runGenerationTask(executor, world, chunkGenerator, structureManager, lightingProvider, function, surroundingChunks, bl);
    }
}
