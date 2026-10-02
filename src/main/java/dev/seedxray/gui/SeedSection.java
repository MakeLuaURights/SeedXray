package dev.seedxray.gui;

import dev.seedxray.SeedXray;
import dev.seedxray.crack.CrackController;
import dev.seedxray.seed.SeedFinder;
import dev.seedxray.seed.SeedState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** "Seed": what is known about the seed, and a box to type one in. */
final class SeedSection extends Section {
	private TextFieldWidget input;
	private final Btn apply = new Btn("Apply", this::applyTyped);
	private final Btn copy = new Btn("Copy", () -> MinecraftClient.getInstance().keyboard.setClipboard(Long.toString(SeedXray.seed.seed)));
	private final Btn search = new Btn("Search again", () -> SeedFinder.startSearch(MinecraftClient.getInstance()));
	private final Btn forget = new Btn("Forget saved", () -> {
		SeedXray.config.knownSeeds.remove(SeedFinder.serverKey(MinecraftClient.getInstance()));
		feedback = "Forgot the seed saved for this server.";
	});
	private String feedback = "";
	private int x, y, w, h;

	private TextFieldWidget field() {
		if (input == null) {
			input = new TextFieldWidget(Gfx.font(), 0, 0, 100, 18, Text.literal("Seed"));
			input.setPlaceholder(Text.literal("Type a seed here...").formatted(Formatting.DARK_GRAY));
			input.setMaxLength(64);
		}
		return input;
	}

	@Override
	String title() {
		return "Seed";
	}

	@Override
	String summary() {
		SeedState s = SeedXray.seed;
		return switch (s.status) {
			case EXACT, VERIFIED -> "found";
			case UNVERIFIED -> "unverified";
			case SEARCHING -> "searching";
			case NOT_FOUND -> "not found";
			default -> "";
		};
	}

	@Override
	void layout(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		copy.at(x + w - 44, y + 12, 44);
		TextFieldWidget f = field();
		f.setX(x);
		f.setY(y + 44);
		f.setWidth(w - 54);
		f.setHeight(18);
		apply.at(x + w - 50, y + 44, 50);
		search.at(x, y + 84, 96);
		forget.at(x + 100, y + 84, 90);
	}

	@Override
	void closed() {
		if (input != null) input.setFocused(false);
	}

	@Override
	void render(DrawContext c, int mx, int my, float delta) {
		TextRenderer f = Gfx.font();
		SeedState s = SeedXray.seed;
		String status;
		int color;
		switch (s.status) {
			case EXACT -> { status = "Read from the world (singleplayer)"; color = 0xFF2E8B2E; }
			case VERIFIED -> { status = "Verified: matches the server's hash"; color = 0xFF2E8B2E; }
			case UNVERIFIED -> { status = "Set by you, not verified"; color = 0xFFB07A00; }
			case SEARCHING -> { status = "Searching... " + s.progress; color = 0xFFB07A00; }
			case NOT_FOUND -> { status = "Not found automatically"; color = 0xFFB02020; }
			default -> { status = "Not in a world"; color = Gfx.TEXT_DARK; }
		}
		c.drawText(f, status, x + 2, y, color, false);
		c.drawText(f, "Seed: " + (s.known() ? Long.toString(s.seed) : "?"), x + 2, y + 15, Gfx.TEXT_DARK, false);
		c.drawText(f, "Server hash: " + (s.hasHash ? Long.toHexString(s.hashedSeed) : "-"), x + 2, y + 28, 0xFF707070, false);
		copy.active = () -> s.known();
		copy.render(c, mx, my);
		field().render(c, mx, my, delta);
		apply.render(c, mx, my);
		if (!feedback.isEmpty()) {
			c.drawText(f, f.trimToWidth(feedback, w - 4), x + 2, y + 68, 0xFF404040, false);
		}
		search.active = () -> s.hasHash && MinecraftClient.getInstance().getServer() == null;
		search.render(c, mx, my);
		forget.render(c, mx, my);
		int ty = y + 108;
		String help;
		if (s.hasHash && !s.known() && SeedXray.config.crackFromStructures) {
			var cc = CrackController.INSTANCE;
			help = "Cracker: " + (s.crack.isEmpty() ? cc.summary() + " - " + cc.needLine() : s.crack)
				+ ". Explore: it watches for temples, igloos, huts, monuments, buried treasure, End cities and the End pillars.";
		} else {
			help = "A server only sends a hash of its seed, so it cannot be read directly. "
				+ "The mod tries small numbers and common words; a typed seed is checked against the hash.";
		}
		for (var line : f.wrapLines(Text.literal(help), w - 4)) {
			if (ty > y + h - 8) break;
			c.drawText(f, line, x + 2, ty, 0xFF606060, false);
			ty += 10;
		}
	}

	private void applyTyped() {
		String text = field().getText().trim();
		if (text.isEmpty()) {
			feedback = "Type a seed first.";
			return;
		}
		boolean force = false;
		if (text.endsWith(" force")) {
			force = true;
			text = text.substring(0, text.length() - 6).trim();
		}
		feedback = SeedFinder.setManual(MinecraftClient.getInstance(), text, force);
	}

	@Override
	boolean mouseClicked(double mx, double my, int button, boolean doubled) {
		TextFieldWidget f = field();
		boolean inField = Gfx.inside(mx, my, f.getX(), f.getY(), f.getWidth(), f.getHeight());
		f.setFocused(inField);
		if (inField) {
			f.mouseClicked(new Click(mx, my, new MouseInput(button, 0)), doubled);
			return true;
		}
		return apply.click(mx, my) || copy.click(mx, my) || search.click(mx, my) || forget.click(mx, my);
	}

	@Override
	boolean keyPressed(KeyInput input) {
		TextFieldWidget f = field();
		if (f.isFocused() && input.isEnter()) {
			applyTyped();
			return true;
		}
		return f.isFocused() && f.keyPressed(input);
	}

	@Override
	boolean charTyped(CharInput input) {
		TextFieldWidget f = field();
		return f.isFocused() && f.charTyped(input);
	}

	@Override
	boolean typing() {
		return input != null && input.isFocused();
	}
}
