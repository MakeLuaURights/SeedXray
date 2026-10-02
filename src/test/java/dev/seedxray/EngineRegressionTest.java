package dev.seedxray;

import static org.junit.jupiter.api.Assertions.*;

import dev.seedxray.sim.*;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Expected values come from a real vanilla 1.21.11 dedicated server running seed 12345:
 * {@code /locate}, the blocks in its generated chunks and its item frames.
 */
public class EngineRegressionTest {
	private static final long SEED = 12345L;

	@BeforeAll
	static void boot() {
		McBootstrap.init();
	}

	private static boolean hasChunk(List<StructureHit> hits, int cx, int cz) {
		return hits.stream().anyMatch(h -> h.chunk.x == cx && h.chunk.z == cz);
	}

	@Test
	void overworldStructures() {
		DimensionSim sim = new DimensionSim(Dim.OVERWORLD, SEED);
		StructureLocator loc = new StructureLocator(sim);
		assertTrue(hasChunk(loc.locate(BlockPos.ORIGIN, 60, Set.of("village_plains")), -9, -52));
		assertTrue(hasChunk(loc.locate(BlockPos.ORIGIN, 70, Set.of("village_plains")), 10, 55));
		assertTrue(hasChunk(loc.locate(BlockPos.ORIGIN, 40, Set.of("ancient_city")), 11, -11));
		assertTrue(hasChunk(loc.locate(BlockPos.ORIGIN, 40, Set.of("trial_chambers")), 0, 14));
		assertTrue(hasChunk(loc.locate(BlockPos.ORIGIN, 40, Set.of("shipwreck")), -8, 2));

		List<StructureHit> strongholds = loc.locate(BlockPos.ORIGIN, 10, Set.of("stronghold"));
		assertEquals(128, strongholds.size());
		assertTrue(hasChunk(strongholds, -35, -103));
		StructureHit first = strongholds.stream().filter(h -> h.chunk.x == -35 && h.chunk.z == -103).findFirst().orElseThrow();
		loc.detail(first);
		assertNotNull(first.detail());
		// the centre of the end portal, with frames two blocks away on each side (checked in the real world)
		assertEquals(new BlockPos(-571, 15, -1687), first.detail().portal());
	}

	@Test
	void netherAndEnd() {
		DimensionSim nether = new DimensionSim(Dim.NETHER, SEED);
		StructureLocator nl = new StructureLocator(nether);
		assertTrue(hasChunk(nl.locate(BlockPos.ORIGIN, 40, Set.of("bastion_remnant")), 11, -16));
		assertTrue(hasChunk(nl.locate(BlockPos.ORIGIN, 40, Set.of("fortress")), 6, -32));

		DimensionSim end = new DimensionSim(Dim.END, SEED);
		StructureLocator el = new StructureLocator(end);
		List<StructureHit> cities = el.locate(BlockPos.ORIGIN, 100, Set.of("end_city"));
		assertTrue(hasChunk(cities, 62, 24));
		Set<BlockPos> elytra = new HashSet<>();
		for (StructureHit h : cities) {
			el.detail(h);
			if (h.detail() != null && h.detail().elytra() != null) elytra.add(h.detail().elytra());
		}
		// item frames holding an elytra in the real world
		assertTrue(elytra.contains(new BlockPos(-164, 142, 1284)), elytra.toString());
	}

