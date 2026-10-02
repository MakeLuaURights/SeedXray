package dev.seedxray;

import com.mojang.logging.LogUtils;
import dev.seedxray.command.SxCommands;
import dev.seedxray.crack.CrackController;
import dev.seedxray.config.SxConfig;
import dev.seedxray.gui.MenuScreen;
import dev.seedxray.render.Esp;
import dev.seedxray.render.EspRenderer;
import dev.seedxray.render.HudRenderer;
import dev.seedxray.seed.SeedFinder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

public class SeedXrayClient implements ClientModInitializer {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of(SeedXray.ID, "main"));
	public static final KeyBinding MENU_KEY = KeyBindingHelper.registerKeyBinding(
		new KeyBinding("key.seedxray.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY)
	);
	public static final KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(
		new KeyBinding("key.seedxray.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY)
	);

	@Override
	public void onInitializeClient() {
		SeedXray.config = SxConfig.load();
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> SeedXray.config.save());

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (MENU_KEY.wasPressed()) {
				if (client.currentScreen == null && client.world != null) client.setScreen(new MenuScreen());
			}
			while (TOGGLE_KEY.wasPressed()) {
				SeedXray.config.enabled = !SeedXray.config.enabled;
				SeedXray.config.save();
			}
			Esp.controller.tick(client);
			CrackController.INSTANCE.tick(client);
		});
		// The server puts the hashed seed in its join/respawn packets; the client keeps it as the world's biome seed.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (client.world != null) SeedFinder.onJoin(client, client.world.getBiomeAccess().seed);
		});
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
			CrackController.INSTANCE.worldChanged();
			SeedFinder.onJoin(client, world.getBiomeAccess().seed);
		});
		ClientChunkEvents.CHUNK_LOAD.register(CrackController.INSTANCE::onChunkLoad);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			SeedFinder.onDisconnect();
			Esp.controller.clear();
		});
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> SxCommands.register(dispatcher));

		EspRenderer.register();
		HudRenderer.register();
		LOGGER.info("[SeedXray] Ready. Press Right Shift in a world to open the menu.");
	}

	public static void openMenu() {
		MinecraftClient client = MinecraftClient.getInstance();
		client.send(() -> {
			if (client.world != null) client.setScreen(new MenuScreen());
		});
	}
}
