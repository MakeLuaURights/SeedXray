package dev.seedxray.crack;

import net.minecraft.registry.RegistryKey;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureSetKeys;

/**
 * The structures the seed cracker can recognise in the world. Each one is placed in a grid of regions by the game,
 * and the offset of the structure inside its region is a function of the seed, so every sighting tells a little
 * about the seed.
 */
public enum Kind {
	DESERT_PYRAMID("Desert Pyramid", StructureSetKeys.DESERT_PYRAMIDS, "desert_pyramid"),
	JUNGLE_TEMPLE("Jungle Temple", StructureSetKeys.JUNGLE_TEMPLES, "jungle_pyramid"),
	SWAMP_HUT("Swamp Hut", StructureSetKeys.SWAMP_HUTS, "swamp_hut"),
	IGLOO("Igloo", StructureSetKeys.IGLOOS, "igloo"),
	BURIED_TREASURE("Buried Treasure", StructureSetKeys.BURIED_TREASURES, "buried_treasure"),
	MONUMENT("Ocean Monument", StructureSetKeys.OCEAN_MONUMENTS, "monument"),
	END_CITY("End City", net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.STRUCTURE_SET, net.minecraft.util.Identifier.ofVanilla("end_cities")), "end_city");

	public final String title;
	public final RegistryKey<StructureSet> set;
	/** Structure id (also the id the structure catalog uses) whose valid biomes a sighting must be in. */
	public final String structureId;

	Kind(String title, RegistryKey<StructureSet> set, String structureId) {
		this.title = title;
		this.set = set;
		this.structureId = structureId;
	}

	/** Old structures can be "lifted": their offsets leak the low bits of the seed on their own. */
	public boolean liftable() {
		return this == DESERT_PYRAMID || this == JUNGLE_TEMPLE || this == SWAMP_HUT || this == IGLOO;
	}
}
