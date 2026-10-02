package dev.seedxray.sim;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

/** The three vanilla dimensions the mod understands. */
public enum Dim {
	OVERWORLD("Overworld", World.OVERWORLD),
	NETHER("Nether", World.NETHER),
	END("The End", World.END);

	public final String title;
	public final RegistryKey<World> key;

	Dim(String title, RegistryKey<World> key) {
		this.title = title;
		this.key = key;
	}

	public static Dim of(RegistryKey<World> key) {
		if (key == World.NETHER) return NETHER;
		if (key == World.END) return END;
		return OVERWORLD;
	}
}
