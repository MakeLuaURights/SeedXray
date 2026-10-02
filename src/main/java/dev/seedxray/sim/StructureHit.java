package dev.seedxray.sim;

import java.util.List;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

/**
 * A structure found by the seed. {@code pos} is where the vanilla generator starts it;
 * {@code position} is kept so the pieces can be generated later if more detail is needed.
 */
public final class StructureHit {
	public final String id;
	public final BlockPos pos;
	/** Chunk the structure starts in (what {@code /locate} reports); {@code pos} can lie in a neighbouring chunk. */
	public final ChunkPos chunk;
	/** Bounding box of all generated pieces. */
	public final BlockBox box;
	final List<StructurePiece> pieces;
	/** Extra points derived from the pieces (e.g. end portal room, elytra frame); filled lazily. */
	StructureDetail detail;
	boolean detailDone;

	StructureHit(String id, BlockPos pos, ChunkPos chunk, List<StructurePiece> pieces) {
		this.id = id;
		this.pos = pos;
		this.chunk = chunk;
		// only these two are asked for pieces again (portal room / elytra frame); keep the rest light
		this.pieces = id.equals("stronghold") || id.equals("end_city") ? pieces : List.of();
		BlockBox box = null;
		for (StructurePiece piece : pieces) {
			BlockBox b = piece.getBoundingBox();
			box = box == null ? b : BlockBox.encompass(List.of(box, b)).orElse(box);
		}
		this.box = box;
	}

	public List<StructurePiece> piecesForTest() {
		return pieces;
	}

	/** Detail point for pseudo structures, or {@code null}. Only valid after {@link StructureLocator#detail}. */
	public StructureDetail detail() {
		return detail;
	}

	/** An exact interesting point inside a structure. */
	public record StructureDetail(BlockPos portal, BlockPos elytra) {
	}
}
