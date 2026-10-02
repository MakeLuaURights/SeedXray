package dev.seedxray.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/** A vertically scrolling list of equally tall rows. Only visible rows are drawn, so thousands of rows cost nothing. */
final class ScrollView {
	interface RowPainter {
		void paint(DrawContext c, int index, int x, int y, int w, int h, boolean hovered);
	}

	int x, y, w, h;
	final int rowHeight;
	int count;
	private double scroll;
	private boolean dragging;

	ScrollView(int rowHeight) {
		this.rowHeight = rowHeight;
	}

	void layout(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		clamp();
	}

	void setCount(int count) {
		this.count = count;
		clamp();
	}

	void reset() {
		scroll = 0;
	}

	private int maxScroll() {
		return Math.max(0, count * rowHeight - h);
	}

	private void clamp() {
		scroll = MathHelper.clamp(scroll, 0, maxScroll());
	}

	private boolean hasBar() {
		return maxScroll() > 0;
	}

	/** Width available to rows. */
	int rowWidth() {
		return w - (hasBar() ? 8 : 0);
	}

	int rowAt(double mx, double my) {
		if (!Gfx.inside(mx, my, x, y, rowWidth(), h)) return -1;
		int i = (int) ((my - y + scroll) / rowHeight);
		return i >= 0 && i < count ? i : -1;
	}

	/** Left edge of row content (x of the list). */
	void render(DrawContext c, int mx, int my, RowPainter painter) {
		Gfx.inset(c, x, y, w, h);
		c.enableScissor(x, y, x + w, y + h);
		int first = (int) (scroll / rowHeight);
		int last = Math.min(count - 1, (int) ((scroll + h) / rowHeight));
		int rw = rowWidth();
		for (int i = first; i <= last; i++) {
			int ry = y + i * rowHeight - (int) scroll;
			boolean hovered = Gfx.inside(mx, my, x, Math.max(ry, y), rw, rowHeight) && my >= y && my < y + h;
			if ((i & 1) == 1) c.fill(x, ry, x + rw, ry + rowHeight, Gfx.LIST_ALT);
			painter.paint(c, i, x, ry, rw, rowHeight, hovered);
		}
		c.disableScissor();
		if (hasBar()) {
			int bx = x + w - 6;
			c.fill(bx, y, bx + 6, y + h, 0xFF101010);
			int thumbH = Math.max(14, (int) ((double) h * h / (count * rowHeight)));
			int thumbY = y + (int) ((h - thumbH) * (scroll / maxScroll()));
			c.fill(bx, thumbY, bx + 6, thumbY + thumbH, Gfx.PANEL);
			c.fill(bx, thumbY, bx + 5, thumbY + 1, Gfx.WHITE);
			c.fill(bx + 5, thumbY, bx + 6, thumbY + thumbH, Gfx.SHADOW);
			c.fill(bx, thumbY + thumbH - 1, bx + 6, thumbY + thumbH, Gfx.SHADOW);
		}
	}

	boolean scrolled(double mx, double my, double amount) {
		if (!Gfx.inside(mx, my, x, y, w, h)) return false;
		scroll = MathHelper.clamp(scroll - amount * rowHeight * 2, 0, maxScroll());
		return true;
	}

	boolean pressBar(double mx, double my) {
		if (hasBar() && Gfx.inside(mx, my, x + w - 6, y, 6, h)) {
			dragging = true;
			dragTo(my);
			return true;
		}
		return false;
	}

	boolean drag(double my) {
		if (!dragging) return false;
		dragTo(my);
		return true;
	}

	boolean release() {
		boolean was = dragging;
		dragging = false;
		return was;
	}

	private void dragTo(double my) {
		int thumbH = Math.max(14, (int) ((double) h * h / (count * rowHeight)));
		double t = (my - y - thumbH / 2.0) / Math.max(1, h - thumbH);
		scroll = MathHelper.clamp(t, 0, 1) * maxScroll();
	}
}
