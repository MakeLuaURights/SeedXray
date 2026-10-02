package dev.seedxray.gui;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;

/** A plain vanilla-styled button that is placed and drawn by its section. */
final class Btn {
	int x, y, w, h = 18;
	final Supplier<String> label;
	final Runnable action;
	BooleanSupplier active = () -> true;

	Btn(Supplier<String> label, Runnable action) {
		this.label = label;
		this.action = action;
	}

	Btn(String label, Runnable action) {
		this(() -> label, action);
	}

	Btn at(int x, int y, int w) {
		this.x = x;
		this.y = y;
		this.w = w;
		return this;
	}

	void render(DrawContext c, int mx, int my) {
		boolean on = active.getAsBoolean();
		Gfx.buttonLabel(c, x, y, w, h, label.get(), on && Gfx.inside(mx, my, x, y, w, h), on);
	}

	boolean click(double mx, double my) {
		if (active.getAsBoolean() && Gfx.inside(mx, my, x, y, w, h)) {
			action.run();
			net.minecraft.client.MinecraftClient.getInstance().getSoundManager()
				.play(net.minecraft.client.sound.PositionedSoundInstance.ui(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0f));
			return true;
		}
		return false;
	}
}
