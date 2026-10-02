package dev.seedxray;

import static org.junit.jupiter.api.Assertions.*;

import dev.seedxray.sim.*;
import java.util.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;
import org.junit.jupiter.api.Test;

/** The menu's structure list must match the structures the game really has, and sit in the right dimension. */
public class CatalogTest {
	@Test
	void catalogMatchesTheGame() {
		McBootstrap.init();
		var registry = WorldgenData.get().registries.getOrThrow(RegistryKeys.STRUCTURE);
		Set<String> real = new TreeSet<>();
		registry.streamEntries().forEach(e -> real.add(e.registryKey().getValue().getPath()));
		Set<String> catalog = new TreeSet<>();
		for (StructureEntry e : StructureCatalog.all()) {
			if (e.id().equals(StructureCatalog.END_PORTAL) || e.id().equals(StructureCatalog.END_SHIP_ELYTRA)) continue;
			catalog.add(e.id());
			assertTrue(real.contains(e.id()), "unknown structure " + e.id());
			// valid biomes must exist in the catalog's dimension
			Structure structure = registry.getOptional(net.minecraft.registry.RegistryKey.of(RegistryKeys.STRUCTURE, Identifier.ofVanilla(e.id()))).orElseThrow().value();
			DimensionSim sim = new DimensionSim(e.dim(), 1L);
			boolean inDim = structure.getValidBiomes().stream().anyMatch(sim.biomeSource.getBiomes()::contains);
			assertTrue(inDim, e.id() + " is not generated in " + e.dim());
		}
		Set<String> missing = new TreeSet<>(real);
		missing.removeAll(catalog);
		assertTrue(missing.isEmpty(), "structures missing from the menu: " + missing);
	}
}
