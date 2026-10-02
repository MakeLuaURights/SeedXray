package dev.seedxray.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

/** User settings, saved as {@code config/seedxray.json}. Plain public fields on purpose: this is a tiny mod. */
public final class SxConfig {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int STYLE_OUTLINE = 0;

	public static final int STYLE_FILL = 1;
	public static final int STYLE_BOTH = 2;

	// --- master switches
	public boolean enabled = true;
	public boolean structureEsp = true;
	public boolean blockEsp = true;

	// --- selections
	/** Structure ids from {@code StructureCatalog}. */
	public Set<String> structures = new LinkedHashSet<>(Set.of("stronghold", "end_portal", "end_city_elytra"));
	/** Block ids such as {@code minecraft:diamond_ore}. */
	public Set<String> blocks = new LinkedHashSet<>(Set.of(
		"minecraft:diamond_ore", "minecraft:deepslate_diamond_ore", "minecraft:ancient_debris"
	));
	/** Optional per-entry color overrides (0xRRGGBB), keyed by structure id or block id. */
	public Map<String, Integer> colors = new HashMap<>();

	// --- ranges / limits
	/** Structure search radius around the player, in chunks. */
	public int structureRadius = 80;
	/** Radius of seed-predicted ores around the player, in chunks. */
	public int oreRadius = 3;
	/** Maximum highlighted blocks drawn at once (nearest first). */
	public int maxBoxes = 500;

	// --- features
	public boolean predictOres = true;
	public boolean scanLoadedChunks = true;
	/** Selecting an ore also selects its deepslate twin. */
	public boolean linkDeepslate = true;

	// --- look
	public int style = STYLE_BOTH;
	public float lineWidth = 2.0f;
	public int fillAlpha = 60;
	public boolean tracers = true;
	public boolean hud = true;
	public boolean hudSeedLine = true;
	public int hudRows = 5;

	// --- performance
	public boolean lowEnd = false;

	// --- seed finding
	public boolean autoSeedSearch = true;
	/** Collect structures and End pillars while exploring to work the seed out (like SeedcrackerX). */
	public boolean crackFromStructures = true;
	/** The seed search tries every number in [-range, range]. */
	public int seedSearchRange = 30_000_000;
	/** Seeds typed by the user / found earlier, per server address. */
	public Map<String, Long> knownSeeds = new HashMap<>();

	public boolean outlineOnly() {
		return style == STYLE_OUTLINE;
	}

	public boolean drawOutline() {
		return style != STYLE_FILL;
	}

	public boolean drawFill() {
		return style != STYLE_OUTLINE;
	}

	/** Applies settings that keep weak machines smooth. */
	public void applyLowEnd(boolean on) {
		lowEnd = on;
		if (on) {
			oreRadius = Math.min(oreRadius, 2);
			maxBoxes = Math.min(maxBoxes, 250);
			structureRadius = Math.min(structureRadius, 48);
			style = STYLE_OUTLINE;
			tracers = false;
		}
	}

	// ---------------------------------------------------------------- persistence

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("seedxray.json");
	}

	public static SxConfig load() {
		Path file = file();
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				SxConfig loaded = GSON.fromJson(reader, SxConfig.class);
				if (loaded != null) {
					loaded.sanitize();
					return loaded;
				}
			} catch (Exception e) {
				LOGGER.warn("[SeedXray] Could not read {}: {}", file, e.toString());
			}
		}
		return new SxConfig();
	}

	public void save() {
		try {
			Files.createDirectories(file().getParent());
			try (Writer writer = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			LOGGER.warn("[SeedXray] Could not save config: {}", e.toString());
		}
	}

	private void sanitize() {
		if (structures == null) structures = new LinkedHashSet<>();
		if (blocks == null) blocks = new LinkedHashSet<>();
		if (colors == null) colors = new HashMap<>();
		if (knownSeeds == null) knownSeeds = new HashMap<>();
		structureRadius = Math.max(8, Math.min(structureRadius, 200));
		oreRadius = Math.max(1, Math.min(oreRadius, 8));
		maxBoxes = Math.max(50, Math.min(maxBoxes, 3000));
		lineWidth = Math.max(1f, Math.min(lineWidth, 6f));
		fillAlpha = Math.max(10, Math.min(fillAlpha, 200));
		hudRows = Math.max(1, Math.min(hudRows, 10));
		seedSearchRange = Math.max(0, Math.min(seedSearchRange, 100_000_000));
		style = Math.max(0, Math.min(style, 2));
	}
}
