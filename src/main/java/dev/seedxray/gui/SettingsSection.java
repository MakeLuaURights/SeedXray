package dev.seedxray.gui;

import dev.seedxray.SeedXray;
import dev.seedxray.config.SxConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;

/** "Settings": switches and sliders. */
final class SettingsSection extends Section {
	private abstract static class Row {
		abstract void render(DrawContext c, int x, int y, int w, boolean hovered);

		/** @return true if the row wants to keep receiving drag events */
		boolean press(double mx, int x, int w) {
			return false;
		}

		void drag(double mx, int x, int w) {
		}
	}

	private static final class Toggle extends Row {
		final String label;
		final BooleanSupplier get;
		final Consumer<Boolean> set;

		Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
			this.label = label;
			this.get = get;
			this.set = set;
		}

		@Override
		void render(DrawContext c, int x, int y, int w, boolean hovered) {
			Gfx.button(c, x + 4, y + 1, w - 8, 18, hovered, true);
			TextRenderer f = Gfx.font();
			c.drawText(f, label, x + 12, y + 6, 0xFFFFFFFF, true);
			boolean on = get.getAsBoolean();
			String v = on ? "ON" : "OFF";
			c.drawText(f, v, x + w - 12 - f.getWidth(v), y + 6, on ? Gfx.GREEN : Gfx.RED, true);
		}

