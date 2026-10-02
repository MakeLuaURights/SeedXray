package dev.seedxray.sim;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import net.minecraft.datafixer.Schemas;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryLoader;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.resource.LifecycledResourceManager;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.VanillaDataPackProvider;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorage;
import org.slf4j.Logger;

/**
 * Vanilla world generation data (structures, biomes, features, noise...) loaded straight from the
 * game jar. It is loaded once, lazily, on a worker thread; it never touches the connected server.
 */
public final class WorldgenData {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static WorldgenData instance;
	/** Only the registries world generation needs; everything else (enchantments, recipes...) is skipped. */
	private static final Set<RegistryKey<?>> NEEDED = Set.of(
		RegistryKeys.BIOME, RegistryKeys.STRUCTURE, RegistryKeys.STRUCTURE_SET, RegistryKeys.TEMPLATE_POOL, RegistryKeys.PROCESSOR_LIST,
		RegistryKeys.PLACED_FEATURE, RegistryKeys.CONFIGURED_FEATURE, RegistryKeys.CONFIGURED_CARVER, RegistryKeys.NOISE_PARAMETERS,
		RegistryKeys.DENSITY_FUNCTION, RegistryKeys.CHUNK_GENERATOR_SETTINGS, RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST
	);

	public final DynamicRegistryManager.Immutable registries;
	public final StructureTemplateManager templates;

	private WorldgenData(DynamicRegistryManager.Immutable registries, StructureTemplateManager templates) {
		this.registries = registries;
		this.templates = templates;
	}

	public static synchronized WorldgenData get() {
		if (instance == null) {
			try {
				instance = load();
			} catch (Exception e) {
				throw new IllegalStateException("Could not load vanilla world generation data", e);
			}
		}
		return instance;
	}

	public static synchronized boolean isLoaded() {
		return instance != null;
	}

	private static WorldgenData load() throws Exception {
		long start = System.currentTimeMillis();
		LifecycledResourceManager resources = new LifecycledResourceManagerImpl(ResourceType.SERVER_DATA, List.of(VanillaDataPackProvider.createDefaultPack()));
		DynamicRegistryManager.Immutable staticRegistries = DynamicRegistryManager.of(Registries.REGISTRIES);
		// Static registries (blocks, items...) already have their tags bound by the running game.
		List<RegistryWrapper.Impl<?>> wrappers = staticRegistries.streamAllRegistries().<RegistryWrapper.Impl<?>>map(DynamicRegistryManager.Entry::value).toList();
		List<RegistryLoader.Entry<?>> needed = RegistryLoader.DYNAMIC_REGISTRIES.stream().filter(e -> NEEDED.contains(e.key())).toList();
		DynamicRegistryManager.Immutable dynamic = RegistryLoader.loadFromResource(resources, wrappers, needed);
		// Tags of the worldgen registries (e.g. the biome tags structures use).
		for (Registry.PendingTagLoad<?> load : TagGroupLoader.startReload(resources, dynamic)) {
			load.apply();
		}
		StructureTemplateManager templates = createTemplateManager(resources);
		LOGGER.info("[SeedXray] Loaded vanilla worldgen data in {} ms", System.currentTimeMillis() - start);
		return new WorldgenData(dynamic, templates);
	}

	/**
	 * The manager only remembers a path of the session (where generated structures would be saved), so the session
	 * is opened and closed right away and its temporary folder removed.
	 */
	private static StructureTemplateManager createTemplateManager(LifecycledResourceManager resources) throws IOException {
		Path dir = Files.createTempDirectory("seedxray");
		try (LevelStorage.Session session = LevelStorage.create(dir).createSessionWithoutSymlinkCheck("tmp")) {
			return new StructureTemplateManager(resources, session, Schemas.getFixer(), Registries.BLOCK);
		} finally {
			try (var walk = Files.walk(dir)) {
				walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
			}
		}
	}
}
