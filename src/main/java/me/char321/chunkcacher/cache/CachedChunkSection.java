package me.char321.chunkcacher.cache;

import net.minecraft.network.PacketByteBuf;

public class CachedChunkSection {
    public int yOffset;
    public short nonEmptyBlockCount;
    public short randomTickableBlockCount;
    public short nonEmptyFluidCount;
    public PacketByteBuf blockStateContainer;
    public PacketByteBuf biomeContainer;
}
