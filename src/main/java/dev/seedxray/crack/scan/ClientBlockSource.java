package dev.seedxray.crack.scan;

import net.minecraft.block.Block;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

/** {@link BlockSource} over the client's world. Use on the client thread only. */
public final class ClientBlockSource implements BlockSource {
	private final ClientWorld world;
	private final BlockPos.Mutable pos = new BlockPos.Mutable();

	public ClientBlockSource(ClientWorld world) {
		this.world = world;
	}

	@Override
	public Block block(int x, int y, int z) {
		return world.getBlockState(pos.set(x, y, z)).getBlock();
	}

	@Override
	public boolean chunkLoaded(int chunkX, int chunkZ) {
		return world.getChunkManager().getWorldChunk(chunkX, chunkZ) != null;
	}

	@Override
	public boolean chunkHas(int chunkX, int chunkZ, Block block) {
		WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkX, chunkZ);
		if (chunk == null) return false;
		for (ChunkSection section : chunk.getSectionArray()) {
			if (section != null && !section.isEmpty() && section.hasAny(state -> state.isOf(block))) return true;
		}
		return false;
	}

	@Override
	public int minY() {
		return world.getBottomY();
	}

	@Override
	public int maxY() {
		return world.getTopYInclusive() + 1;
	}

	@Override
	public Identifier biome(int x, int y, int z) {
		return world.getBiome(pos.set(x, y, z)).getKey().map(k -> k.getValue()).orElse(Identifier.ofVanilla("none"));
	}
}
