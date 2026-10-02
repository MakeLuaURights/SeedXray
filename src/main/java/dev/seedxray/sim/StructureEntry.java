package dev.seedxray.sim;

import net.minecraft.item.Item;

/**
 * One selectable structure in the menu. {@code id} is the vanilla structure id path
 * (e.g. {@code village_plains}); {@code color} is the default ESP color (0xRRGGBB).
 */
public record StructureEntry(String id, String name, Dim dim, Item icon, int color) {
	public String key() {
		return "minecraft:" + id;
	}
}