		@Override
		boolean press(double mx, int x, int w) {
			set.accept(!get.getAsBoolean());
			click();
			return false;
		}
	}

	private static final class Cycle extends Row {
		final String label;
		final Supplier<String> value;
		final Runnable next;

		Cycle(String label, Supplier<String> value, Runnable next) {
			this.label = label;
			this.value = value;
			this.next = next;
		}

		@Override
		void render(DrawContext c, int x, int y, int w, boolean hovered) {
			Gfx.button(c, x + 4, y + 1, w - 8, 18, hovered, true);
			TextRenderer f = Gfx.font();
			c.drawText(f, label, x + 12, y + 6, 0xFFFFFFFF, true);
			String v = value.get();
			c.drawText(f, v, x + w - 12 - f.getWidth(v), y + 6, Gfx.GOLD, true);
		}

		@Override
		boolean press(double mx, int x, int w) {
			next.run();
			click();
			return false;
		}
	}

	private static final class Slider extends Row {
		final String label;
		final int min, max, step;
		final IntSupplier get;
		final IntConsumer set;

		Slider(String label, int min, int max, int step, IntSupplier get, IntConsumer set) {
			this.label = label;
			this.min = min;
			this.max = max;
			this.step = step;
			this.get = get;
			this.set = set;
		}

		@Override
		void render(DrawContext c, int x, int y, int w, boolean hovered) {
			Gfx.button(c, x + 4, y + 1, w - 8, 18, hovered, true);
			float t = (get.getAsInt() - min) / (float) (max - min);
			int fillW = (int) ((w - 12) * MathHelper.clamp(t, 0, 1));
			c.fill(x + 6, y + 3, x + 6 + fillW, y + 17, 0x5055FF55);
			c.fill(x + 6 + fillW - 1, y + 3, x + 6 + fillW + 1, y + 17, 0xFFFFFFFF);
			TextRenderer f = Gfx.font();
			c.drawText(f, label, x + 12, y + 6, 0xFFFFFFFF, true);
			String v = Integer.toString(get.getAsInt());
			c.drawText(f, v, x + w - 12 - f.getWidth(v), y + 6, Gfx.GOLD, true);
		}

		@Override
		boolean press(double mx, int x, int w) {
			drag(mx, x, w);
			return true;
		}

		@Override
		void drag(double mx, int x, int w) {
			double t = MathHelper.clamp((mx - (x + 6)) / (w - 12), 0, 1);
			int v = min + (int) Math.round(t * (max - min) / step) * step;
			set.accept(MathHelper.clamp(v, min, max));
		}
	}

	private static void click() {
		MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	private final ScrollView list = new ScrollView(20);
	private final List<Row> rows = new ArrayList<>();
	private Row dragging;

	SettingsSection() {
		SxConfig c = SeedXray.config;
		rows.add(new Toggle("ESP enabled", () -> c.enabled, v -> c.enabled = v));
		rows.add(new Toggle("Structure ESP", () -> c.structureEsp, v -> c.structureEsp = v));
		rows.add(new Toggle("Block ESP", () -> c.blockEsp, v -> {
			c.blockEsp = v;
			SeedXray.selectionChanged();
		}));
		rows.add(new Cycle("Highlight style", () -> switch (c.style) {
			case 0 -> "Outline";
			case 1 -> "Filled";
			default -> "Outline + filled";
		}, () -> c.style = (c.style + 1) % 3));
		rows.add(new Slider("Line width", 1, 5, 1, () -> Math.round(c.lineWidth), v -> c.lineWidth = v));
		rows.add(new Slider("Fill opacity", 10, 160, 10, () -> c.fillAlpha, v -> c.fillAlpha = v));
		rows.add(new Toggle("Tracer lines to structures", () -> c.tracers, v -> c.tracers = v));
		rows.add(new Toggle("HUD: arrows and distances", () -> c.hud, v -> c.hud = v));
		rows.add(new Toggle("HUD: seed line", () -> c.hudSeedLine, v -> c.hudSeedLine = v));
		rows.add(new Slider("HUD rows", 1, 8, 1, () -> c.hudRows, v -> c.hudRows = v));
		rows.add(new Slider("Structure search radius (chunks)", 16, 160, 8, () -> c.structureRadius, v -> c.structureRadius = v));
		rows.add(new Slider("Ore prediction radius (chunks)", 1, 8, 1, () -> c.oreRadius, v -> c.oreRadius = v));
		rows.add(new Slider("Max highlighted blocks", 100, 2000, 50, () -> c.maxBoxes, v -> c.maxBoxes = v));
		rows.add(new Toggle("Predict ores from the seed", () -> c.predictOres, v -> {
			c.predictOres = v;
			SeedXray.selectionChanged();
		}));
		rows.add(new Toggle("Scan loaded chunks", () -> c.scanLoadedChunks, v -> {
			c.scanLoadedChunks = v;
			SeedXray.selectionChanged();
		}));
		rows.add(new Toggle("Select deepslate twins too", () -> c.linkDeepslate, v -> c.linkDeepslate = v));
		rows.add(new Toggle("Low-end mode (lighter)", () -> c.lowEnd, v -> {
			c.applyLowEnd(v);
			c.lowEnd = v;
			SeedXray.selectionChanged();
		}));
		rows.add(new Cycle("Reset all colors", () -> c.colors.isEmpty() ? "default" : c.colors.size() + " custom", () -> {
			c.colors.clear();
			SeedXray.selectionChanged();
		}));
		list.setCount(rows.size());
	}

	@Override
	String title() {
		return "Settings";
	}

	@Override
	void layout(int x, int y, int w, int h) {
		list.layout(x, y, w, h);
	}

	@Override
	void render(DrawContext c, int mx, int my, float delta) {
		list.render(c, mx, my, (ctx, i, rx, ry, rw, rh, hovered) -> rows.get(i).render(ctx, rx, ry, rw, hovered));
	}

	@Override
	boolean mouseClicked(double mx, double my, int button, boolean doubled) {
		if (list.pressBar(mx, my)) return true;
		int i = list.rowAt(mx, my);
		if (i < 0) return false;
		Row row = rows.get(i);
		if (row.press(mx, list.x, list.rowWidth())) dragging = row;
		return true;
	}

	@Override
	boolean mouseDragged(double mx, double my) {
		if (dragging != null) {
			dragging.drag(mx, list.x, list.rowWidth());
			return true;
		}
		return list.drag(my);
	}

	@Override
	boolean mouseReleased(double mx, double my) {
		boolean was = dragging != null;
		dragging = null;
		return list.release() || was;
	}

	@Override
	boolean mouseScrolled(double mx, double my, double amount) {
		return list.scrolled(mx, my, amount);
	}
}
