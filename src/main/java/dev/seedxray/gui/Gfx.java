package dev.seedxray.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Small drawing helpers in the classic Minecraft look (grey bevelled panels, vanilla button sprites). */
final class Gfx {
	static final int PANEL = 0xFFC6C6C6;
	static final int WHITE = 0xFFFFFFFF;
	static final int SHADOW = 0xFF555555;
	static final int BLACK = 0xFF000000;
	static final int TEXT_DARK = 0xFF404040;
	static final int LIST_BG = 0xFF262626;
	static final int LIST_ALT = 0xFF2E2E2E;
	static final int GREEN = 0xFF55FF55;
	static final int RED = 0xFFFF5555;
	static final int GOLD = 0xFFFFAA00;
	static final int GRAY = 0xFFAAAAAA;

	private static final Identifier BUTTON = Identifier.ofVanilla("widget/button");
	private static final Identifier BUTTON_HOVER = Identifier.ofVanilla("widget/button_highlighted");
	private static final Identifier BUTTON_OFF = Identifier.ofVanilla("widget/button_disabled");

	private Gfx() {
	}

	static TextRenderer font() {
		return MinecraftClient.getInstance().textRenderer;
	}

	/** The raised grey window of the inventory screens. */
	static void panel(DrawContext c, int x, int y, int w, int h) {
		c.fill(x, y, x + w, y + h, BLACK);
		c.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
		c.fill(x + 1, y + 1, x + w - 2, y + 3, WHITE);
		c.fill(x + 1, y + 1, x + 3, y + h - 2, WHITE);
		c.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, SHADOW);
		c.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, SHADOW);
	}

	/** A dark sunken area for lists. */
	static void inset(DrawContext c, int x, int y, int w, int h) {
		c.fill(x - 1, y - 1, x + w + 1, y + h + 1, SHADOW);
		c.fill(x - 1, y + h, x + w + 1, y + h + 1, WHITE);
		c.fill(x + w, y - 1, x + w + 1, y + h + 1, WHITE);
		c.fill(x, y, x + w, y + h, LIST_BG);
	}

	static void button(DrawContext c, int x, int y, int w, int h, boolean hovered, boolean active) {
		Identifier sprite = !active ? BUTTON_OFF : hovered ? BUTTON_HOVER : BUTTON;
		c.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h);
	}

	static void buttonLabel(DrawContext c, int x, int y, int w, int h, String label, boolean hovered, boolean active) {
		button(c, x, y, w, h, hovered, active);
		TextRenderer f = font();
		String text = f.trimToWidth(label, w - 6);
		c.drawText(f, text, x + (w - f.getWidth(text)) / 2, y + (h - 8) / 2, active ? 0xFFFFFFFF : 0xFFA0A0A0, true);
	}

	static void checkbox(DrawContext c, int x, int y, boolean checked, boolean hovered) {
		c.fill(x, y, x + 12, y + 12, BLACK);
		c.fill(x + 1, y + 1, x + 11, y + 11, hovered ? 0xFF6A6A6A : 0xFF4A4A4A);
		c.fill(x + 1, y + 1, x + 11, y + 2, 0xFF2A2A2A);
		c.fill(x + 1, y + 1, x + 2, y + 11, 0xFF2A2A2A);
		if (checked) {
			int[][] px = {{2, 6}, {3, 7}, {4, 8}, {5, 7}, {6, 6}, {7, 5}, {8, 4}, {9, 3}};
			for (int[] p : px) {
				c.fill(x + p[0], y + p[1] - 1, x + p[0] + 1, y + p[1] + 2, GREEN);
			}
		}
	}

	static void swatch(DrawContext c, int x, int y, int size, int rgb, boolean hovered) {
		c.fill(x - 1, y - 1, x + size + 1, y + size + 1, hovered ? WHITE : BLACK);
		c.fill(x, y, x + size, y + size, 0xFF000000 | rgb);
	}

	/** A small solid triangle: pointing down when {@code open}, right when closed. */
	static void chevron(DrawContext c, int x, int y, boolean open, int color) {
		if (open) {
			for (int i = 0; i < 4; i++) c.fill(x + i, y + i, x + 8 - i, y + i + 1, color);
		} else {
			for (int i = 0; i < 4; i++) c.fill(x + i, y + i, x + i + 1, y + 8 - i, color);
		}
	}

	static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}
}
