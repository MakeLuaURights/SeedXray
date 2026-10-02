package dev.seedxray.sim;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.Heightmap;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.OreFeature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.ScatteredOreFeature;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer;
import net.minecraft.world.gen.placementmodifier.BiomePlacementModifier;
import net.minecraft.world.gen.placementmodifier.CountPlacementModifier;
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import net.minecraft.world.gen.placementmodifier.RarityFilterPlacementModifier;
import net.minecraft.world.gen.placementmodifier.SquarePlacementModifier;
import org.slf4j.Logger;

/**
 * Predicts ore veins the way the game generates them: same decoration seeds, same feature order,
 * same placement modifiers and the same vein shapes ({@code OreFeature} / {@code ScatteredOreFeature}).
 * Terrain comes from {@link TerrainCache} (vanilla noise, surface rules and carvers). Not thread safe.
 */
public final class OreSimulator {
	private static final Logger LOGGER = LogUtils.getLogger();

	private record OreFeatureEntry(PlacedFeature placed, OreFeatureConfig config, boolean scattered, int step, int index) {
	}

	private final DimensionSim sim;
	private final List<OreFeatureEntry> features = new ArrayList<>();
	private final BiomeAccess biomeAccess;
	private final HeightContext heightContext;
	private final TerrainCache terrain;
	/** Blocks placed by earlier veins in the chunk currently being simulated. */
	private final Long2ObjectOpenHashMap<BlockState> overlay = new Long2ObjectOpenHashMap<>();

	public OreSimulator(DimensionSim sim, TerrainCache terrain) {
		this.sim = sim;
		this.terrain = terrain;
		this.biomeAccess = new BiomeAccess(
			(x, y, z) -> sim.biomeSource.getBiome(x, y, z, sim.noise.getMultiNoiseSampler()), BiomeAccess.hashSeed(sim.seed)
		);
		this.heightContext = new HeightContext(sim.generator, sim.heightView);
		collectFeatures();
	}

	private void collectFeatures() {
		List<PlacedFeatureIndexer.IndexedFeatures> indexed = sim.generator.indexedFeaturesListSupplier.get();
		for (int step = 0; step < indexed.size(); step++) {
			List<PlacedFeature> list = indexed.get(step).features();
			for (int index = 0; index < list.size(); index++) {
				PlacedFeature placed = list.get(index);
				ConfiguredFeature<?, ?> configured = placed.feature().value();
				boolean ore = configured.feature() instanceof OreFeature;
				boolean scattered = configured.feature() instanceof ScatteredOreFeature;
				if ((ore || scattered) && configured.config() instanceof OreFeatureConfig config && supported(placed)) {
					features.add(new OreFeatureEntry(placed, config, scattered, step, index));
				}
			}
		}
	}

	private static boolean supported(PlacedFeature placed) {
		for (PlacementModifier m : placed.placementModifiers()) {
			if (!(m instanceof CountPlacementModifier || m instanceof SquarePlacementModifier || m instanceof HeightRangePlacementModifier
				|| m instanceof RarityFilterPlacementModifier || m instanceof BiomePlacementModifier)) {
				return false;
			}
		}
		return true;
	}

	/** All ore blocks the feature pass of chunk (cx, cz) places. */
	public OreChunk simulate(int cx, int cz) {
		overlay.clear();
		List<Long> positions = new ArrayList<>();
		List<Block> blocks = new ArrayList<>();
		ChunkRandom random = new ChunkRandom(new Xoroshiro128PlusPlusRandom(RandomSeed.getSeed()));
		long populationSeed = random.setPopulationSeed(sim.seed, cx * 16, cz * 16);
		BlockPos origin = new BlockPos(cx * 16, sim.minY(), cz * 16);
		for (OreFeatureEntry entry : features) {
			random.setDecoratorSeed(populationSeed, entry.index, entry.step);
			try {
				place(entry, random, origin, positions, blocks);
			} catch (Exception e) {
				LOGGER.warn("[SeedXray] Ore simulation failed for {}: {}", entry.placed, e.toString());
			}
		}
		collectNoiseVeins(cx, cz, positions, blocks);
		long[] pos = new long[positions.size()];
		for (int i = 0; i < pos.length; i++) pos[i] = positions.get(i);
		return new OreChunk(cx, cz, pos, blocks.toArray(new Block[0]));
	}

