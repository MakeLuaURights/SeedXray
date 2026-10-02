package dev.seedxray.gui;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

/** One block in the "Block ESP" list. */
public final class BlockEntry {
	public final Block block;
	/** {@code minecraft:diamond_ore} */
	public final String id;
	/** English name, {@code Diamond Ore}. */
	public final String name;
	public final String searchText;
	public final ItemStack icon;
	/** The seed can predict where this block generates (ores and other vein blocks). */
	public final boolean predicted;

	BlockEntry(Block block, String id, String name, ItemStack icon, boolean predicted) {
		this.block = block;
		this.id = id;
		this.name = name;
		this.searchText = (name + " " + id).toLowerCase(java.util.Locale.ROOT);
		this.icon = icon;
		this.predicted = predicted;
	}
}
