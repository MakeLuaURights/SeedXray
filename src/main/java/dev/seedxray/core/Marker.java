package dev.seedxray.core;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

/**
 * A structure (or a point inside one) to show. {@code id} is a {@code StructureCatalog} id; {@code box} may be null.
 */
public record Marker(String id, BlockPos pos, BlockBox box) {
}
