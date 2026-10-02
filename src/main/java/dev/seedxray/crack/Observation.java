package dev.seedxray.crack;

/** A structure that was seen in the world: its kind and the chunk the game started it in. */
public record Observation(Kind kind, int chunkX, int chunkZ) {
	public Placement placement() {
		return Placement.of(kind);
	}

	/** Could {@code seed} (only its low 48 bits matter) have put this structure here? */
	public boolean matches(long seed) {
		Placement p = placement();
		if (p.hasFrequency && !p.vanilla.applyFrequencyReduction(chunkX, chunkZ, seed)) return false;
		return p.matches(seed, p.regionX(chunkX), p.regionX(chunkZ), p.offset(chunkX), p.offset(chunkZ));
	}
}
