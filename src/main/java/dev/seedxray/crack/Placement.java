package dev.seedxray.crack;

import dev.seedxray.sim.WorldgenData;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.SpreadType;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;

/**
 * How one structure kind is spread over the world, read from the game's own data. Provides the very fast
 * "could this seed have placed the structure there" test the brute force needs: it is the game's
 * {@code RandomSpreadStructurePlacement.getStartChunk} with the random number generator written out.
 */
public final class Placement {
	private static final long MULT = 0x5DEECE66DL;
	private static final long MASK = (1L << 48) - 1;
	private static final long REGION_X = 341873128712L;
	private static final long REGION_Z = 132897987541L;

	public final Kind kind;
	public final RandomSpreadStructurePlacement vanilla;
	public final int spacing;
	/** Range of the offset inside a region, {@code spacing - separation}. */
	public final int range;
	public final int salt;
	public final boolean triangular;
	/** Extra chance a chunk must pass, 1 when every start chunk generates. */
	public final boolean hasFrequency;

	private static final Placement[] CACHE = new Placement[Kind.values().length];

	public static synchronized Placement of(Kind kind) {
		Placement p = CACHE[kind.ordinal()];
		if (p == null) {
			StructurePlacement sp = WorldgenData.get().registries.getOrThrow(RegistryKeys.STRUCTURE_SET).getOrThrow(kind.set).value().placement();
			p = new Placement(kind, (RandomSpreadStructurePlacement) sp);
			CACHE[kind.ordinal()] = p;
		}
		return p;
	}

	private Placement(Kind kind, RandomSpreadStructurePlacement vanilla) {
		this.kind = kind;
		this.vanilla = vanilla;
		this.spacing = vanilla.getSpacing();
		this.range = vanilla.getSpacing() - vanilla.getSeparation();
		this.salt = vanilla.getSalt();
		this.triangular = vanilla.getSpreadType() == SpreadType.TRIANGULAR;
		this.hasFrequency = kind == Kind.BURIED_TREASURE;
	}

	/** Information a sighting carries, in bits (an estimate: used to know when there is enough). */
	public double bits() {
		if (kind == Kind.BURIED_TREASURE) return Math.log(100) / Math.log(2);
		double bits = 2 * Math.log(range) / Math.log(2);
		return triangular ? bits - 1.5 : bits;
	}

	public int regionX(int chunkX) {
		return Math.floorDiv(chunkX, spacing);
	}

	/** Offset (0..range-1) of a chunk inside its region. */
	public int offset(int chunk) {
		return chunk - Math.floorDiv(chunk, spacing) * spacing;
	}

	/** Same result as {@code vanilla.getStartChunk(seed, ...)} for the region {@code (rx, rz)}, returned as offsets. */
	public long startOffsets(long seed, int rx, int rz) {
		long state = ((rx * REGION_X + rz * REGION_Z + seed + salt) ^ MULT) & MASK;
		long[] s = {state};
		int x, z;
		if (triangular) {
			x = (nextInt(s, range) + nextInt(s, range)) / 2;
			z = (nextInt(s, range) + nextInt(s, range)) / 2;
		} else {
			x = nextInt(s, range);
			z = nextInt(s, range);
		}
		return ((long) x << 32) | (z & 0xFFFFFFFFL);
	}

	private static int nextInt(long[] s, int bound) {
		if ((bound & -bound) == bound) {
			s[0] = (s[0] * MULT + 0xBL) & MASK;
			return (int) ((bound * (long) (int) (s[0] >>> 17)) >> 31);
		}
		int bits, val;
		do {
			s[0] = (s[0] * MULT + 0xBL) & MASK;
			bits = (int) (s[0] >>> 17);
			val = bits % bound;
		} while (bits - val + (bound - 1) < 0);
		return val;
	}

	/** Allocation-free version of the above that tests against observed offsets. */
	public boolean matches(long seed, int rx, int rz, int ox, int oz) {
		long state = ((rx * REGION_X + rz * REGION_Z + seed + salt) ^ MULT) & MASK;
		int bound = range;
		int x, z;
		if (!triangular && (bound & -bound) != bound) {
			// two draws, written out so the hot loop does not allocate
			int bits, val;
			do {
				state = (state * MULT + 0xBL) & MASK;
				bits = (int) (state >>> 17);
				val = bits % bound;
			} while (bits - val + (bound - 1) < 0);
			if (val != ox) return false;
			do {
				state = (state * MULT + 0xBL) & MASK;
				bits = (int) (state >>> 17);
				val = bits % bound;
			} while (bits - val + (bound - 1) < 0);
			return val == oz;
		}
		long packed = startOffsets(seed, rx, rz);
		x = (int) (packed >> 32);
		z = (int) packed;
		return x == ox && z == oz;
	}
}
