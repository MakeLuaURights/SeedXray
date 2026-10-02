package dev.seedxray;

import static org.junit.jupiter.api.Assertions.*;

import dev.seedxray.seed.*;
import java.util.List;
import java.util.OptionalLong;
import net.minecraft.world.biome.source.BiomeAccess;
import org.junit.jupiter.api.Test;

public class SeedSearchTest {
	@Test
	void hashMatchesVanilla() {
		SeedHash h = new SeedHash();
		for (long seed : new long[] {0, 1, -1, 12345, Long.MAX_VALUE, Long.MIN_VALUE, -4172144997902289642L, 987654321987L}) {
			assertEquals(BiomeAccess.hashSeed(seed), h.hash(seed), "seed " + seed);
		}
	}

	@Test
	void findsSeeds() {
		List<String> words = SeedSearch.readResourceLines("/assets/seedxray/seed_words.txt");
		assertTrue(words.size() > 100);
		long t = System.currentTimeMillis();
		for (long seed : new long[] {0, 12345, -777, 9_999_999, "hello".hashCode(), "ab3".hashCode()}) {
			OptionalLong r = SeedSearch.run(BiomeAccess.hashSeed(seed), List.of(), words, 10_000_000, () -> false, s -> {});
			assertTrue(r.isPresent(), "seed " + seed);
			assertEquals(seed, r.getAsLong());
		}
		System.out.println("SEEDSEARCH ms=" + (System.currentTimeMillis() - t));
		OptionalLong none = SeedSearch.run(BiomeAccess.hashSeed(123456789012345L), List.of(), words, 100_000, () -> false, s -> {});
		assertTrue(none.isEmpty());
		OptionalLong known = SeedSearch.run(BiomeAccess.hashSeed(-4172144997902289642L), SeedSearch.readSeedResource("/assets/seedxray/known_seeds.txt"), words, 10, () -> false, s -> {});
		assertEquals(-4172144997902289642L, known.getAsLong());
	}
}
