package dev.seedxray;

import static org.junit.jupiter.api.Assertions.*;

import dev.seedxray.crack.*;
import dev.seedxray.sim.*;
import java.util.*;
import java.util.stream.IntStream;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class CrackTest {
	@BeforeAll
	static void boot() {
		McBootstrap.init();
	}

	@Test
	void fastPlacementTestEqualsTheGame() {
		java.util.Random rnd = new java.util.Random(7);
		for (Kind kind : Kind.values()) {
			Placement p = Placement.of(kind);
			for (int i = 0; i < 300; i++) {
				long seed = rnd.nextLong();
				int rx = rnd.nextInt(200) - 100, rz = rnd.nextInt(200) - 100;
				ChunkPos real = p.vanilla.getStartChunk(seed, rx * p.spacing, rz * p.spacing);
				long packed = p.startOffsets(seed, rx, rz);
				assertEquals(real.x - rx * p.spacing, (int) (packed >> 32), kind + " x");
				assertEquals(real.z - rz * p.spacing, (int) packed, kind + " z");
				assertTrue(p.matches(seed, rx, rz, real.x - rx * p.spacing, real.z - rz * p.spacing), kind + " matches");
				Observation o = new Observation(kind, real.x, real.z);
				if (!p.hasFrequency) assertTrue(o.matches(seed), kind + " observation");
			}
		}
	}

	@Test
	void pillarSeedIsBitsOfTheGeneratorState() {
		java.util.Random rnd = new java.util.Random(3);
		for (int i = 0; i < 1000; i++) {
			long seed = rnd.nextLong() & ((1L << 48) - 1);
			long state = (seed ^ 0x5DEECE66DL) & ((1L << 48) - 1);
			state = (state * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
			state = (state * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
			assertEquals((int) ((state >>> 16) & 0xFFFF), Cracker.pillarSeedOf(seed));
			// the game's own pillar order
			int pillarSeed = Cracker.pillarSeedOf(seed);
			var shuffled = Util.shuffle(IntStream.range(0, 10), Random.create((long) pillarSeed));
			List<Integer> heights = new ArrayList<>();
			for (int k = 0; k < 10; k++) heights.add(76 + shuffled.getInt(k) * 3);
			assertEquals(heights, PillarData.heightsFor(pillarSeed));
		}
	}

	/** Real structure sightings of a seed, as the cracker would collect them. */
	static List<Observation> sightings(long seed, int radius) {
		DimensionSim sim = new DimensionSim(Dim.OVERWORLD, seed);
		StructureLocator loc = new StructureLocator(sim);
		Map<String, Kind> kinds = Map.of("desert_pyramid", Kind.DESERT_PYRAMID, "jungle_pyramid", Kind.JUNGLE_TEMPLE, "swamp_hut", Kind.SWAMP_HUT,
			"igloo", Kind.IGLOO, "buried_treasure", Kind.BURIED_TREASURE, "monument", Kind.MONUMENT);
		List<Observation> out = new ArrayList<>();
		for (var e : kinds.entrySet()) {
			for (StructureHit h : loc.locate(BlockPos.ORIGIN, radius, Set.of(e.getKey()))) {
				out.add(new Observation(e.getValue(), h.chunk.x, h.chunk.z));
			}
		}
		return out;
	}

	@Test
	void realSightingsAllMatchTheirSeed() {
		List<Observation> obs = sightings(12345L, 120);
		assertTrue(obs.size() > 5, "found " + obs.size());
		for (Observation o : obs) assertTrue(o.matches(12345L), o.toString());
	}

	@Test
	@Tag("slow")
	void pillarPathFindsTheSeed() {
		long seed = 12345L;
		List<Observation> obs = new ArrayList<>(sightings(seed, 200));
		double bits = obs.stream().mapToDouble(o -> o.placement().bits()).sum();
		System.out.println("CRACK sightings=" + obs.size() + " bits=" + bits);
		assertTrue(bits >= 32);
		int pillar = Cracker.pillarSeedOf(seed);
		long t = System.currentTimeMillis();
		Set<Long> found = Cracker.pillarSearch(pillar, obs, Runtime.getRuntime().availableProcessors(), () -> false, p -> {});
		System.out.println("CRACK pillar search ms=" + (System.currentTimeMillis() - t) + " candidates=" + found.size());
		assertTrue(found.contains(seed), found.toString());
		long hashed = net.minecraft.world.biome.source.BiomeAccess.hashSeed(seed);
		assertEquals(List.of(seed), Cracker.worldSeedsFromHash(seed, hashed));
	}

	@Test
	@Tag("slow")
	void liftingPathFindsTheSeed() {
		long seed = 12345L;
		List<Observation> all = sightings(seed, 200);
		List<Observation> lift = new ArrayList<>();
		for (Observation o : all) if (o.kind().liftable()) lift.add(o);
		lift.sort(Comparator.comparingLong(o -> (long) o.chunkX() * o.chunkX() + (long) o.chunkZ() * o.chunkZ()));
		List<Observation> use = new ArrayList<>();
		double bits = 0;
		for (Observation o : lift) {
			use.add(o);
			bits += o.placement().bits();
			if (bits >= 40) break;
		}
		System.out.println("CRACK use=" + use);
		System.out.println("CRACK lifting sightings=" + use.size() + " bits=" + bits + " of " + lift.size() + " available");
		assertTrue(bits >= 40, "only " + bits + " bits of old structures near spawn");
		long t = System.currentTimeMillis();
		List<Long> lowers = Cracker.liftLowerBits(use);
		System.out.println("CRACK lowers=" + lowers.size() + " ms=" + (System.currentTimeMillis() - t));
		assertTrue(lowers.contains(seed & ((1L << 19) - 1)));
		t = System.currentTimeMillis();
		Set<Long> found = Cracker.liftingSearch(use, lowers, Runtime.getRuntime().availableProcessors(), () -> false, p -> {});
		System.out.println("CRACK lifting ms=" + (System.currentTimeMillis() - t) + " candidates=" + found.size());
		assertTrue(found.contains(seed));
		int bad = 0;
		for (long c : found) {
			for (Observation o : use) if (!o.matches(c)) { bad++; break; }
		}
		System.out.println("CRACK candidates that fail the plain test: " + bad + " of " + found.size());
		assertEquals(0, bad);
	}
}
