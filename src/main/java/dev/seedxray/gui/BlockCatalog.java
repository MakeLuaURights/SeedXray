package dev.seedxray.gui;

import dev.seedxray.core.Names;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Every block of the game, for the searchable Block ESP list. Built once, on first use. */
public final class BlockCatalog {
	/** Blocks the world generator places in veins, i.e. the ones the seed can predict. */
	private static final Set<String> PREDICTED = Set.of(
		"ancient_debris", "andesite", "blackstone", "clay", "coal_ore", "copper_ore", "deepslate_coal_ore", "deepslate_copper_ore",
		"deepslate_diamond_ore", "deepslate_emerald_ore", "deepslate_gold_ore", "deepslate_iron_ore", "deepslate_lapis_ore",
		"deepslate_redstone_ore", "diamond_ore", "diorite", "dirt", "emerald_ore", "gold_ore", "granite", "gravel", "infested_deepslate",
		"infested_stone", "iron_ore", "lapis_ore", "magma_block", "nether_gold_ore", "nether_quartz_ore", "redstone_ore", "soul_sand",
		"tuff", "raw_iron_block", "raw_copper_block"
	);
	/** Shown first when nothing is typed: the blocks people usually look for. */
	private static final List<String> POPULAR = List.of(
		"diamond_ore", "deepslate_diamond_ore", "ancient_debris", "emerald_ore", "deepslate_emerald_ore", "gold_ore", "deepslate_gold_ore",
		"nether_gold_ore", "iron_ore", "deepslate_iron_ore", "raw_iron_block", "copper_ore", "deepslate_copper_ore", "raw_copper_block",
		"redstone_ore", "deepslate_redstone_ore", "lapis_ore", "deepslate_lapis_ore", "coal_ore", "deepslate_coal_ore", "nether_quartz_ore",
		"obsidian", "crying_obsidian", "spawner", "chest", "trial_spawner", "vault", "budding_amethyst", "amethyst_cluster", "bedrock"
	);

	private static List<BlockEntry> all;

	private BlockCatalog() {
	}

	public static synchronized List<BlockEntry> all() {
		if (all == null) all = build();
		return all;
	}

	private static List<BlockEntry> build() {
		List<BlockEntry> list = new ArrayList<>();
		for (Block block : Registries.BLOCK) {
			Identifier id = Registries.BLOCK.getId(block);
			String path = id.getPath();
			if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR || path.equals("moving_piston") || path.equals("piston_head")) continue;
			ItemStack icon = iconFor(block);
			if (icon.isEmpty()) continue;
			list.add(new BlockEntry(block, id.toString(), Names.title(path), icon, PREDICTED.contains(path)));
		}
		list.sort(Comparator.comparingInt(BlockCatalog::rank).thenComparing(e -> e.name));
		return list;
	}

	private static int rank(BlockEntry e) {
		int i = POPULAR.indexOf(e.id.substring(e.id.indexOf(':') + 1));
		if (i >= 0) return i;
		return e.predicted ? 100 : 1000;
	}

	private static ItemStack iconFor(Block block) {
		Item item = block.asItem();
		if (item != Items.AIR) return new ItemStack(item);
		if (block == Blocks.WATER) return new ItemStack(Items.WATER_BUCKET);
		if (block == Blocks.LAVA) return new ItemStack(Items.LAVA_BUCKET);
		if (block == Blocks.FIRE || block == Blocks.SOUL_FIRE) return new ItemStack(Items.FLINT_AND_STEEL);
		if (block == Blocks.NETHER_PORTAL) return new ItemStack(Items.OBSIDIAN);
		if (block == Blocks.END_PORTAL || block == Blocks.END_GATEWAY) return new ItemStack(Items.END_PORTAL_FRAME);
		return ItemStack.EMPTY;
	}

	/** Entries whose name or id contains every word of {@code query}. */
	public static List<BlockEntry> search(String query) {
		String q = query.trim().toLowerCase(Locale.ROOT);
		if (q.isEmpty()) return all();
		String[] words = q.split("\\s+");
		List<BlockEntry> out = new ArrayList<>();
		for (BlockEntry e : all()) {
			boolean ok = true;
			for (String w : words) {
				if (!e.searchText.contains(w)) {
					ok = false;
					break;
				}
			}
			if (ok) out.add(e);
		}
		// names that start with the query first
		out.sort(Comparator.comparingInt((BlockEntry e) -> e.name.toLowerCase(Locale.ROOT).startsWith(q) ? 0 : 1));
		return out;
	}
}