	/** Real diamond ore positions in chunks -1..1 of the test server. */
	private static final String REAL_DIAMONDS = "-16,-42,-11;-16,-41,-11;-16,-41,-10;-16,-16,-9;-16,-16,-8;-16,-15,-8;-15,-56,8;-15,-55,8;-15,-41,-11;-15,-41,-10;-15,-17,4;-15,-17,5;"
		+ "-15,-16,-9;-15,-16,-8;-15,-16,4;-15,-16,5;-15,-15,-9;-15,-15,-8;-14,-57,9;-14,-56,8;-14,-56,9;-14,-39,8;-14,-39,9;-14,-24,17;-14,-17,4;-14,-17,5;"
		+ "-14,-16,4;-14,-16,5;-13,-39,8;-13,-39,9;-9,0,12;-9,0,13;-8,0,12;-8,0,13;-7,-49,7;-7,-49,8;-7,-48,7;-7,-48,8;-7,2,-7;-6,-49,7;-6,-49,8;-6,-48,7;"
		+ "-6,-48,8;-6,-48,21;-6,-48,22;-6,-45,25;-6,-44,25;-6,-39,24;-6,-38,23;-6,-16,-5;-6,-16,-4;-6,1,-8;-6,2,-8;-5,-58,2;-5,-58,3;-5,-49,22;-5,-48,21;"
		+ "-5,-48,22;-5,-45,24;-5,-44,24;-5,-43,11;-5,-39,24;-5,-38,23;-5,-33,-3;-5,-32,-3;-5,-16,-5;-5,-16,-4;-4,-59,2;-4,-58,2;-4,-58,3;-4,-57,18;-4,-56,17;"
		+ "-4,-56,18;-3,-56,17;-3,-56,18;-3,-55,17;-3,-18,-4;-3,-18,-3;-2,-19,26;-2,-18,-4;-2,-17,-4;-2,-17,-3;-2,-16,-3;-1,-50,22;-1,-50,23;-1,-30,-12;"
		+ "-1,-30,-11;-1,-20,26;-1,-20,27;-1,-19,26;-1,-12,30;0,-42,-7;0,-41,-7;0,-41,-6;0,-31,-12;0,-30,-12;0,-30,-11;0,-22,-4;0,-21,-4;0,-21,-3;0,-20,27;"
		+ "0,-19,26;0,-13,31;0,-12,30;1,-41,-7;1,-41,-6;1,-40,-6;1,-21,-3;4,-60,-4;4,-59,-4;4,-32,21;4,-31,21;5,-60,-14;6,-60,-15;6,-9,2;6,-9,3;7,-9,2;7,-9,3;"
		+ "7,-8,3;9,-61,11;9,-52,-4;9,-51,-4;9,-51,20;9,-51,21;9,-28,12;9,-28,13;9,-27,12;9,-27,13;10,-51,21;10,-32,24;10,-32,25;10,-31,24;10,-31,25;10,-28,12;"
		+ "10,-28,13;10,-27,7;10,-27,12;10,-27,13;11,-52,24;11,-51,24;11,-48,26;11,-47,26;11,-32,25;11,-31,24;11,-28,6;11,-28,7;11,-13,0;11,-12,0;11,-12,1;"
		+ "12,-39,-5;12,-14,1;12,-13,1;12,-12,0;13,-46,14;13,-46,15;13,-45,14;13,-45,15;13,-38,-6;13,-37,-6;13,-36,-5;13,-35,-5;13,-30,28;13,-30,29;13,-13,6;"
		+ "13,-13,7;14,-57,21;14,-56,21;14,-47,14;14,-46,14;14,-46,15;14,-45,14;14,-45,15;14,-36,-6;14,-36,-5;14,-30,28;14,-30,29;14,-21,2;14,-20,1;15,-21,2;15,-20,1";

	@Test
	void predictedDiamondsMatchTheRealWorld() {
		DimensionSim sim = new DimensionSim(Dim.OVERWORLD, SEED);
		OreSimulator ores = new OreSimulator(sim, new TerrainCache(sim, 100, true));
		Set<Long> predicted = new HashSet<>();
		Block diamond = Registries.BLOCK.get(net.minecraft.util.Identifier.ofVanilla("deepslate_diamond_ore"));
		for (int cx = -2; cx <= 1; cx++) {
			for (int cz = -2; cz <= 1; cz++) {
				OreChunk c = ores.simulate(cx, cz);
				for (int i = 0; i < c.size(); i++) if (c.blocks[i] == diamond) predicted.add(c.positions[i]);
			}
		}
		int total = 0, hit = 0;
		for (String s : REAL_DIAMONDS.split(";")) {
			String[] p = s.split(",");
			total++;
			if (predicted.contains(BlockPos.asLong(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])))) hit++;
		}
		assertTrue(total > 100);
		assertTrue(hit >= total * 0.9, hit + " of " + total + " real diamond blocks predicted");
	}
}
