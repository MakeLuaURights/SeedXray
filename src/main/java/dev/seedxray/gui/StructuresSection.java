package dev.seedxray.gui;

import dev.seedxray.SeedXray;
import dev.seedxray.core.Colors;
import dev.seedxray.sim.Dim;
import dev.seedxray.sim.StructureCatalog;
import dev.seedxray.sim.StructureEntry;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/** "Structure ESP": tick the structures to highlight. Only the current dimension's structures are listed. */
final class StructuresSection extends Section {
	private static final int[] PALETTE = {
		0xFF4D4D, 0xFFA033, 0xFFE14D, 0x7CFF4D, 0x2ECC71, 0x3CF0E6, 0x4D8DFF, 0xB36BFF, 0xFF55FF, 0xFFFFFF, 0xA0A0A0, 0xFF9EC4
	};

	private final ScrollView list = new ScrollView(20);
	private final Btn all = new Btn("All", this::selectAll);
	private final Btn none = new Btn("None", this::selectNone);
	private int x, y, w;
	private Dim dim = Dim.OVERWORLD;
	private List<StructureEntry> entries = StructureCatalog.of(Dim.OVERWORLD);

	@Override
	String title() {
		return "Structure ESP";
	}

	@Override
	String summary() {
		refresh();
		long n = entries.stream().filter(e -> SeedXray.config.structures.contains(e.id())).count();
		return n + "/" + entries.size();
	}

	private void refresh() {
		Dim now = SeedXray.dim;
		if (now != dim || entries.isEmpty()) {
			dim = now;
			entries = StructureCatalog.of(now);
			list.reset();
		}
		list.setCount(entries.size());
	}

	@Override
	void layout(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		none.at(x + w - 40, y, 40);
		all.at(x + w - 40 - 4 - 34, y, 34);
		list.layout(x, y + 22, w, Math.max(20, h - 22));
		refresh();
	}

	@Override
	void render(DrawContext c, int mx, int my, float delta) {
		refresh();
		TextRenderer f = Gfx.font();
		c.drawText(f, dim.title + " structures", x + 2, y + 5, Gfx.TEXT_DARK, false);
		all.render(c, mx, my);
		none.render(c, mx, my);
		list.render(c, mx, my, (ctx, i, rx, ry, rw, rh, hovered) -> {
			StructureEntry e = entries.get(i);
			boolean on = SeedXray.config.structures.contains(e.id());
			if (hovered) ctx.fill(rx, ry, rx + rw, ry + rh, 0x30FFFFFF);
			Gfx.checkbox(ctx, rx + 5, ry + 4, on, hovered);
			ctx.drawItem(stack(e), rx + 22, ry + 2);
			String name = f.trimToWidth(e.name(), rw - 68);
			ctx.drawText(f, name, rx + 42, ry + 6, on ? 0xFFFFFFFF : 0xFFB0B0B0, true);
			boolean overSwatch = hovered && mx >= rx + rw - 18;
			Gfx.swatch(ctx, rx + rw - 16, ry + 5, 10, Colors.structure(e.id()), overSwatch);
		});
	}

	private static final java.util.Map<String, ItemStack> STACKS = new java.util.HashMap<>();

	private static ItemStack stack(StructureEntry e) {
		return STACKS.computeIfAbsent(e.id(), k -> new ItemStack(e.icon()));
	}

	@Override
	boolean mouseClicked(double mx, double my, int button, boolean doubled) {
		if (all.click(mx, my) || none.click(mx, my) || list.pressBar(mx, my)) return true;
		int i = list.rowAt(mx, my);
		if (i < 0) return false;
		StructureEntry e = entries.get(i);
		if (mx >= list.x + list.rowWidth() - 18) {
			cycleColor(e.id(), button == 1);
		} else {
			var set = SeedXray.config.structures;
			if (!set.remove(e.id())) set.add(e.id());
			SeedXray.selectionChanged();
		}
		return true;
	}

	private static void cycleColor(String id, boolean reset) {
		var colors = SeedXray.config.colors;
		Integer cur = colors.get(id);
		if (reset) {
			colors.remove(id);
			return;
		}
		int next = -1;
		if (cur == null) {
			next = 0;
		} else {
			for (int i = 0; i < PALETTE.length; i++) {
				if (PALETTE[i] == cur) next = i + 1;
			}
			if (next < 0) next = 0;
		}
		if (next >= PALETTE.length) colors.remove(id);
		else colors.put(id, PALETTE[next]);
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

	private void selectAll() {
		for (StructureEntry e : entries) SeedXray.config.structures.add(e.id());
		SeedXray.selectionChanged();
	}

	private void selectNone() {
		for (StructureEntry e : entries) SeedXray.config.structures.remove(e.id());
		SeedXray.selectionChanged();
	}
}
