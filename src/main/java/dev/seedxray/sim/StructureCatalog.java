package dev.seedxray.sim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

/** Static list of every vanilla structure the mod can find, grouped by dimension, with English names and icons. */
public final class StructureCatalog {
	/** Pseudo-structures derived from real ones (found by generating the structure's pieces). */
	public static final String END_PORTAL = "end_portal";
	public static final String END_SHIP_ELYTRA = "end_city_elytra";

	private static final Map<String, StructureEntry> BY_ID = new LinkedHashMap<>();
	private static final List<StructureEntry> ALL = new ArrayList<>();

	private static void add(String id, String name, Dim dim, Item icon, int color) {
		StructureEntry e = new StructureEntry(id, name, dim, icon, color);
		BY_ID.put(id, e);
		ALL.add(e);
	}

	static {
		// Overworld
		add("stronghold", "Stronghold", Dim.OVERWORLD, Items.ENDER_EYE, 0x55FF55);
		add(END_PORTAL, "End Portal (in Stronghold)", Dim.OVERWORLD, Items.END_PORTAL_FRAME, 0xB36BFF);
		add("village_plains", "Village (Plains)", Dim.OVERWORLD, Items.BELL, 0xFFD84D);
		add("village_desert", "Village (Desert)", Dim.OVERWORLD, Items.BELL, 0xFFD84D);
		add("village_savanna", "Village (Savanna)", Dim.OVERWORLD, Items.BELL, 0xFFD84D);
		add("village_snowy", "Village (Snowy)", Dim.OVERWORLD, Items.BELL, 0xFFD84D);
		add("village_taiga", "Village (Taiga)", Dim.OVERWORLD, Items.BELL, 0xFFD84D);
		add("pillager_outpost", "Pillager Outpost", Dim.OVERWORLD, Items.CROSSBOW, 0xC0392B);
		add("mansion", "Woodland Mansion", Dim.OVERWORLD, Items.TOTEM_OF_UNDYING, 0x8B5A2B);
		add("monument", "Ocean Monument", Dim.OVERWORLD, Items.PRISMARINE_BRICKS, 0x3FD0C9);
		add("ancient_city", "Ancient City", Dim.OVERWORLD, Items.SCULK_SHRIEKER, 0x1E8F8F);
		add("trial_chambers", "Trial Chambers", Dim.OVERWORLD, Items.TRIAL_KEY, 0xE08A3C);
		add("trail_ruins", "Trail Ruins", Dim.OVERWORLD, Items.BRUSH, 0xC9A66B);
		add("desert_pyramid", "Desert Pyramid", Dim.OVERWORLD, Items.SANDSTONE, 0xE5C87A);
		add("jungle_pyramid", "Jungle Temple", Dim.OVERWORLD, Items.MOSSY_COBBLESTONE, 0x4CAF50);
		add("igloo", "Igloo", Dim.OVERWORLD, Items.SNOW_BLOCK, 0xDDEEFF);
		add("swamp_hut", "Swamp Hut", Dim.OVERWORLD, Items.CAULDRON, 0x6B8E23);
		add("mineshaft", "Mineshaft", Dim.OVERWORLD, Items.RAIL, 0xA0A0A0);
		add("mineshaft_mesa", "Mineshaft (Badlands)", Dim.OVERWORLD, Items.RAIL, 0xA0A0A0);
		add("buried_treasure", "Buried Treasure", Dim.OVERWORLD, Items.CHEST, 0xFFAA00);
		add("shipwreck", "Shipwreck", Dim.OVERWORLD, Items.OAK_BOAT, 0x8B6B3D);
		add("shipwreck_beached", "Shipwreck (Beached)", Dim.OVERWORLD, Items.OAK_BOAT, 0x8B6B3D);
		add("ocean_ruin_cold", "Ocean Ruins (Cold)", Dim.OVERWORLD, Items.STONE_BRICKS, 0x6FA3C7);
		add("ocean_ruin_warm", "Ocean Ruins (Warm)", Dim.OVERWORLD, Items.SANDSTONE, 0xD9B38C);
		add("ruined_portal", "Ruined Portal", Dim.OVERWORLD, Items.CRYING_OBSIDIAN, 0xA855F7);
		add("ruined_portal_desert", "Ruined Portal (Desert)", Dim.OVERWORLD, Items.CRYING_OBSIDIAN, 0xA855F7);
		add("ruined_portal_jungle", "Ruined Portal (Jungle)", Dim.OVERWORLD, Items.CRYING_OBSIDIAN, 0xA855F7);
		add("ruined_portal_mountain", "Ruined Portal (Mountain)", Dim.OVERWORLD, Items.CRYING_OBSIDIAN, 0xA855F7);
		add("ruined_portal_ocean", "Ruined Portal (Ocean)", Dim.OVERWORLD, Items.CRYING_OBSIDIAN, 0xA855F7);
		add("ruined_portal_swamp", "Ruined Portal (Swamp)", Dim.OVERWORLD, Items.CRYING_OBSIDIAN, 0xA855F7);
		// Nether
		add("fortress", "Nether Fortress", Dim.NETHER, Items.NETHER_BRICKS, 0xFF5555);
		add("bastion_remnant", "Bastion Remnant", Dim.NETHER, Items.GILDED_BLACKSTONE, 0xFFC107);
		add("nether_fossil", "Nether Fossil", Dim.NETHER, Items.BONE_BLOCK, 0xEDE7D6);
		add("ruined_portal_nether", "Ruined Portal (Nether)", Dim.NETHER, Items.CRYING_OBSIDIAN, 0xA855F7);
		// The End
		add(END_SHIP_ELYTRA, "End Ship (Elytra)", Dim.END, Items.ELYTRA, 0xFF55FF);
		add("end_city", "End City", Dim.END, Items.PURPUR_BLOCK, 0xD59CFC);
	}

	private StructureCatalog() {
	}

	public static List<StructureEntry> all() {
		return ALL;
	}

	public static List<StructureEntry> of(Dim dim) {
		return ALL.stream().filter(e -> e.dim() == dim).toList();
	}

	public static StructureEntry get(String id) {
		return BY_ID.get(id);
	}
}
