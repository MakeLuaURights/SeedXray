package dev.seedxray.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

/** One collapsible block of the menu. */
abstract class Section {
	abstract String title();

	/** Short text shown right-aligned on the header, e.g. "3/12". */
	String summary() {
		return "";
	}

	abstract void layout(int x, int y, int w, int h);

	abstract void render(DrawContext c, int mx, int my, float delta);

	/** True while a text box has the keyboard. */
	boolean typing() {
		return false;
	}

	void opened() {
	}

	void closed() {
	}

	boolean mouseClicked(double mx, double my, int button, boolean doubled) {
		return false;
	}

	boolean mouseDragged(double mx, double my) {
		return false;
	}

	boolean mouseReleased(double mx, double my) {
		return false;
	}

	boolean mouseScrolled(double mx, double my, double amount) {
		return false;
	}

	boolean keyPressed(KeyInput input) {
		return false;
	}

	boolean charTyped(CharInput input) {
		return false;
	}
}
