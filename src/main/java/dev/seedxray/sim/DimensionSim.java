package dev.seedxray.sim;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.biome.source.TheEndBiomeSource;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.registry.RegistryKey;

/**
 * A vanilla chunk generator for one dimension of one seed, built purely from the game's own world
 * generation code. Everything the mod predicts (structures, ores) is derived from this.
 */
public final class DimensionSim {
	public final Dim dim;
	public final long seed;
	public final DynamicRegistryManager registries;
	public final NoiseChunkGenerator generator;
	public final BiomeSource biomeSource;
	public final NoiseConfig noise;
	public final StructurePlacementCalculator placements;
	public final HeightLimitView heightView;

	public DimensionSim(Dim dim, long seed) {
		this.dim = dim;
		this.seed = seed;
		WorldgenData data = WorldgenData.get();
		this.registries = data.registries;

		RegistryKey<ChunkGeneratorSettings> settingsKey;
		int minY;
		int height;
		switch (dim) {
			case NETHER -> {
				settingsKey = ChunkGeneratorSettings.NETHER;
				biomeSource = multiNoise(MultiNoiseBiomeSourceParameterLists.NETHER);
				minY = 0;
				height = 256;
			}
			case END -> {
				settingsKey = ChunkGeneratorSettings.END;
				biomeSource = TheEndBiomeSource.createVanilla(registries.getOrThrow(RegistryKeys.BIOME));
				minY = 0;
				height = 256;
			}
			default -> {
				settingsKey = ChunkGeneratorSettings.OVERWORLD;
				biomeSource = multiNoise(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
				minY = -64;
				height = 384;
			}
		}
		RegistryEntry<ChunkGeneratorSettings> settings = registries.getOrThrow(RegistryKeys.CHUNK_GENERATOR_SETTINGS).getOrThrow(settingsKey);
		this.generator = new NoiseChunkGenerator(biomeSource, settings);
		this.noise = NoiseConfig.create(registries, settingsKey, seed);
		this.placements = StructurePlacementCalculator.create(noise, seed, biomeSource, registries.getOrThrow(RegistryKeys.STRUCTURE_SET));
		this.heightView = HeightLimitView.create(minY, height);
	}

	private BiomeSource multiNoise(RegistryKey<MultiNoiseBiomeSourceParameterList> key) {
		RegistryEntry<MultiNoiseBiomeSourceParameterList> preset = registries.getOrThrow(RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getOrThrow(key);
		return MultiNoiseBiomeSource.create(preset);
	}

	public int minY() {
		return heightView.getBottomY();
	}

	public int maxY() {
		return heightView.getTopYInclusive();
	}
}