	/** Blocks the noise stage itself places as big veins (not features): copper/iron veins with raw ore blocks. */
	private static final java.util.Set<Block> NOISE_VEIN_BLOCKS = java.util.Set.of(
		Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.RAW_COPPER_BLOCK, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.RAW_IRON_BLOCK
	);

	private void collectNoiseVeins(int cx, int cz, List<Long> outPos, List<Block> outBlocks) {
		if (sim.dim != Dim.OVERWORLD) return;
		ProtoChunk chunk = terrain.get(cx, cz);
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (ChunkSection section : chunk.getSectionArray()) {
			if (section == null || section.isEmpty() || !section.hasAny(state -> NOISE_VEIN_BLOCKS.contains(state.getBlock()))) continue;
			int baseY = chunk.sectionIndexToCoord(java.util.Arrays.asList(chunk.getSectionArray()).indexOf(section)) * 16;
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						Block block = section.getBlockState(x, y, z).getBlock();
						if (NOISE_VEIN_BLOCKS.contains(block)) {
							outPos.add(BlockPos.asLong(cx * 16 + x, baseY + y, cz * 16 + z));
							outBlocks.add(block);
						}
					}
				}
			}
		}
	}

	/** Mirrors {@code PlacedFeature.generate}: placement modifiers feed positions depth first, sharing one random. */
	private void place(OreFeatureEntry entry, Random random, BlockPos pos, List<Long> outPos, List<Block> outBlocks) {
		applyModifiers(entry, entry.placed.placementModifiers(), 0, random, pos, outPos, outBlocks);
	}

	private void applyModifiers(OreFeatureEntry entry, List<PlacementModifier> modifiers, int i, Random random, BlockPos pos, List<Long> outPos, List<Block> outBlocks) {
		if (i == modifiers.size()) {
			generateOre(entry, random, pos, outPos, outBlocks);
			return;
		}
		PlacementModifier modifier = modifiers.get(i);
		if (modifier instanceof CountPlacementModifier count) {
			int n = count.count.get(random);
			for (int k = 0; k < n; k++) applyModifiers(entry, modifiers, i + 1, random, pos, outPos, outBlocks);
		} else if (modifier instanceof SquarePlacementModifier) {
			int x = random.nextInt(16) + pos.getX();
			int z = random.nextInt(16) + pos.getZ();
			applyModifiers(entry, modifiers, i + 1, random, new BlockPos(x, pos.getY(), z), outPos, outBlocks);
		} else if (modifier instanceof HeightRangePlacementModifier range) {
			applyModifiers(entry, modifiers, i + 1, random, pos.withY(range.height.get(random, heightContext)), outPos, outBlocks);
		} else if (modifier instanceof RarityFilterPlacementModifier rarity) {
			if (random.nextFloat() < 1.0F / rarity.chance) applyModifiers(entry, modifiers, i + 1, random, pos, outPos, outBlocks);
		} else if (modifier instanceof BiomePlacementModifier) {
			RegistryEntry<Biome> biome = biomeAccess.getBiome(pos);
			if (sim.generator.getGenerationSettings(biome).isFeatureAllowed(entry.placed)) {
				applyModifiers(entry, modifiers, i + 1, random, pos, outPos, outBlocks);
			}
		}
	}

	// ---- terrain ----

	/** Same as {@code StructureWorldAccess.getTopY(OCEAN_FLOOR_WG, x, z)}. */
	private int surfaceHeight(int x, int z) {
		return terrain.get(x >> 4, z >> 4).sampleHeightmap(Heightmap.Type.OCEAN_FLOOR_WG, x, z) + 1;
	}

	private BlockState stateAt(int x, int y, int z) {
		BlockState placed = overlay.get(BlockPos.asLong(x, y, z));
		return placed != null ? placed : terrain.getBlockState(x, y, z);
	}

	private boolean exposedToAir(int x, int y, int z) {
		for (Direction d : Direction.values()) {
			if (stateAt(x + d.getOffsetX(), y + d.getOffsetY(), z + d.getOffsetZ()).isAir()) return true;
		}
		return false;
	}

	private boolean shouldPlace(BlockState state, Random random, OreFeatureConfig config, OreFeatureConfig.Target target, int x, int y, int z) {
		if (!target.target.test(state, random)) return false;
		float chance = config.discardOnAirChance;
		boolean keep = chance <= 0.0F || (chance < 1.0F && random.nextFloat() >= chance);
		return keep || !exposedToAir(x, y, z);
	}

	private void set(OreFeatureEntry entry, int x, int y, int z, BlockState state, List<Long> outPos, List<Block> outBlocks) {
		overlay.put(BlockPos.asLong(x, y, z), state);
		outPos.add(BlockPos.asLong(x, y, z));
		outBlocks.add(state.getBlock());
	}

	// ---- OreFeature / ScatteredOreFeature ----

	private void generateOre(OreFeatureEntry entry, Random random, BlockPos origin, List<Long> outPos, List<Block> outBlocks) {
		if (entry.scattered) {
			generateScattered(entry, random, origin, outPos, outBlocks);
		} else {
			generateVein(entry, random, origin, outPos, outBlocks);
		}
	}

	private void generateScattered(OreFeatureEntry entry, Random random, BlockPos origin, List<Long> outPos, List<Block> outBlocks) {
		OreFeatureConfig config = entry.config;
		int count = random.nextInt(config.size + 1);
		for (int j = 0; j < count; j++) {
			int spread = Math.min(j, 7);
			int dx = spread(random, spread);
			int dy = spread(random, spread);
			int dz = spread(random, spread);
			int x = origin.getX() + dx, y = origin.getY() + dy, z = origin.getZ() + dz;
			BlockState state = stateAt(x, y, z);
			for (OreFeatureConfig.Target target : config.targets) {
				if (shouldPlace(state, random, config, target, x, y, z)) {
					set(entry, x, y, z, target.state, outPos, outBlocks);
					break;
				}
			}
		}
	}

	private static int spread(Random random, int spread) {
		return Math.round((random.nextFloat() - random.nextFloat()) * spread);
	}

	private void generateVein(OreFeatureEntry entry, Random random, BlockPos blockPos, List<Long> outPos, List<Block> outBlocks) {
		OreFeatureConfig config = entry.config;
		float f = random.nextFloat() * (float) Math.PI;
		float g = config.size / 8.0F;
		int i = MathHelper.ceil((config.size / 16.0F * 2.0F + 1.0F) / 2.0F);
		double startX = blockPos.getX() + Math.sin(f) * g;
		double endX = blockPos.getX() - Math.sin(f) * g;
		double startZ = blockPos.getZ() + Math.cos(f) * g;
		double endZ = blockPos.getZ() - Math.cos(f) * g;
		double startY = blockPos.getY() + random.nextInt(3) - 2;
		double endY = blockPos.getY() + random.nextInt(3) - 2;
		int x0 = blockPos.getX() - MathHelper.ceil(g) - i;
		int y0 = blockPos.getY() - 2 - i;
		int z0 = blockPos.getZ() - MathHelper.ceil(g) - i;
		int horizontal = 2 * (MathHelper.ceil(g) + i);
		int vertical = 2 * (2 + i);

		boolean aboveGround = true;
		search:
		for (int s = x0; s <= x0 + horizontal; s++) {
			for (int t = z0; t <= z0 + horizontal; t++) {
				if (y0 <= surfaceHeight(s, t)) {
					aboveGround = false;
					break search;
				}
			}
		}
		if (aboveGround) return;

		int size = config.size;
		BitSet bits = new BitSet(horizontal * vertical * horizontal);
		double[] ds = new double[size * 4];
		for (int k = 0; k < size; k++) {
			float fk = (float) k / size;
			double d = MathHelper.lerp((double) fk, startX, endX);
			double e = MathHelper.lerp((double) fk, startY, endY);
			double gg = MathHelper.lerp((double) fk, startZ, endZ);
			double h = random.nextDouble() * size / 16.0;
			double l = ((MathHelper.sin((float) Math.PI * fk) + 1.0F) * h + 1.0) / 2.0;
			ds[k * 4] = d;
			ds[k * 4 + 1] = e;
			ds[k * 4 + 2] = gg;
			ds[k * 4 + 3] = l;
		}
		for (int k = 0; k < size - 1; k++) {
			if (!(ds[k * 4 + 3] <= 0.0)) {
				for (int m = k + 1; m < size; m++) {
					if (!(ds[m * 4 + 3] <= 0.0)) {
						double d = ds[k * 4] - ds[m * 4];
						double e = ds[k * 4 + 1] - ds[m * 4 + 1];
						double gg = ds[k * 4 + 2] - ds[m * 4 + 2];
						double h = ds[k * 4 + 3] - ds[m * 4 + 3];
						if (h * h > d * d + e * e + gg * gg) {
							if (h > 0.0) ds[m * 4 + 3] = -1.0;
							else ds[k * 4 + 3] = -1.0;
						}
					}
				}
			}
		}
		int minY = sim.minY();
		int maxY = sim.maxY();
		for (int mx = 0; mx < size; mx++) {
			double d = ds[mx * 4 + 3];
			if (d < 0.0) continue;
			double e = ds[mx * 4];
			double gg = ds[mx * 4 + 1];
			double h = ds[mx * 4 + 2];
			int n = Math.max(MathHelper.floor(e - d), x0);
			int o = Math.max(MathHelper.floor(gg - d), y0);
			int p = Math.max(MathHelper.floor(h - d), z0);
			int q = Math.max(MathHelper.floor(e + d), n);
			int r = Math.max(MathHelper.floor(gg + d), o);
			int s = Math.max(MathHelper.floor(h + d), p);
			for (int t = n; t <= q; t++) {
				double u = (t + 0.5 - e) / d;
				if (u * u >= 1.0) continue;
				for (int v = o; v <= r; v++) {
					double w = (v + 0.5 - gg) / d;
					if (u * u + w * w >= 1.0) continue;
					for (int aa = p; aa <= s; aa++) {
						double ab = (aa + 0.5 - h) / d;
						if (u * u + w * w + ab * ab >= 1.0 || v < minY || v > maxY) continue;
						int bit = t - x0 + (v - y0) * horizontal + (aa - z0) * horizontal * vertical;
						if (bits.get(bit)) continue;
						bits.set(bit);
						BlockState state = stateAt(t, v, aa);
						for (OreFeatureConfig.Target target : config.targets) {
							if (shouldPlace(state, random, config, target, t, v, aa)) {
								set(entry, t, v, aa, target.state, outPos, outBlocks);
								break;
							}
						}
					}
				}
			}
		}
	}

	/** Blocks any ore feature of this dimension can place (for the menu's "seed predicted" marker). */
	public java.util.Set<Block> producibleBlocks() {
		java.util.Set<Block> set = new java.util.HashSet<>();
		for (OreFeatureEntry e : features) {
			for (OreFeatureConfig.Target t : e.config.targets) set.add(t.state.getBlock());
		}
		return set;
	}
}
