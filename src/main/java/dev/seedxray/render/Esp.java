package dev.seedxray.render;

import dev.seedxray.core.EspController;

/** Holder for the single {@link EspController}. */
public final class Esp {
	public static final EspController controller = new EspController();

	private Esp() {
	}
}
