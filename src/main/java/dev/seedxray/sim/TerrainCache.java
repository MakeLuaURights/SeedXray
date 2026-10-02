package dev.seedxray.sim;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.chunk.PalettesFactory;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.carver.CarverContext;
import net.minecraft.world.gen.carver.CarvingMask;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

/**
 * Real vanilla terrain (noise stage, surface rules, caves) for chunks, generated without a world
 * with the game's own generator code. Used to know exactly which blocks ores can replace.
 * Keeps a bounded number of chunks. Not thread safe.
 */
public final class TerrainCache {
	private final DimensionSim sim;
	private final Registry<Biome> biomes;
	private final PalettesFactory palettes;
	private final BiomeAccess biomeAccess;
	private final Long2ObjectLinkedOpenHashMap<ProtoChunk> chunks = new Long2ObjectLinkedOpenHashMap<>();
	private final int maxChunks;
	private final boolean carve;

	public TerrainCache(DimensionSim sim, int maxChunks, boolean carve) {
		this.sim = sim;
		this.maxChunks = maxChunks;
		this.carve = carve;
		this.biomes = sim.registries.getOrThrow(RegistryKeys.BIOME);
		this.palettes = PalettesFactory.fromRegistryManager(sim.registries);
		this.biomeAccess = new BiomeAccess((x, y, z) -> sim.biomeSource.getBiome(x, y, z, sim.noise.getMultiNoiseSampler()), BiomeAccess.hashSeed(sim.seed));
	}

	public ProtoChunk get(int cx, int cz) {
		long key = ChunkPos.toLong(cx, cz);
		ProtoChunk chunk = chunks.getAndMoveToLast(key);
		if (chunk == null) {
			chunk = build(cx, cz);
			chunks.putAndMoveToLast(key, chunk);
			while (chunks.size() > maxChunks) {
				chunks.removeFirst();
			}
		}
		return chunk;
	}

	public BlockState getBlockState(int x, int y, int z) {
		return get(x >> 4, z >> 4).getBlockState(new BlockPos(x, y, z));
	}

	private ProtoChunk build(int cx, int cz) {
		ProtoChunk chunk = new ProtoChunk(new ChunkPos(cx, cz), UpgradeData.NO_UPGRADE_DATA, sim.heightView, palettes, null);
		var generator = sim.generator;
		var settings = generator.getSettings().value();
		AquiferSampler.FluidLevelSampler fluids = generator.fluidLevelSampler.get();
		ChunkNoiseSampler sampler = ChunkNoiseSampler.create(chunk, sim.noise, DensityFunctionTypes.Beardifier.INSTANCE, settings, fluids, Blender.getNoBlending());
		chunk.getOrCreateChunkNoiseSampler(c -> sampler);
		generator.populateBiomes(sim.noise, Blender.getNoBlending(), null, chunk).join();
		generator.populateNoise(Blender.getNoBlending(), sim.noise, null, chunk).join();
		generator.buildSurface(chunk, new HeightContext(generator, sim.heightView), sim.noise, null, biomeAccess, biomes, Blender.getNoBlending());
		if (carve) carve(chunk, sampler);
		chunk.chunkNoiseSampler = null; // big and no longer needed
		return chunk;
	}

	/** Same as {@code NoiseChunkGenerator.carve}, minus the ChunkRegion. */
	private void carve(ProtoChunk chunk, ChunkNoiseSampler sampler) {
		var generator = sim.generator;
		ChunkRandom random = new ChunkRandom(new CheckedRandom(0L));
		ChunkPos pos = chunk.getPos();
		AquiferSampler aquifer = sampler.getAquiferSampler();
		CarverContext context = new CarverContext(generator, sim.registries, chunk.getHeightLimitView(), sampler, sim.noise, generator.getSettings().value().surfaceRule());
		CarvingMask mask = chunk.getOrCreateCarvingMask();
		for (int dx = -8; dx <= 8; dx++) {
			for (int dz = -8; dz <= 8; dz++) {
				ChunkPos other = new ChunkPos(pos.x + dx, pos.z + dz);
				RegistryEntry<Biome> biome = sim.biomeSource.getBiome(BiomeCoords.fromBlock(other.getStartX()), 0, BiomeCoords.fromBlock(other.getStartZ()), sim.noise.getMultiNoiseSampler());
				int l = 0;
				for (RegistryEntry<ConfiguredCarver<?>> carver : generator.getGenerationSettings(biome).getCarversForStep()) {
					ConfiguredCarver<?> configured = carver.value();
					random.setCarverSeed(sim.seed + l, other.x, other.z);
					if (configured.shouldCarve(random)) {
						configured.carve(context, chunk, biomeAccess::getBiome, random, aquifer, other, mask);
					}
					l++;
				}
			}
		}
	}
}
