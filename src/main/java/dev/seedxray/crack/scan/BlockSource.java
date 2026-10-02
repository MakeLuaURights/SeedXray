package dev.seedxray.crack.scan;

import net.minecraft.block.Block;
import net.minecraft.util.Identifier;

/** Read access to the blocks the player's client currently has. */
public interface BlockSource {
	Block block(int x, int y, int z);

	boolean chunkLoaded(int chunkX, int chunkZ);

	/** True if the chunk at hand has a block of this type (cheap: looks at the section palettes only). */
	boolean chunkHas(int chunkX, int chunkZ, Block block);

	int minY();

	int maxY();

	/** Id of the biome at a block position. */
	Identifier biome(int x, int y, int z);
}
