package dev.seedxray;

import net.minecraft.SharedConstants;
import net.minecraft.Bootstrap;

public final class McBootstrap {
	private static boolean done;

	public static synchronized void init() {
		if (done) return;
		SharedConstants.createGameVersion();
		Bootstrap.initialize();
		// A running game has these bound already; outside of it bind the block tags ourselves.
		var rm = new net.minecraft.resource.LifecycledResourceManagerImpl(net.minecraft.resource.ResourceType.SERVER_DATA,
			java.util.List.of(net.minecraft.resource.VanillaDataPackProvider.createDefaultPack()));
		net.minecraft.registry.Registries.REGISTRIES.streamEntries().forEach(r -> {
			if (r.value() instanceof net.minecraft.registry.MutableRegistry<?> m) {
				loadTags(rm, m);
			}
		});
		net.minecraft.registry.Registries.REGISTRIES.streamEntries().forEach(r -> r.value().freeze());
		done = true;
	}

	private static <T> void loadTags(net.minecraft.resource.ResourceManager rm, net.minecraft.registry.MutableRegistry<T> registry) {
		net.minecraft.registry.tag.TagGroupLoader.loadInitial(rm, registry);
	}
}
