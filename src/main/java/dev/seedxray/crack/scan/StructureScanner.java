package dev.seedxray.crack.scan;

import dev.seedxray.crack.Kind;
import dev.seedxray.crack.Observation;
import dev.seedxray.sim.Dim;
import dev.seedxray.sim.WorldgenData;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.gen.structure.Structure;

/**
 * Recognises structures in the blocks the client has loaded and says which chunk the game started each one in.
 * Ported from the finders of SeedcrackerX (MIT, https://github.com/19MisterX98/SeedcrackerX): same layouts, same search
 * positions, same start-chunk rules. Block access is on the caller's thread.
 */
public final class StructureScanner {
	private static final Vec3i DESERT = new Vec3i(21, 15, 21);
	private static final Vec3i JUNGLE = new Vec3i(12, 10, 15);
	private static final Vec3i SWAMP = new Vec3i(7, 7, 9);
	private static final Vec3i IGLOO = new Vec3i(7, 5, 8);
	private static final Vec3i END_CITY = new Vec3i(8, 4, 8);
	private static final Vec3i MONUMENT = new Vec3i(8, 5, 8);

	private final BlockSource world;
	private final Map<Kind, List<PieceFinder>> finders = new EnumMap<>(Kind.class);
	private final Map<Kind, Set<Identifier>> biomes = new EnumMap<>(Kind.class);

	public StructureScanner(BlockSource world) {
		this.world = world;
		int minY = world.minY(), maxY = world.maxY();
		finders.put(Kind.DESERT_PYRAMID, temple(DESERT, Patterns::desert));
		finders.put(Kind.JUNGLE_TEMPLE, temple(JUNGLE, Patterns::jungle));
		finders.put(Kind.SWAMP_HUT, temple(SWAMP, Patterns::swamp));
		List<PieceFinder> igloo = new ArrayList<>();
		for (Direction d : Direction.Type.HORIZONTAL) {
			PieceFinder f = new PieceFinder(d, IGLOO, true);
			f.searchPositions = jigsawPositions(d, 3, 5, 0, 0, IGLOO, minY, maxY);
			Patterns.igloo(f);
			igloo.add(f);
		}
		finders.put(Kind.IGLOO, igloo);
		List<PieceFinder> monument = new ArrayList<>();
		PieceFinder m = new PieceFinder(Direction.NORTH, MONUMENT, false);
		m.searchPositions = column(56, 56, -1, -1);
		Patterns.monument(m);
		monument.add(m);
		finders.put(Kind.MONUMENT, monument);
		List<PieceFinder> city = new ArrayList<>();
		for (Direction d : Direction.Type.HORIZONTAL) {
			PieceFinder f = new PieceFinder(d, END_CITY, false);
			f.searchPositions = column(55, 100, -1, -1);
			Patterns.endCity(f);
			city.add(f);
		}
		finders.put(Kind.END_CITY, city);
		for (Kind kind : Kind.values()) biomes.put(kind, validBiomes(kind));
	}

	private interface Layout {
		void build(PieceFinder finder);
	}

	private List<PieceFinder> temple(Vec3i size, Layout layout) {
		List<PieceFinder> list = new ArrayList<>();
		for (Direction d : Direction.Type.HORIZONTAL) {
			PieceFinder f = new PieceFinder(d, size, false);
			f.searchPositions = column(0, 200, 0, 0);
			layout.build(f);
			list.add(f);
		}
		return list;
	}

	/** Search positions of a chunk: one column (x, z) over y in [minY, maxY], or all columns when x is -1. */
	private static List<BlockPos> column(int minY, int maxY, int x, int z) {
		List<BlockPos> list = new ArrayList<>();
		for (int y = minY; y <= maxY; y++) {
			if (x >= 0) {
				list.add(new BlockPos(x, y, z));
			} else {
				for (int xx = 0; xx < 16; xx++) {
					for (int zz = 0; zz < 16; zz++) list.add(new BlockPos(xx, y, zz));
				}
			}
		}
		return list;
	}

	/** Where a jigsaw piece's origin can be inside its start chunk (the piece rotates about a pivot). */
	private static List<BlockPos> jigsawPositions(Direction direction, int xRotation, int zRotation, int xOffset, int zOffset, Vec3i size, int minY, int maxY) {
		int x = switch (direction) {
			case EAST -> xRotation - size.getZ() + 1 + zRotation - zOffset;
			case SOUTH -> xRotation - size.getX() + 1 + xRotation - xOffset;
			case WEST -> xRotation - zRotation + zOffset;
			default -> xOffset;
		};
		int z = switch (direction) {
			case EAST -> zRotation - xRotation + xOffset;
			case SOUTH -> zRotation - size.getZ() + 1 + zRotation - zOffset;
			case WEST -> zRotation - size.getX() + 1 + xRotation - xOffset;
			default -> zOffset;
		};
		List<BlockPos> list = new ArrayList<>();
		for (int y = minY; y < maxY; y++) list.add(new BlockPos(x, y, z));
		return list;
	}

