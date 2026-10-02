package dev.seedxray.gui;

import dev.seedxray.SeedXray;
import dev.seedxray.SeedXrayClient;
import dev.seedxray.seed.SeedState;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * The SeedXray menu: a grey Minecraft-style window with collapsible sub-menus
 * (Structure ESP, Block ESP, Settings, Seed). One sub-menu is open at a time and fills the rest of the window.
 */
public final class MenuScreen extends Screen {
	private static final int HEADER_H = 18;
	private static final int GAP = 2;

	private final List<Section> sections = List.of(new StructuresSection(), new BlocksSection(), new SettingsSection(), new SeedSection());
	private static int open = 0;

	private int px, py, pw, ph;
	private final Btn master = new Btn(() -> "ESP: " + (SeedXray.config.enabled ? "ON" : "OFF"), () -> SeedXray.config.enabled = !SeedXray.config.enabled);

	public MenuScreen() {
		super(Text.literal("SeedXray"));
	}

	@Override
	protected void init() {
		pw = MathHelper.clamp(width - 24, 250, 340);
		ph = MathHelper.clamp(height - 8, 190, 380);
		px = (width - pw) / 2;
		py = (height - ph) / 2;
		master.at(px + pw - 6 - 62, py + 6, 62);
		relayout();
		sections.get(open).opened();
	}

	private void relayout() {
		int y = py + 34;
		int x = px + 6;
		int w = pw - 12;
		int total = py + ph - 8 - y;
		int contentH = total - sections.size() * (HEADER_H + GAP);
		for (int i = 0; i < sections.size(); i++) {
			y += HEADER_H + GAP;
			if (i == open) {
				sections.get(i).layout(x + 2, y + 2, w - 4, contentH - 4);
				y += contentH;
			}
		}
	}

	private int headerY(int index) {
		int y = py + 34;
		int total = py + ph - 8 - y;
		int contentH = total - sections.size() * (HEADER_H + GAP);
		for (int i = 0; i < index; i++) {
			y += HEADER_H + GAP;
			if (i == open) y += contentH;
		}
		return y;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void removed() {
		sections.get(open).closed();
		SeedXray.config.save();
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		// a plain dim instead of the blur: the menu is meant to stay light on weak machines
		context.fill(0, 0, width, height, 0x80000000);
	}

	@Override
	public void render(DrawContext c, int mx, int my, float delta) {
		renderBackground(c, mx, my, delta);
		TextRenderer f = textRenderer;
		Gfx.panel(c, px, py, pw, ph);
		c.drawText(f, "SeedXray", px + 10, py + 9, Gfx.TEXT_DARK, false);
		c.drawText(f, "SeedXray", px + 9, py + 8, 0xFFFFFFFF, true);
		master.render(c, mx, my);
		c.drawText(f, f.trimToWidth(statusLine(), pw - 20), px + 10, py + 22, 0xFF505050, false);

		for (int i = 0; i < sections.size(); i++) {
			Section s = sections.get(i);
			int hy = headerY(i);
			int hx = px + 6, hw = pw - 12;
			boolean hovered = Gfx.inside(mx, my, hx, hy, hw, HEADER_H);
			Gfx.button(c, hx, hy, hw, HEADER_H, hovered, true);
			c.drawText(f, s.title(), hx + 20, hy + 5, 0xFFFFFFFF, true);
			Gfx.chevron(c, hx + 7, hy + 5, i == open, 0xFFFFFFFF);
			String summary = s.summary();
			if (!summary.isEmpty()) c.drawText(f, summary, hx + hw - 8 - f.getWidth(summary), hy + 5, 0xFFC0C0C0, true);
			if (i == open) s.render(c, mx, my, delta);
		}
	}

	private static String statusLine() {
		SeedState s = SeedXray.seed;
		String seed = switch (s.status) {
			case EXACT -> "seed " + s.seed;
			case VERIFIED -> "seed " + s.seed + " (verified)";
			case UNVERIFIED -> "seed " + s.seed + " (unverified)";
			case SEARCHING -> "searching for the seed...";
			case NOT_FOUND -> "seed not found - see the Seed menu";
			default -> "no seed";
		};
		return SeedXray.dim.title + "  |  " + seed;
	}

	private void select(int index) {
		if (index == open) return;
		sections.get(open).closed();
		open = index;
		relayout();
		sections.get(open).opened();
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		double mx = click.x(), my = click.y();
		int button = click.button();
		if (master.click(mx, my)) return true;
		for (int i = 0; i < sections.size(); i++) {
			if (Gfx.inside(mx, my, px + 6, headerY(i), pw - 12, HEADER_H)) {
				MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				select(i);
				return true;
			}
		}
		if (sections.get(open).mouseClicked(mx, my, button, doubled)) return true;
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		return sections.get(open).mouseDragged(click.x(), click.y()) || super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(Click click) {
		return sections.get(open).mouseReleased(click.x(), click.y()) || super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double horizontalAmount, double verticalAmount) {
		return sections.get(open).mouseScrolled(mx, my, verticalAmount) || super.mouseScrolled(mx, my, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.isEscape()) {
			close();
			return true;
		}
		if (sections.get(open).keyPressed(input)) return true;
		if (!sections.get(open).typing() && SeedXrayClient.MENU_KEY.matchesKey(input)) {
			close();
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharInput input) {
		return sections.get(open).charTyped(input) || super.charTyped(input);
	}
}
