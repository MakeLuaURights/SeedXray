package dev.seedxray.gui;

import dev.seedxray.SeedXray;
import dev.seedxray.core.Colors;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** "Block ESP": a searchable, scrollable list of every block. */
final class BlocksSection extends Section {
	private static final int[] PALETTE = {
		0xFF4D4D, 0xFFA033, 0xFFE14D, 0x7CFF4D, 0x2ECC71, 0x3CF0E6, 0x4D8DFF, 0xB36BFF, 0xFF55FF, 0xFFFFFF, 0xA0A0A0, 0xFF9EC4
	};

	private boolean onlySelected;
	private final ScrollView list = new ScrollView(20);
	private TextFieldWidget search;
	private final Btn selectedOnly = new Btn(() -> "Selected: " + (onlySelected ? "ON" : "OFF"), () -> {
		onlySelected = !onlySelected;
		refilter();
	});
	private final Btn clear = new Btn("Clear", () -> {
		SeedXray.config.blocks.clear();
		SeedXray.selectionChanged();
		refilter();
	});
	private List<BlockEntry> shown = List.of();
	private String lastQuery = null;
	private int x, y, w;

	private TextFieldWidget field() {
		if (search == null) {
			search = new TextFieldWidget(Gfx.font(), 0, 0, 100, 18, Text.literal("Search"));
			search.setPlaceholder(Text.literal("Search blocks...").formatted(net.minecraft.util.Formatting.DARK_GRAY));
			search.setMaxLength(48);
			search.setChangedListener(q -> refilter());
		}
		return search;
	}

	@Override
	String title() {
		return "Block ESP";
	}

	@Override
	String summary() {
		return SeedXray.config.blocks.size() + " selected";
	}

	private void refilter() {
		String q = search == null ? "" : search.getText();
		lastQuery = q;
		List<BlockEntry> found = BlockCatalog.search(q);
		if (onlySelected) {
			found = found.stream().filter(e -> SeedXray.config.blocks.contains(e.id)).toList();
		}
		shown = found;
		list.setCount(shown.size());
		list.reset();
	}

	@Override
	void layout(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		TextFieldWidget f = field();
		int buttons = 74 + 4 + 40;
		f.setX(x);
		f.setY(y);
		f.setWidth(w - buttons - 4);
		f.setHeight(18);
		selectedOnly.at(x + w - buttons, y, 74);
		clear.at(x + w - 40, y, 40);
		list.layout(x, y + 22, w, Math.max(20, h - 22));
		if (lastQuery == null) refilter();
	}

	@Override
	void opened() {
		field().setFocused(true);
		if (lastQuery == null) refilter();
	}

	@Override
	void closed() {
		if (search != null) search.setFocused(false);
	}

	@Override
	void render(DrawContext c, int mx, int my, float delta) {
		TextRenderer f = Gfx.font();
		field().render(c, mx, my, delta);
		selectedOnly.render(c, mx, my);
		clear.render(c, mx, my);
		list.render(c, mx, my, (ctx, i, rx, ry, rw, rh, hovered) -> {
			BlockEntry e = shown.get(i);
			boolean on = SeedXray.config.blocks.contains(e.id);
			if (hovered) ctx.fill(rx, ry, rx + rw, ry + rh, 0x30FFFFFF);
			Gfx.checkbox(ctx, rx + 5, ry + 4, on, hovered);
			ctx.drawItem(e.icon, rx + 22, ry + 2);
			int nameW = rw - 68 - (e.predicted ? 12 : 0);
			ctx.drawText(f, f.trimToWidth(e.name, nameW), rx + 42, ry + 6, on ? 0xFFFFFFFF : 0xFFB0B0B0, true);
			if (e.predicted) {
				// the seed can predict this block: it is found even where it is not loaded or hidden by anti-xray
				ctx.drawText(f, "S", rx + rw - 28, ry + 6, Gfx.GOLD, true);
			}
			Gfx.swatch(ctx, rx + rw - 16, ry + 5, 10, Colors.block(e.block), hovered && mx >= rx + rw - 18);
		});
		if (shown.isEmpty()) {
			c.drawText(f, onlySelected ? "Nothing selected yet" : "No block matches", list.x + 8, list.y + 8, Gfx.GRAY, false);
		}
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
		if (selectedOnly.click(mx, my) || clear.click(mx, my) || list.pressBar(mx, my)) return true;
		int i = list.rowAt(mx, my);
		if (i < 0) return false;
		BlockEntry e = shown.get(i);
		if (mx >= list.x + list.rowWidth() - 18) {
			cycleColor(e, button == 1);
		} else {
			toggle(e);
		}
		return true;
	}

	private void toggle(BlockEntry e) {
		var set = SeedXray.config.blocks;
		boolean now = !set.contains(e.id);
		apply(e.id, now);
		if (SeedXray.config.linkDeepslate) {
			String path = e.id.substring(e.id.indexOf(':') + 1);
			String twin = path.startsWith("deepslate_") ? path.substring(10) : "deepslate_" + path;
			Identifier id = Identifier.of("minecraft", twin);
			if (Registries.BLOCK.containsId(id)) apply(id.toString(), now);
		}
		SeedXray.selectionChanged();
		if (onlySelected) refilter();
	}

	private static void apply(String id, boolean on) {
		if (on) SeedXray.config.blocks.add(id);
		else SeedXray.config.blocks.remove(id);
	}

	private void cycleColor(BlockEntry e, boolean reset) {
		var colors = SeedXray.config.colors;
		Integer cur = colors.get(e.id);
		if (reset) {
			colors.remove(e.id);
		} else {
			int next = 0;
			if (cur != null) {
				next = -1;
				for (int i = 0; i < PALETTE.length; i++) if (PALETTE[i] == cur) next = i + 1;
				if (next < 0) next = 0;
			}
			if (next >= PALETTE.length) colors.remove(e.id);
			else colors.put(e.id, PALETTE[next]);
		}
		SeedXray.selectionChanged(); // colors are baked into the selection
	}

	@Override
	boolean mouseDragged(double mx, double my) {
		return list.drag(my);
	}

	@Override
	boolean mouseReleased(double mx, double my) {
		return list.release();
	}

	@Override
	boolean mouseScrolled(double mx, double my, double amount) {
		return list.scrolled(mx, my, amount);
	}

	@Override
	boolean keyPressed(KeyInput input) {
		TextFieldWidget f = field();
		return f.isFocused() && f.keyPressed(input);
	}

	@Override
	boolean charTyped(CharInput input) {
		TextFieldWidget f = field();
		return f.isFocused() && f.charTyped(input);
	}

	@Override
	boolean typing() {
		return search != null && search.isFocused();
	}
}
