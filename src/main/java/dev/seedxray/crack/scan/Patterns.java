package dev.seedxray.crack.scan;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

/**
 * Block layouts of the structures the cracker looks for, ported from SeedcrackerX
 * (https://github.com/19MisterX98/SeedcrackerX, MIT license, KaptainWutax / 19MisterX98). Only the block types matter:
 * a spot matches when every listed block is the listed block type.
 */
final class Patterns {
	private Patterns() {
	}

	static void desert(PieceFinder finder) {
		Block blockState_1 = Blocks.SANDSTONE_STAIRS;
		Block blockState_2 = Blocks.SANDSTONE_STAIRS;
		Block blockState_3 = Blocks.SANDSTONE_STAIRS;
		Block blockState_4 = Blocks.SANDSTONE_STAIRS;
		finder.fillWithOutline(0, 0, 0, 4, 9, 4, Blocks.SANDSTONE, Blocks.AIR, false);
		finder.fillWithOutline(1, 10, 1, 3, 10, 3, Blocks.SANDSTONE, Blocks.SANDSTONE, false);
		finder.addBlock(blockState_1, 2, 10, 0);
		finder.addBlock(blockState_2, 2, 10, 4);
		finder.addBlock(blockState_3, 0, 10, 2);
		finder.addBlock(blockState_4, 4, 10, 2);
		finder.fillWithOutline(finder.width - 5, 0, 0, finder.width - 1, 9, 4, Blocks.SANDSTONE, Blocks.AIR, false);
		finder.fillWithOutline(finder.width - 4, 10, 1, finder.width - 2, 10, 3, Blocks.SANDSTONE, Blocks.SANDSTONE, false);
		finder.addBlock(blockState_1, finder.width - 3, 10, 0);
		finder.addBlock(blockState_2, finder.width - 3, 10, 4);
		finder.addBlock(blockState_3, finder.width - 5, 10, 2);
		finder.addBlock(blockState_4, finder.width - 1, 10, 2);
		finder.fillWithOutline(8, 0, 0, 12, 4, 4, Blocks.SANDSTONE, Blocks.AIR, false);
		finder.fillWithOutline(9, 1, 0, 11, 3, 4, Blocks.AIR, Blocks.AIR, false);
		finder.addBlock(Blocks.CUT_SANDSTONE, 9, 1, 1);
		finder.addBlock(Blocks.CUT_SANDSTONE, 9, 2, 1);
		finder.addBlock(Blocks.CUT_SANDSTONE, 9, 3, 1);
		finder.addBlock(Blocks.CUT_SANDSTONE, 10, 3, 1);
		finder.addBlock(Blocks.CUT_SANDSTONE, 11, 3, 1);
		finder.addBlock(Blocks.CUT_SANDSTONE, 11, 2, 1);
		finder.addBlock(Blocks.CUT_SANDSTONE, 11, 1, 1);
		finder.fillWithOutline(4, 1, 1, 8, 3, 3, Blocks.SANDSTONE, Blocks.AIR, false);
		finder.fillWithOutline(4, 1, 2, 8, 2, 2, Blocks.AIR, Blocks.AIR, false);
		finder.fillWithOutline(12, 1, 1, 16, 3, 3, Blocks.SANDSTONE, Blocks.AIR, false);
		finder.fillWithOutline(12, 1, 2, 16, 2, 2, Blocks.AIR, Blocks.AIR, false);
		finder.fillWithOutline(5, 4, 5, finder.width - 6, 4, finder.depth - 6, Blocks.SANDSTONE, Blocks.SANDSTONE, false);
		finder.fillWithOutline(9, 4, 9, 11, 4, 11, Blocks.AIR, Blocks.AIR, false);
		finder.fillWithOutline(8, 1, 8, 8, 3, 8, Blocks.CUT_SANDSTONE, Blocks.CUT_SANDSTONE, false);
		finder.fillWithOutline(12, 1, 8, 12, 3, 8, Blocks.CUT_SANDSTONE, Blocks.CUT_SANDSTONE, false);
		finder.fillWithOutline(8, 1, 12, 8, 3, 12, Blocks.CUT_SANDSTONE, Blocks.CUT_SANDSTONE, false);
		finder.fillWithOutline(12, 1, 12, 12, 3, 12, Blocks.CUT_SANDSTONE, Blocks.CUT_SANDSTONE, false);
		finder.fillWithOutline(1, 1, 5, 4, 4, 11, Blocks.SANDSTONE, Blocks.SANDSTONE, false);
		finder.fillWithOutline(finder.width - 5, 1, 5, finder.width - 2, 4, 11, Blocks.SANDSTONE, Blocks.SANDSTONE, false);
		finder.fillWithOutline(6, 7, 9, 6, 7, 11, Blocks.SANDSTONE, Blocks.SANDSTONE, false);
		finder.fillWithOutline(finder.width - 7, 7, 9, finder.width - 7, 7, 11, Blocks.SANDSTONE, Blocks.SANDSTONE, false);
		finder.fillWithOutline(5, 5, 9, 5, 7, 11, Blocks.CUT_SANDSTONE, Blocks.CUT_SANDSTONE, false);
		finder.fillWithOutline(finder.width - 6, 5, 9, finder.width - 6, 7, 11, Blocks.CUT_SANDSTONE, Blocks.CUT_SANDSTONE, false);
		finder.addBlock(Blocks.AIR, 5, 5, 10);
		finder.addBlock(Blocks.AIR, 5, 6, 10);
		finder.addBlock(Blocks.AIR, 6, 6, 10);
		finder.addBlock(Blocks.AIR, finder.width - 6, 5, 10);
		finder.addBlock(Blocks.AIR, finder.width - 6, 6, 10);
		finder.addBlock(Blocks.AIR, finder.width - 7, 6, 10);
		finder.fillWithOutline(2, 4, 4, 2, 6, 4, Blocks.AIR, Blocks.AIR, false);
		finder.fillWithOutline(finder.width - 3, 4, 4, finder.width - 3, 6, 4, Blocks.AIR, Blocks.AIR, false);
		finder.addBlock(blockState_1, 2, 4, 5);
		finder.addBlock(blockState_1, 2, 3, 4);
		finder.addBlock(blockState_1, finder.width - 3, 4, 5);
		finder.addBlock(blockState_1, finder.width - 3, 3, 4);
		finder.fillWithOutline(1, 1, 3, 2, 2, 3, Blocks.SANDSTONE, Blocks.SANDSTONE, false);
		finder.fillWithOutline(finder.width - 3, 1, 3, finder.width - 2, 2, 3, Blocks.SANDSTONE, Blocks.SANDSTONE, false);
		finder.addBlock(Blocks.SANDSTONE, 1, 1, 2);
		finder.addBlock(Blocks.SANDSTONE, finder.width - 2, 1, 2);
		finder.addBlock(Blocks.SANDSTONE_SLAB, 1, 2, 2);
		finder.addBlock(Blocks.SANDSTONE_SLAB, finder.width - 2, 2, 2);
		finder.addBlock(blockState_4, 2, 1, 2);
		finder.addBlock(blockState_3, finder.width - 3, 1, 2);
		finder.fillWithOutline(4, 3, 5, 4, 3, 17, Blocks.SANDSTONE, Blocks.SANDSTONE, false);
		finder.fillWithOutline(finder.width - 5, 3, 5, finder.width - 5, 3, 17, Blocks.SANDSTONE, Blocks.SANDSTONE, false);
		finder.fillWithOutline(3, 1, 5, 4, 2, 16, Blocks.AIR, Blocks.AIR, false);
		finder.fillWithOutline(finder.width - 6, 1, 5, finder.width - 5, 2, 16, Blocks.AIR, Blocks.AIR, false);

		int int_7;
		for (int_7 = 5; int_7 <= 17; int_7 += 2) {
		    finder.addBlock(Blocks.CUT_SANDSTONE, 4, 1, int_7);
		    finder.addBlock(Blocks.CHISELED_SANDSTONE, 4, 2, int_7);
		    finder.addBlock(Blocks.CUT_SANDSTONE, finder.width - 5, 1, int_7);
		    finder.addBlock(Blocks.CHISELED_SANDSTONE, finder.width - 5, 2, int_7);
		}

		finder.addBlock(Blocks.ORANGE_TERRACOTTA, 10, 0, 7);
		finder.addBlock(Blocks.ORANGE_TERRACOTTA, 10, 0, 8);
		finder.addBlock(Blocks.ORANGE_TERRACOTTA, 9, 0, 9);
		finder.addBlock(Blocks.ORANGE_TERRACOTTA, 11, 0, 9);
		finder.addBlock(Blocks.ORANGE_TERRACOTTA, 8, 0, 10);
		finder.addBlock(Blocks.ORANGE_TERRACOTTA, 12, 0, 10);
		finder.addBlock(Blocks.ORANGE_TERRACOTTA, 7, 0, 10);
		finder.addBlock(Blocks.ORANGE_TERRACOTTA, 13, 0, 10);
		finder.addBlock(Blocks.ORANGE_TERRACOTTA, 9, 0, 11);
		finder.addBlock(Blocks.ORANGE_TERRACOTTA, 11, 0, 11);
		finder.addBlock(Blocks.ORANGE_TERRACOTTA, 10, 0, 12);
		finder.addBlock(Blocks.ORANGE_TERRACOTTA, 10, 0, 13);
		finder.addBlock(Blocks.BLUE_TERRACOTTA, 10, 0, 10);

		for (int_7 = 0; int_7 <= finder.width - 1; int_7 += finder.width - 1) {
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7, 2, 1);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7, 2, 2);
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7, 2, 3);
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7, 3, 1);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7, 3, 2);
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7, 3, 3);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7, 4, 1);
		    finder.addBlock(Blocks.CHISELED_SANDSTONE, int_7, 4, 2);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7, 4, 3);
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7, 5, 1);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7, 5, 2);
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7, 5, 3);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7, 6, 1);
		    finder.addBlock(Blocks.CHISELED_SANDSTONE, int_7, 6, 2);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7, 6, 3);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7, 7, 1);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7, 7, 2);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7, 7, 3);
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7, 8, 1);
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7, 8, 2);
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7, 8, 3);
		}

		for (int_7 = 2; int_7 <= finder.width - 3; int_7 += finder.width - 3 - 2) {
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7 - 1, 2, 0);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7, 2, 0);
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7 + 1, 2, 0);
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7 - 1, 3, 0);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7, 3, 0);
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7 + 1, 3, 0);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7 - 1, 4, 0);
		    finder.addBlock(Blocks.CHISELED_SANDSTONE, int_7, 4, 0);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7 + 1, 4, 0);
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7 - 1, 5, 0);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7, 5, 0);
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7 + 1, 5, 0);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7 - 1, 6, 0);
		    finder.addBlock(Blocks.CHISELED_SANDSTONE, int_7, 6, 0);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7 + 1, 6, 0);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7 - 1, 7, 0);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7, 7, 0);
		    finder.addBlock(Blocks.ORANGE_TERRACOTTA, int_7 + 1, 7, 0);
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7 - 1, 8, 0);
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7, 8, 0);
		    finder.addBlock(Blocks.CUT_SANDSTONE, int_7 + 1, 8, 0);
		}

		finder.fillWithOutline(8, 4, 0, 12, 6, 0, Blocks.CUT_SANDSTONE, Blocks.CUT_SANDSTONE, false);
		finder.addBlock(Blocks.AIR, 8, 6, 0);
		finder.addBlock(Blocks.AIR, 12, 6, 0);
		finder.addBlock(Blocks.ORANGE_TERRACOTTA, 9, 5, 0);
		finder.addBlock(Blocks.CHISELED_SANDSTONE, 10, 5, 0);
		finder.addBlock(Blocks.ORANGE_TERRACOTTA, 11, 5, 0);
		finder.fillWithOutline(8, -14, 8, 12, -11, 12, Blocks.CUT_SANDSTONE, Blocks.CUT_SANDSTONE, false);
		finder.fillWithOutline(8, -10, 8, 12, -10, 12, Blocks.CHISELED_SANDSTONE, Blocks.CHISELED_SANDSTONE, false);
		finder.fillWithOutline(8, -9, 8, 12, -9, 12, Blocks.CUT_SANDSTONE, Blocks.CUT_SANDSTONE, false);
		finder.fillWithOutline(8, -8, 8, 12, -1, 12, Blocks.SANDSTONE, Blocks.SANDSTONE, false);
		finder.fillWithOutline(9, -11, 9, 11, -1, 11, Blocks.AIR, Blocks.AIR, false);
		finder.addBlock(Blocks.STONE_PRESSURE_PLATE, 10, -11, 10);
		finder.fillWithOutline(9, -13, 9, 11, -13, 11, Blocks.TNT, Blocks.AIR, false);
		finder.addBlock(Blocks.AIR, 8, -11, 10);
		finder.addBlock(Blocks.AIR, 8, -10, 10);
		finder.addBlock(Blocks.CHISELED_SANDSTONE, 7, -10, 10);
		finder.addBlock(Blocks.CUT_SANDSTONE, 7, -11, 10);
		finder.addBlock(Blocks.AIR, 12, -11, 10);
		finder.addBlock(Blocks.AIR, 12, -10, 10);
		finder.addBlock(Blocks.CHISELED_SANDSTONE, 13, -10, 10);
		finder.addBlock(Blocks.CUT_SANDSTONE, 13, -11, 10);
		finder.addBlock(Blocks.AIR, 10, -11, 8);
		finder.addBlock(Blocks.AIR, 10, -10, 8);
		finder.addBlock(Blocks.CHISELED_SANDSTONE, 10, -10, 7);
		finder.addBlock(Blocks.CUT_SANDSTONE, 10, -11, 7);
		finder.addBlock(Blocks.AIR, 10, -11, 12);
		finder.addBlock(Blocks.AIR, 10, -10, 12);
		finder.addBlock(Blocks.CHISELED_SANDSTONE, 10, -10, 13);
		finder.addBlock(Blocks.CUT_SANDSTONE, 10, -11, 13);
		}

	static void jungle(PieceFinder finder) {
		Block eastStairs = Blocks.COBBLESTONE_STAIRS;
		Block westStairs = Blocks.COBBLESTONE_STAIRS;
		Block southStairs = Blocks.COBBLESTONE_STAIRS;
		Block northStairs = Blocks.COBBLESTONE_STAIRS;
		finder.addBlock(northStairs, 5, 9, 6);
		finder.addBlock(northStairs, 6, 9, 6);
		finder.addBlock(southStairs, 5, 9, 8);
		finder.addBlock(southStairs, 6, 9, 8);
		finder.addBlock(northStairs, 4, 0, 0);
		finder.addBlock(northStairs, 5, 0, 0);
		finder.addBlock(northStairs, 6, 0, 0);
		finder.addBlock(northStairs, 7, 0, 0);
		finder.addBlock(northStairs, 4, 1, 8);
		finder.addBlock(northStairs, 4, 2, 9);
		finder.addBlock(northStairs, 4, 3, 10);
		finder.addBlock(northStairs, 7, 1, 8);
		finder.addBlock(northStairs, 7, 2, 9);
		finder.addBlock(northStairs, 7, 3, 10);
		finder.addBlock(eastStairs, 4, 4, 5);
		finder.addBlock(westStairs, 7, 4, 5);
		finder.addBlock((Blocks.TRIPWIRE_HOOK), 1, -3, 8);
		finder.addBlock((Blocks.TRIPWIRE_HOOK), 4, -3, 8);
		finder.addBlock(((Blocks.TRIPWIRE)), 2, -3, 8);
		finder.addBlock(((Blocks.TRIPWIRE)), 3, -3, 8);
		Block blockState_5 = (Blocks.REDSTONE_WIRE);
		finder.addBlock(Blocks.REDSTONE_WIRE, 5, -3, 7);
		finder.addBlock(blockState_5, 5, -3, 6);
		finder.addBlock(blockState_5, 5, -3, 5);
		finder.addBlock(blockState_5, 5, -3, 4);
		finder.addBlock(blockState_5, 5, -3, 3);
		finder.addBlock(blockState_5, 5, -3, 2);
		finder.addBlock((Blocks.REDSTONE_WIRE), 5, -3, 1);
		finder.addBlock(Blocks.REDSTONE_WIRE, 4, -3, 1);
		finder.addBlock(Blocks.MOSSY_COBBLESTONE, 3, -3, 1);

		finder.addBlock(Blocks.VINE, 3, -2, 2);
		finder.addBlock((Blocks.TRIPWIRE_HOOK), 7, -3, 1);
		finder.addBlock((Blocks.TRIPWIRE_HOOK), 7, -3, 5);
		finder.addBlock(((Blocks.TRIPWIRE)), 7, -3, 2);
		finder.addBlock(((Blocks.TRIPWIRE)), 7, -3, 3);
		finder.addBlock(((Blocks.TRIPWIRE)), 7, -3, 4);
		finder.addBlock(Blocks.REDSTONE_WIRE, 8, -3, 6);
		finder.addBlock((Blocks.REDSTONE_WIRE), 9, -3, 6);
		finder.addBlock((Blocks.REDSTONE_WIRE), 9, -3, 5);
		finder.addBlock(Blocks.MOSSY_COBBLESTONE, 9, -3, 4);
		finder.addBlock(Blocks.REDSTONE_WIRE, 9, -2, 4);

		finder.addBlock(Blocks.VINE, 8, -1, 3);
		finder.addBlock(Blocks.VINE, 8, -2, 3);

		finder.addBlock(Blocks.MOSSY_COBBLESTONE, 9, -3, 2);
		finder.addBlock(Blocks.MOSSY_COBBLESTONE, 8, -3, 1);
		finder.addBlock(Blocks.MOSSY_COBBLESTONE, 4, -3, 5);
		finder.addBlock(Blocks.MOSSY_COBBLESTONE, 5, -2, 5);
		finder.addBlock(Blocks.MOSSY_COBBLESTONE, 5, -1, 5);
		finder.addBlock(Blocks.MOSSY_COBBLESTONE, 6, -3, 5);
		finder.addBlock(Blocks.MOSSY_COBBLESTONE, 7, -2, 5);
		finder.addBlock(Blocks.MOSSY_COBBLESTONE, 7, -1, 5);
		finder.addBlock(Blocks.MOSSY_COBBLESTONE, 8, -3, 5);
		finder.addBlock(Blocks.CHISELED_STONE_BRICKS, 8, -2, 11);
		finder.addBlock(Blocks.CHISELED_STONE_BRICKS, 9, -2, 11);
		finder.addBlock(Blocks.CHISELED_STONE_BRICKS, 10, -2, 11);
		Block blockState_6 = (Blocks.LEVER);
		finder.addBlock(blockState_6, 8, -2, 12);
		finder.addBlock(blockState_6, 9, -2, 12);
		finder.addBlock(blockState_6, 10, -2, 12);
		finder.addBlock(Blocks.MOSSY_COBBLESTONE, 10, -2, 9);
		finder.addBlock(Blocks.REDSTONE_WIRE, 8, -2, 9);
		finder.addBlock(Blocks.REDSTONE_WIRE, 8, -2, 10);
		finder.addBlock(Blocks.REDSTONE_WIRE, 10, -1, 9);
		finder.addBlock(Blocks.STICKY_PISTON, 9, -2, 8);
		finder.addBlock(Blocks.STICKY_PISTON, 10, -2, 8);
		finder.addBlock(Blocks.STICKY_PISTON, 10, -1, 8);
		finder.addBlock(Blocks.REPEATER, 10, -2, 10);
		}

	static void swamp(PieceFinder finder) {
		finder.fillWithOutline(1, 1, 1, 5, 1, 7, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS, false);
		finder.fillWithOutline(1, 4, 2, 5, 4, 7, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS, false);
		finder.fillWithOutline(2, 1, 0, 4, 1, 0, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS, false);
		finder.fillWithOutline(2, 2, 2, 3, 3, 2, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS, false);
		finder.fillWithOutline(1, 2, 3, 1, 3, 6, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS, false);
		finder.fillWithOutline(5, 2, 3, 5, 3, 6, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS, false);
		finder.fillWithOutline(2, 2, 7, 4, 3, 7, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS, false);
		finder.fillWithOutline(1, 0, 2, 1, 3, 2, Blocks.OAK_LOG, Blocks.OAK_LOG, false);
		finder.fillWithOutline(5, 0, 2, 5, 3, 2, Blocks.OAK_LOG, Blocks.OAK_LOG, false);
		finder.fillWithOutline(1, 0, 7, 1, 3, 7, Blocks.OAK_LOG, Blocks.OAK_LOG, false);
		finder.fillWithOutline(5, 0, 7, 5, 3, 7, Blocks.OAK_LOG, Blocks.OAK_LOG, false);
		finder.addBlock(Blocks.OAK_FENCE, 2, 3, 2);
		finder.addBlock(Blocks.OAK_FENCE, 3, 3, 7);
		finder.addBlock(Blocks.AIR, 1, 3, 4);
		finder.addBlock(Blocks.AIR, 5, 3, 4);
		finder.addBlock(Blocks.AIR, 5, 3, 5);
		finder.addBlock(Blocks.POTTED_RED_MUSHROOM, 1, 3, 5);
		finder.addBlock(Blocks.CRAFTING_TABLE, 3, 2, 6);
		finder.addBlock(Blocks.CAULDRON, 4, 2, 6);
		finder.addBlock(Blocks.OAK_FENCE, 1, 2, 1);
		finder.addBlock(Blocks.OAK_FENCE, 5, 2, 1);
		Block northStairs = Blocks.SPRUCE_STAIRS;
		Block eastStairs = Blocks.SPRUCE_STAIRS;
		Block westStairs = Blocks.SPRUCE_STAIRS;
		Block southStairs = Blocks.SPRUCE_STAIRS;
		finder.fillWithOutline(0, 4, 1, 6, 4, 1, northStairs, northStairs, false);
		finder.fillWithOutline(0, 4, 2, 0, 4, 7, eastStairs, eastStairs, false);
		finder.fillWithOutline(6, 4, 2, 6, 4, 7, westStairs, westStairs, false);
		finder.fillWithOutline(0, 4, 8, 6, 4, 8, southStairs, southStairs, false);
		finder.addBlock(northStairs, 0, 4, 1);
		finder.addBlock(northStairs, 6, 4, 1);
		finder.addBlock(southStairs, 0, 4, 8);
		finder.addBlock(southStairs, 6, 4, 8);
		}

	static void igloo(PieceFinder finder) {
		Block snow = Blocks.SNOW_BLOCK;
		Block ice = Blocks.ICE;
		Block workBench = Blocks.CRAFTING_TABLE;

		finder.addBlock(workBench, 1, 1, 5);
		for(int y = 0; y < 3; y++) {
		    finder.addBlock(snow, 2, y, 0);
		    finder.addBlock(snow, 2, y, 1);
		    finder.addBlock(snow, 1, y, 2);
		    finder.addBlock(snow, 0, y, 3);
		    finder.addBlock(snow, 0, y, 4);
		    finder.addBlock(ice, 0, 1, 4);
		    finder.addBlock(snow, 0, y, 5);
		    finder.addBlock(snow, 1, y, 6);
		    finder.addBlock(snow, 2, y, 7);

		    finder.addBlock(snow, 3, y, 7);

		    finder.addBlock(snow, 4, y, 0);
		    finder.addBlock(snow, 4, y, 1);
		    finder.addBlock(snow, 5, y, 2);
		    finder.addBlock(snow, 6, y, 3);
		    finder.addBlock(snow, 6, y, 4);
		    finder.addBlock(ice, 6, 1, 4);
		    finder.addBlock(snow, 6, y, 5);
		    finder.addBlock(snow, 5, y, 6);
		    finder.addBlock(snow, 4, y, 7);
		}
		}

	static void endCity(PieceFinder finder) {
		Block air = Blocks.AIR;
		Block endstoneBricks = Blocks.END_STONE_BRICKS;
		Block purpur = Blocks.PURPUR_BLOCK;
		Block purpurPillar = Blocks.PURPUR_PILLAR;
		Block purpleGlass = Blocks.MAGENTA_STAINED_GLASS;
		finder.fillWithOutline(0, 0, 0, 7, 4, 7, endstoneBricks, null, false);
		finder.fillWithOutline(0, 0, 0, 0, 3, 0, purpurPillar, purpurPillar, false);
		finder.fillWithOutline(7, 0, 0, 7, 3, 0, purpurPillar, purpurPillar, false);
		finder.fillWithOutline(0, 0, 7, 0, 3, 7, purpurPillar, purpurPillar, false);
		finder.fillWithOutline(7, 0, 7, 7, 3, 7, purpurPillar, purpurPillar, false);
		finder.fillWithOutline(0, 0, 0, 7, 0, 7, purpur, purpur, false);
		finder.fillWithOutline(3, 1, 0, 4, 3, 0, air, air, false);
		finder.fillWithOutline(0, 2, 2, 0, 3, 2, purpleGlass, purpleGlass, false);
		finder.fillWithOutline(0, 2, 5, 0, 3, 5, purpleGlass, purpleGlass, false);
		finder.fillWithOutline(7, 2, 2, 7, 3, 2, purpleGlass, purpleGlass, false);
		finder.fillWithOutline(7, 2, 5, 7, 3, 5, purpleGlass, purpleGlass, false);
		}

	static void monument(PieceFinder finder) {
		Block prismarine = Blocks.PRISMARINE;
		Block prismarineBricks = Blocks.PRISMARINE_BRICKS;
		Block darkPrismarine = Blocks.DARK_PRISMARINE;
		Block seaLantern = Blocks.SEA_LANTERN;
		Block water = Blocks.WATER;
		for (int i = 0; i < 4; i++) {
		    int x = i >= 2 ? 7 : 0;
		    int z = i % 2 == 0 ? 0 : 7;

		    for (int y = 0; y < 3; y++) {
		        finder.addBlock(prismarineBricks, x, y, z);
		    }
		}
		for (int i = 0; i < 4; i++) {
		    int x = i >= 2 ? 6 : 1;
		    int z = i % 2 == 0 ? 1 : 6;
		    finder.addBlock(prismarineBricks, x, 3, z);
		}
		for (int x = 2; x <= 5; x++) {
		    for (int z = 2; z <= 5; z++) {
		        if (x == 2 || x == 5 || z == 2 || z == 5) {
		            finder.addBlock(prismarine, x, 4, z);
		        }
		    }
		}
		for (int i = 0; i < 4; i++) {
		    int x = i >= 2 ? 5 : 2;
		    int z = i % 2 == 0 ? 2 : 5;
		    finder.addBlock(prismarineBricks, x, 4, z);
		    finder.addBlock(seaLantern, x, 3, z);
		}
		}
}
