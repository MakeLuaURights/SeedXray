package dev.seedxray.core;

import dev.seedxray.sim.Dim;
import java.util.List;
import net.minecraft.util.math.BlockPos;

/** Immutable result of one structure search. */
public record StructureSnapshot(long seed, Dim dim, BlockPos center, int radiusChunks, List<Marker> markers, boolean complete) {
	public static final StructureSnapshot EMPTY = new StructureSnapshot(0, Dim.OVERWORLD, BlockPos.ORIGIN, 0, List.of(), true);
}
