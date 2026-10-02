package dev.seedxray.core;

import dev.seedxray.SeedXray;
import dev.seedxray.sim.StructureCatalog;
import dev.seedxray.sim.StructureEntry;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.MathHelper;

/** Default ESP colors (0xRRGGBB); the user can override any of them in the config. */
public final class Colors {
	private Colors() {
	}

	public static int structure(String id) {
		Integer custom = SeedXray.config.colors.get(id);
		if (custom != null) return custom;
		StructureEntry e = StructureCatalog.get(id);
		return e != null ? e.color() : 0xFFFFFF;
	}

	public static int block(Block block) {
		String id = Registries.BLOCK.getId(block).toString();
		Integer custom = SeedXray.config.colors.get(id);
		return custom != null ? custom : defaultBlock(id.substring(id.indexOf(':') + 1));
	}

	public static int defaultBlock(String path) {
		if (path.contains("diamond")) return 0x3CF0E6;
		if (path.contains("emerald")) return 0x17DD62;
		if (path.contains("ancient_debris") || path.contains("netherite")) return 0xD08A64;
		if (path.contains("gold")) return 0xFFD33D;
		if (path.contains("raw_iron") || path.contains("iron")) return 0xE3B79A;
		if (path.contains("copper")) return 0xF08A5D;
		if (path.contains("redstone")) return 0xFF3030;
		if (path.contains("lapis")) return 0x3A5BFF;
		if (path.contains("coal")) return 0x9A9A9A;
		if (path.contains("quartz")) return 0xF3EEDD;
		if (path.contains("amethyst")) return 0xB57BFF;
		if (path.contains("glowstone")) return 0xFFE27A;
		if (path.contains("obsidian")) return 0xA23BFF;
		if (path.contains("spawner")) return 0x20C0FF;
		if (path.contains("chest") || path.contains("barrel")) return 0xFFA52E;
		if (path.contains("lava")) return 0xFF6A00;
		if (path.contains("water")) return 0x3D7BFF;
		if (path.contains("bedrock")) return 0xE0E0E0;
		if (path.contains("tnt")) return 0xFF4040;
		// everything else: a stable, well separated hue from the name
		float hue = (path.hashCode() & 0x7FFFFFFF) % 360 / 360f;
		return hsv(hue, 0.65f, 1f);
	}

	public static int hsv(float h, float s, float v) {
		float r = 0, g = 0, b = 0;
		int i = (int) Math.floor(h * 6);
		float f = h * 6 - i;
		float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
		switch (i % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return (MathHelper.floor(r * 255) << 16) | (MathHelper.floor(g * 255) << 8) | MathHelper.floor(b * 255);
	}
}
