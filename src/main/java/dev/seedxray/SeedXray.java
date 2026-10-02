package dev.seedxray;

import dev.seedxray.config.SxConfig;
import dev.seedxray.core.Predictor;
import dev.seedxray.seed.SeedState;
import dev.seedxray.sim.Dim;

/** Global mod state. Everything here is created once at startup. */
public final class SeedXray {
	public static final String ID = "seedxray";

	public static SxConfig config = new SxConfig();
	public static final SeedState seed = new SeedState();
	public static final Predictor predictor = new Predictor();

	/** Dimension the player is in right now (client thread writes, anyone reads). */
	public static volatile Dim dim = Dim.OVERWORLD;
	/** Bumped whenever the set of selected structures/blocks changes so caches can refresh. */
	public static volatile int selectionVersion = 0;

	private SeedXray() {
	}

	public static void selectionChanged() {
		selectionVersion++;
	}
}
