package dev.seedxray.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.seedxray.SeedXray;
import dev.seedxray.config.SxConfig;
import dev.seedxray.core.BoxList;
import dev.seedxray.core.Colors;
import dev.seedxray.core.Marker;
import dev.seedxray.core.StructureSnapshot;
import dev.seedxray.sim.Dim;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/**
 * Draws the highlights through walls: outlines and translucent faces for blocks and structures, plus tracer lines.
 * Everything goes into two vertex buffers per frame, so the cost is a few thousand vertices at most.
 */
public final class EspRenderer {
	private static final RenderPipeline LINES_PIPELINE = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
			.withLocation(Identifier.of(SeedXray.ID, "pipeline/esp_lines"))
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withDepthWrite(false)
			.build()
	);
	private static final RenderPipeline FILL_PIPELINE = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
			.withLocation(Identifier.of(SeedXray.ID, "pipeline/esp_fill"))
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withDepthWrite(false)
			.withCull(false)
			.build()
	);
	private static final RenderLayer LINES = RenderLayer.of("seedxray_lines", RenderSetup.builder(LINES_PIPELINE).translucent().build());
	private static final RenderLayer FILL = RenderLayer.of("seedxray_fill", RenderSetup.builder(FILL_PIPELINE).translucent().build());

	private EspRenderer() {
	}

	public static void register() {
		WorldRenderEvents.END_MAIN.register(EspRenderer::render);
	}

	private static void render(WorldRenderContext context) {
		SxConfig cfg = SeedXray.config;
		MinecraftClient client = MinecraftClient.getInstance();
		if (!cfg.enabled || client.player == null || client.world == null) return;

		BoxList boxes = cfg.blockEsp ? Esp.controller.boxes() : BoxList.EMPTY;
		StructureSnapshot snapshot = SeedXray.predictor.structures();
		boolean structures = cfg.structureEsp && SeedXray.seed.known() && snapshot.seed() == SeedXray.seed.seed
			&& snapshot.dim() == Dim.of(client.world.getRegistryKey());
		if (boxes.count == 0 && !structures) return;

		Vec3d cam = context.worldState().cameraRenderState.pos;
		MatrixStack.Entry entry = context.matrices().peek();
		VertexConsumerProvider consumers = context.consumers();
		Vector3f forward = context.worldState().cameraRenderState.orientation.transform(new Vector3f(0, 0, -1));

		// Fog would fade distant highlights into the scenery, so the layers are flushed right away with fog switched off.
		// (The provider keeps one open buffer at a time, so each layer is written in one go: faces first, lines on top.)
		VertexConsumerProvider.Immediate immediate = consumers instanceof VertexConsumerProvider.Immediate i ? i : null;
		GpuBufferSlice previousFog = RenderSystem.getShaderFog();
		if (immediate != null) {
			RenderSystem.setShaderFog(client.gameRenderer.fogRenderer.getFogBuffer(FogRenderer.FogType.NONE));
		}
		try {
			if (cfg.drawFill()) {
				VertexConsumer fill = consumers.getBuffer(FILL);
				draw(fill, true, entry, cam, forward, boxes, structures ? snapshot : null, cfg);
				if (immediate != null) immediate.draw(FILL);
			}
			VertexConsumer lines = consumers.getBuffer(LINES);
			draw(lines, false, entry, cam, forward, boxes, structures ? snapshot : null, cfg);
			if (immediate != null) immediate.draw(LINES);
		} finally {
			if (immediate != null) RenderSystem.setShaderFog(previousFog);
		}
	}

	/** One pass over everything to draw: either only the translucent faces or only the lines. */
	private static void draw(VertexConsumer c, boolean faces, MatrixStack.Entry entry, Vec3d cam, Vector3f forward, BoxList boxes, StructureSnapshot snapshot, SxConfig cfg) {
		float width = cfg.lineWidth;
		int fillAlpha = cfg.fillAlpha;

		// ---- blocks
		for (int i = 0; i < boxes.count; i++) {
			int rgb = boxes.rgb[i];
			float x = (float) (boxes.x[i] - cam.x), y = (float) (boxes.y[i] - cam.y), z = (float) (boxes.z[i] - cam.z);
			if (faces) {
				fillBox(c, entry, x, y, z, x + 1, y + 1, z + 1, (fillAlpha << 24) | rgb);
			} else if (cfg.drawOutline()) {
				float e = 0.002f; // keep the outline just outside the block
				box(c, entry, x - e, y - e, z - e, x + 1 + e, y + 1 + e, z + 1 + e, 0xFF000000 | rgb, width);
			}
		}
		if (snapshot == null) return;

		// ---- structures
		Map<String, Marker> nearest = new HashMap<>();
		for (Marker m : snapshot.markers()) {
			if (!cfg.structures.contains(m.id())) continue;
			int rgb = Colors.structure(m.id());
			BlockBox b = m.box();
			double dx = m.pos().getX() + 0.5 - cam.x, dz = m.pos().getZ() + 0.5 - cam.z;
			double dist2 = dx * dx + dz * dz;
			Marker best = nearest.get(m.id());
			if (best == null || dist2 < sq(best.pos().getX() + 0.5 - cam.x) + sq(best.pos().getZ() + 0.5 - cam.z)) nearest.put(m.id(), m);
			if (dist2 > 400.0 * 400.0) continue; // beyond the far plane; the HUD and tracer still point at it
			if (b == null) b = new BlockBox(m.pos());
			float x0 = (float) (b.getMinX() - cam.x), y0 = (float) (b.getMinY() - cam.y), z0 = (float) (b.getMinZ() - cam.z);
			float x1 = (float) (b.getMaxX() + 1 - cam.x), y1 = (float) (b.getMaxY() + 1 - cam.y), z1 = (float) (b.getMaxZ() + 1 - cam.z);
			if (faces) {
				fillBox(c, entry, x0, y0, z0, x1, y1, z1, (Math.max(20, fillAlpha / 2) << 24) | rgb);
			} else {
				box(c, entry, x0, y0, z0, x1, y1, z1, 0xFF000000 | rgb, width + 1f);
				// a tall beam so it can be spotted from afar
				float cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
				line(c, entry, cx, y0, cz, cx, y1 + 64, cz, 0xFF000000 | rgb, width);
			}
		}
		if (!faces && cfg.tracers) {
			float sx = forward.x * 1.6f, sy = forward.y * 1.6f - 0.15f, sz = forward.z * 1.6f;
			for (Marker m : nearest.values()) {
				float tx = (float) (m.pos().getX() + 0.5 - cam.x), ty = (float) (m.pos().getY() + 0.5 - cam.y), tz = (float) (m.pos().getZ() + 0.5 - cam.z);
				line(c, entry, sx, sy, sz, tx, ty, tz, 0xCC000000 | Colors.structure(m.id()), Math.max(1f, width - 0.5f));
			}
		}
	}

	private static double sq(double v) {
		return v * v;
	}

	private static void line(VertexConsumer c, MatrixStack.Entry m, float x1, float y1, float z1, float x2, float y2, float z2, int argb, float width) {
		float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
		float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1e-4f) return;
		dx /= len;
		dy /= len;
		dz /= len;
		c.vertex(m, x1, y1, z1).color(argb).normal(m, dx, dy, dz).lineWidth(width);
		c.vertex(m, x2, y2, z2).color(argb).normal(m, dx, dy, dz).lineWidth(width);
	}

	private static void box(VertexConsumer c, MatrixStack.Entry m, float x0, float y0, float z0, float x1, float y1, float z1, int argb, float w) {
		line(c, m, x0, y0, z0, x1, y0, z0, argb, w);
		line(c, m, x0, y0, z1, x1, y0, z1, argb, w);
		line(c, m, x0, y1, z0, x1, y1, z0, argb, w);
		line(c, m, x0, y1, z1, x1, y1, z1, argb, w);
		line(c, m, x0, y0, z0, x0, y1, z0, argb, w);
		line(c, m, x1, y0, z0, x1, y1, z0, argb, w);
		line(c, m, x0, y0, z1, x0, y1, z1, argb, w);
		line(c, m, x1, y0, z1, x1, y1, z1, argb, w);
		line(c, m, x0, y0, z0, x0, y0, z1, argb, w);
		line(c, m, x1, y0, z0, x1, y0, z1, argb, w);
		line(c, m, x0, y1, z0, x0, y1, z1, argb, w);
		line(c, m, x1, y1, z0, x1, y1, z1, argb, w);
	}

	private static void quad(VertexConsumer c, MatrixStack.Entry m, int argb, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz) {
		c.vertex(m, ax, ay, az).color(argb);
		c.vertex(m, bx, by, bz).color(argb);
		c.vertex(m, cx, cy, cz).color(argb);
		c.vertex(m, dx, dy, dz).color(argb);
	}

	private static void fillBox(VertexConsumer c, MatrixStack.Entry m, float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
		quad(c, m, argb, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
		quad(c, m, argb, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
		quad(c, m, argb, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
		quad(c, m, argb, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		quad(c, m, argb, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
		quad(c, m, argb, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
	}
}
