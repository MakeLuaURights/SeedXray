package dev.seedxray.sim;

import net.minecraft.block.Block;

/** Ore blocks predicted for one chunk's feature pass (they may extend a few blocks into neighbouring chunks). */
public final class OreChunk {
	public final int chunkX;
	public final int chunkZ;
	/** Packed with {@link net.minecraft.util.math.BlockPos#asLong}. */
	public final long[] positions;
	public final Block[] blocks;

	OreChunk(int chunkX, int chunkZ, long[] positions, Block[] blocks) {
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		this.positions = positions;
		this.blocks = blocks;
	}

	public int size() {
		return positions.length;
	}
}
