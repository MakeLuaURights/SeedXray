package dev.seedxray.core;

/** Block positions to draw, nearest first. Immutable once published. */
public final class BoxList {
	public static final BoxList EMPTY = new BoxList(new int[0], new int[0], new int[0], new int[0], 0);

	public final int[] x;
	public final int[] y;
	public final int[] z;
	/** 0xRRGGBB */
	public final int[] rgb;
	public final int count;

	public BoxList(int[] x, int[] y, int[] z, int[] rgb, int count) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.rgb = rgb;
		this.count = count;
	}
}
