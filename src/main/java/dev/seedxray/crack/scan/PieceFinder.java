package dev.seedxray.crack.scan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

/**
 * Looks for one structure layout, facing one direction. Ported from SeedcrackerX's {@code PieceFinder} and
 * {@code JigsawFinder} (MIT, https://github.com/19MisterX98/SeedcrackerX). The layout is a map of positions to block
 * types; a candidate origin matches when every one of them holds.
 */
public class PieceFinder {
	private final BlockBox box;
	final Map<BlockPos, Block> structure = new LinkedHashMap<>();
	List<BlockPos> searchPositions = new ArrayList<>();
	final Direction facing;
	final int width;
	final int height;
	final int depth;
	private final boolean jigsaw;

	public PieceFinder(Direction facing, Vec3i size, boolean jigsaw) {
		this.facing = facing;
		this.jigsaw = jigsaw;
		this.width = size.getX();
		this.height = size.getY();
		this.depth = size.getZ();
		if (facing.getAxis() == Direction.Axis.Z) {
			this.box = new BlockBox(0, 0, 0, size.getX() - 1, size.getY() - 1, size.getZ() - 1);
		} else {
			this.box = new BlockBox(0, 0, 0, size.getZ() - 1, size.getY() - 1, size.getX() - 1);
		}
	}

	/** Size of the structure as it lies in the world with this orientation. */
	public Vec3i layout() {
		if (facing.getAxis() != Direction.Axis.Z) return new Vec3i(depth, height, width);
		return new Vec3i(width, height, depth);
	}

	/** Origins (world coordinates) of every match in the chunk whose lowest corner is at {@code (startX, startZ)}. */
	public List<BlockPos> findAt(BlockSource world, int startX, int startZ) {
		List<BlockPos> result = new ArrayList<>();
		if (structure.isEmpty()) return result;
		for (BlockPos center : searchPositions) {
			boolean found = true;
			for (Map.Entry<BlockPos, Block> e : structure.entrySet()) {
				Block want = e.getValue();
				if (want == null) continue;
				BlockPos p = e.getKey();
				if (world.block(startX + center.getX() + p.getX(), center.getY() + p.getY(), startZ + center.getZ() + p.getZ()) != want) {
					found = false;
					break;
				}
			}
			if (found) result.add(new BlockPos(startX + center.getX(), center.getY(), startZ + center.getZ()));
		}
		return result;
	}

	// ---- layout construction (same transforms as the structure pieces of the game)

	private int applyX(int x, int z) {
		if (jigsaw) {
			return switch (facing) {
				case EAST -> -z + layout().getX() - 1;
				case SOUTH -> -x + layout().getX() - 1;
				case WEST -> z;
				default -> x;
			};
		}
		return switch (facing) {
			case NORTH, SOUTH -> box.getMinX() + x;
			case WEST -> box.getMaxX() - z;
			case EAST -> box.getMinX() + z;
			default -> x;
		};
	}

	private int applyY(int y) {
		return jigsaw ? y : y + box.getMinY();
	}

	private int applyZ(int x, int z) {
		if (jigsaw) {
			return switch (facing) {
				case EAST -> x;
				case SOUTH -> -z + layout().getZ() - 1;
				case WEST -> -x + layout().getZ() - 1;
				default -> z;
			};
		}
		return switch (facing) {
			case NORTH -> box.getMaxZ() - z;
			case SOUTH -> box.getMinZ() + z;
			case WEST, EAST -> box.getMinZ() + x;
			default -> z;
		};
	}

	public void addBlock(Block block, int x, int y, int z) {
		BlockPos pos = new BlockPos(applyX(x, z), applyY(y), applyZ(x, z));
		if (jigsaw || box.contains(pos)) {
			if (block == null) structure.remove(pos);
			else structure.put(pos, block);
		}
	}

	public void fillWithOutline(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Block outline, Block inside, boolean onlyReplaceAir) {
		for (int y = minY; y <= maxY; ++y) {
			for (int x = minX; x <= maxX; ++x) {
				for (int z = minZ; z <= maxZ; ++z) {
					if (y != minY && y != maxY && x != minX && x != maxX && z != minZ && z != maxZ) {
						addBlock(inside, x, y, z);
					} else {
						addBlock(outline, x, y, z);
					}
				}
			}
		}
	}
}
