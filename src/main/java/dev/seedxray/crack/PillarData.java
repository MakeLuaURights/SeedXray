package dev.seedxray.crack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The ten obsidian pillars of the End: their order is a shuffle seeded with 16 bits taken from the world seed, so the
 * pillar heights seen in the End reveal those 16 bits.
 */
public final class PillarData {
	private final List<Integer> heights;

	/** Heights in the order of the pillar ring (index 0..9 around the circle). */
	public PillarData(List<Integer> heights) {
		this.heights = heights;
	}

	public List<Integer> heights() {
		return heights;
	}

	public static List<Integer> heightsFor(int pillarSeed) {
		List<Integer> indices = new ArrayList<>();
		for (int i = 0; i < 10; i++) indices.add(i);
		Collections.shuffle(indices, new Random(pillarSeed));
		List<Integer> result = new ArrayList<>();
		for (int index : indices) result.add(76 + index * 3);
		return result;
	}

	/** Every 16 bit pillar seed that produces the observed heights (almost always exactly one). */
	public List<Integer> candidates() {
		List<Integer> out = new ArrayList<>();
		for (int seed = 0; seed < 1 << 16; seed++) {
			if (heightsFor(seed).equals(heights)) out.add(seed);
		}
		return out;
	}
}
