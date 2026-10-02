package dev.seedxray.render;

import dev.seedxray.SeedXray;
import dev.seedxray.config.SxConfig;
import dev.seedxray.core.Colors;
import dev.seedxray.core.Marker;
import dev.seedxray.core.StructureSnapshot;
import dev.seedxray.seed.SeedState;
import dev.seedxray.sim.Dim;
import dev.seedxray.sim.StructureCatalog;
import dev.seedxray.sim.StructureEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** The always-on overlay: seed status and an arrow + distance to the nearest structure of each selected kind. */
public final class HudRenderer {
	private static final Map<String, ItemStack> ICONS = new HashMap<>();

	private HudRenderer() {
	}

	public static void register() {
		HudElementRegistry.addLast(Identifier.of(SeedXray.ID, "overlay"), HudRenderer::render);
	}

	private record Row(StructureEntry entry, Marker marker, double distance) {
	}

	private static void render(DrawContext context, RenderTickCounter tickCounter) {
		SxConfig cfg = SeedXray.config;
		MinecraftClient client = MinecraftClient.getInstance();
		if (!cfg.enabled || !cfg.hud || client.player == null || client.world == null || client.options.hudHidden) return;
		TextRenderer font = client.textRenderer;
		int x = 6;
		int y = 6;

		if (cfg.hudSeedLine) {
			String line = seedLine();
			int w = font.getWidth(line);
			context.fill(x - 3, y - 3, x + w + 3, y + 11, 0x80000000);
			context.drawText(font, line, x, y, 0xFFFFFFFF, true);
			y += 16;
			String status = SeedXray.predictor.status();
			if (!status.isEmpty()) {
				context.drawText(font, status, x, y, 0xFFAAAAAA, true);
				y += 12;
			}
		}

		StructureSnapshot snapshot = SeedXray.predictor.structures();
		if (!cfg.structureEsp || !SeedXray.seed.known() || snapshot.seed() != SeedXray.seed.seed
			|| snapshot.dim() != Dim.of(client.world.getRegistryKey())) return;

		double px = client.player.getX(), py = client.player.getY(), pz = client.player.getZ();
		Map<String, Row> nearest = new HashMap<>();
		for (Marker m : snapshot.markers()) {
			if (!cfg.structures.contains(m.id())) continue;
			StructureEntry entry = StructureCatalog.get(m.id());
			if (entry == null) continue;
			double d = Math.sqrt(sq(m.pos().getX() + 0.5 - px) + sq(m.pos().getZ() + 0.5 - pz));
			Row best = nearest.get(m.id());
			if (best == null || d < best.distance) nearest.put(m.id(), new Row(entry, m, d));
		}
		if (nearest.isEmpty()) return;
		List<Row> rows = new ArrayList<>(nearest.values());
		rows.sort(Comparator.comparingDouble(r -> r.distance));
		float tickProgress = tickCounter.getTickProgress(false);
		float yaw = client.player.getYaw(tickProgress);

		int shown = 0;
		for (Row row : rows) {
			if (shown++ >= cfg.hudRows) break;
			String label = row.entry.name() + "  " + (int) row.distance + "m";
			if (row.entry.id().equals(StructureCatalog.END_PORTAL) || row.entry.id().equals(StructureCatalog.END_SHIP_ELYTRA)) {
				label += "  Y " + row.marker.pos().getY();
			}
			int w = font.getWidth(label);
			int rowW = 22 + 18 + w;
			context.fill(x - 3, y - 2, x + rowW + 3, y + 18, 0x80000000);
			double dx = row.marker.pos().getX() + 0.5 - px, dz = row.marker.pos().getZ() + 0.5 - pz;
			float rel = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)) - yaw);
			arrow(context, x + 8, y + 8, rel, 0xFF000000 | Colors.structure(row.entry.id()));
			context.drawItem(icon(row.entry), x + 20, y);
			context.drawText(font, label, x + 40, y + 5, 0xFFFFFFFF, true);
			y += 22;
		}
	}

	private static double sq(double v) {
		return v * v;
	}

	private static ItemStack icon(StructureEntry entry) {
		return ICONS.computeIfAbsent(entry.id(), k -> new ItemStack(entry.icon()));
	}

	/** An arrow pointing {@code angle} degrees clockwise from "up" (up = straight ahead). */
	private static void arrow(DrawContext context, int cx, int cy, float angle, int color) {
		var m = context.getMatrices();
		m.pushMatrix();
		m.translate(cx, cy);
		m.rotate((float) Math.toRadians(angle));
		context.fill(-1, -2, 2, 7, 0xFF000000); // outline
		context.fill(0, -1, 1, 6, color);
		for (int i = 0; i < 6; i++) {
			context.fill(-i - 1, -7 + i, i + 2, -6 + i, 0xFF000000);
		}
		for (int i = 0; i < 5; i++) {
			context.fill(-i, -6 + i, i + 1, -5 + i, color);
		}
		m.popMatrix();
	}

	private static String seedLine() {
		SeedState s = SeedXray.seed;
		return switch (s.status) {
			case EXACT -> "Seed " + s.seed;
			case VERIFIED -> "Seed " + s.seed + " (verified)";
			case UNVERIFIED -> "Seed " + s.seed + " (unverified)";
			case SEARCHING -> "Seed: searching... " + s.progress;
			case NOT_FOUND -> "Seed: not found - type /seedxray seed <seed>";
			default -> "Seed: unknown";
		};
	}
}
