package dev.seedxray.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.seedxray.SeedXray;
import dev.seedxray.SeedXrayClient;
import dev.seedxray.core.Chat;
import dev.seedxray.core.Marker;
import dev.seedxray.core.StructureSnapshot;
import dev.seedxray.seed.SeedFinder;
import dev.seedxray.seed.SeedState;
import dev.seedxray.sim.Dim;
import dev.seedxray.sim.StructureCatalog;
import dev.seedxray.sim.StructureEntry;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/** {@code /seedxray} (alias {@code /sx}): menu, seed, and a printout of where the selected structures are. */
public final class SxCommands {
	private SxCommands() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		var root = ClientCommandManager.literal("seedxray")
			.executes(ctx -> {
				SeedXrayClient.openMenu();
				return 1;
			})
			.then(ClientCommandManager.literal("menu").executes(ctx -> {
				SeedXrayClient.openMenu();
				return 1;
			}))
			.then(ClientCommandManager.literal("toggle").executes(ctx -> {
				SeedXray.config.enabled = !SeedXray.config.enabled;
				SeedXray.config.save();
				Chat.info("ESP " + (SeedXray.config.enabled ? "enabled" : "disabled"));
				return 1;
			}))
			.then(ClientCommandManager.literal("search").executes(ctx -> {
				SeedFinder.startSearch(MinecraftClient.getInstance());
				return 1;
			}))
			.then(ClientCommandManager.literal("where").executes(ctx -> {
				where();
				return 1;
			}))
			.then(ClientCommandManager.literal("seed")
				.executes(ctx -> {
					showSeed();
					return 1;
				})
				.then(ClientCommandManager.argument("seed", StringArgumentType.greedyString()).executes(ctx -> {
					String text = StringArgumentType.getString(ctx, "seed").trim();
					boolean force = text.endsWith(" force");
					if (force) text = text.substring(0, text.length() - 6).trim();
					String result = SeedFinder.setManual(MinecraftClient.getInstance(), text, force);
					Chat.info(result);
					return 1;
				})));
		var registered = dispatcher.register(root);
		dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("sx").redirect(registered));
	}

	private static void showSeed() {
		SeedState s = SeedXray.seed;
		if (s.known()) {
			Chat.seedFound(s.seed, s.status.name().toLowerCase(java.util.Locale.ROOT));
		} else if (s.status == SeedState.Status.SEARCHING) {
			Chat.info("Still searching... " + s.progress);
		} else if (s.hasHash) {
			Chat.warn("Seed not found. The server's hash is " + Long.toHexString(s.hashedSeed) + ". Use /seedxray seed <seed>.");
		} else {
			Chat.warn("No seed yet (not in a world).");
		}
	}

	private static void where() {
		MinecraftClient client = MinecraftClient.getInstance();
		StructureSnapshot snap = SeedXray.predictor.structures();
		if (client.player == null || !SeedXray.seed.known() || snap.dim() != Dim.of(client.world.getRegistryKey())) {
			Chat.warn("Nothing found yet (the seed must be known and the search finished).");
			return;
		}
		Map<String, Marker> nearest = new HashMap<>();
		BlockPos p = client.player.getBlockPos();
		for (Marker m : snap.markers()) {
			Marker best = nearest.get(m.id());
			if (best == null || m.pos().getSquaredDistance(p) < best.pos().getSquaredDistance(p)) nearest.put(m.id(), m);
		}
		if (nearest.isEmpty()) {
			Chat.warn("No selected structure found nearby. Select some in the menu (Right Shift).");
			return;
		}
		for (Marker m : nearest.values()) {
			StructureEntry e = StructureCatalog.get(m.id());
			String name = e != null ? e.name() : m.id();
			BlockPos pos = m.pos();
			String coords = pos.getX() + " " + pos.getY() + " " + pos.getZ();
			int dist = (int) Math.sqrt(pos.getSquaredDistance(p.getX(), pos.getY(), p.getZ()));
			MutableText line = Text.literal(name + ": ").formatted(Formatting.GRAY)
				.append(Text.literal(coords).setStyle(Style.EMPTY.withColor(Formatting.AQUA).withUnderline(true)
					.withClickEvent(new ClickEvent.CopyToClipboard(coords))
					.withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to copy")))))
				.append(Text.literal("  " + dist + " blocks").formatted(Formatting.DARK_GRAY));
			Chat.send(line);
		}
	}
}