	private static Set<Identifier> validBiomes(Kind kind) {
		Set<Identifier> set = new HashSet<>();
		var registry = WorldgenData.get().registries.getOrThrow(RegistryKeys.STRUCTURE);
		Structure structure = registry.getOrThrow(RegistryKey.of(RegistryKeys.STRUCTURE, Identifier.ofVanilla(kind.structureId))).value();
		structure.getValidBiomes().stream().forEach(b -> b.getKey().ifPresent(k -> set.add(k.getValue())));
		return set;
	}

	/** Which dimension a kind generates in. */
	public static Dim dimensionOf(Kind kind) {
		return kind == Kind.END_CITY ? Dim.END : Dim.OVERWORLD;
	}

	private boolean biomeOk(Kind kind, int chunkX, int chunkZ) {
		return biomes.get(kind).contains(world.biome((chunkX << 4) + 8, 64, (chunkZ << 4) + 8));
	}

	private static final Map<Kind, Block> HINT = new EnumMap<>(Kind.class);

	static {
		HINT.put(Kind.DESERT_PYRAMID, Blocks.CHISELED_SANDSTONE);
		HINT.put(Kind.JUNGLE_TEMPLE, Blocks.MOSSY_COBBLESTONE);
		HINT.put(Kind.SWAMP_HUT, Blocks.SPRUCE_PLANKS);
		HINT.put(Kind.IGLOO, Blocks.CRAFTING_TABLE);
		HINT.put(Kind.MONUMENT, Blocks.PRISMARINE_BRICKS);
		HINT.put(Kind.END_CITY, Blocks.END_STONE_BRICKS);
		HINT.put(Kind.BURIED_TREASURE, Blocks.CHEST);
	}

	private boolean hint(Kind kind, int ox, int oz) {
		Block block = HINT.get(kind);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (world.chunkHas(ox + dx, oz + dz, block)) return true;
			}
		}
		return false;
	}

	/**
	 * Structures that start in chunk {@code (ox, oz)}. All 8 neighbours must be loaded (layouts can spill into them);
	 * the caller checks that.
	 */
	public List<Observation> scan(Dim dim, int ox, int oz) {
		List<Observation> out = new ArrayList<>();
		for (Kind kind : Kind.values()) {
			if (kind == Kind.BURIED_TREASURE) continue;
			if (dimensionOf(kind) != dim) continue;
			if (!biomeOk(kind, ox, oz) || !hint(kind, ox, oz)) continue;
			for (PieceFinder f : finders.get(kind)) {
				List<BlockPos> found = f.findAt(world, ox << 4, oz << 4);
				if (found.isEmpty()) continue;
				BlockPos pos = found.get(0);
				switch (kind) {
					case MONUMENT -> out.add(new Observation(kind, ox + 1, oz + 1));
					case END_CITY -> out.add(new Observation(kind, (pos.getX() + 1) >> 4, (pos.getZ() + 1) >> 4));
					default -> out.add(new Observation(kind, ox, oz));
				}
				break;
			}
		}
		return out;
	}

	private static final Set<Block> CHEST_HOLDERS = Set.of(
		Blocks.SANDSTONE, Blocks.STONE, Blocks.ANDESITE, Blocks.GRANITE, Blocks.DIORITE, Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.GRAVEL
	);

	/** A buried treasure chest is always at (9, y, 9) of its chunk. */
	public Observation scanTreasure(int cx, int cz) {
		if (!biomeOk(Kind.BURIED_TREASURE, cx, cz) || !world.chunkHas(cx, cz, Blocks.CHEST)) return null;
		for (int y = world.minY(); y < world.maxY(); y++) {
			int x = (cx << 4) + 9, z = (cz << 4) + 9;
			if (world.block(x, y, z) == Blocks.CHEST && CHEST_HOLDERS.contains(world.block(x, y - 1, z))) {
				return new Observation(Kind.BURIED_TREASURE, cx, cz);
			}
		}
		return null;
	}

	/** Heights of the ten End pillars in the order of the pillar ring, or null when they are not all visible. */
	public List<Integer> scanPillars() {
		List<Integer> heights = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			int x = (int) Math.floor(42.0 * Math.cos(2.0 * (-Math.PI + (Math.PI / 10.0) * i)));
			int z = (int) Math.floor(42.0 * Math.sin(2.0 * (-Math.PI + (Math.PI / 10.0) * i)));
			if (!world.chunkLoaded(x >> 4, z >> 4)) return null;
			int found = Integer.MIN_VALUE;
			for (int y = 76; y <= 76 + 3 * 10; y++) {
				if (world.block(x, y, z) == Blocks.BEDROCK) {
					found = y;
					break;
				}
			}
			if (found == Integer.MIN_VALUE) return null;
			heights.add(found);
		}
		return heights;
	}
}
