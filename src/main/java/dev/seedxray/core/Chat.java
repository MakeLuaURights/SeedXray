package dev.seedxray.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Local-only chat lines (never sent to the server). */
public final class Chat {
	private Chat() {
	}

	private static MutableText prefix() {
		return Text.literal("[").formatted(Formatting.DARK_GRAY)
			.append(Text.literal("SeedXray").formatted(Formatting.AQUA, Formatting.BOLD))
			.append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
	}

	/** Runs on the client thread; safe to call from any thread. */
	public static void send(Text body) {
		MinecraftClient client = MinecraftClient.getInstance();
		client.execute(() -> {
			if (client.inGameHud != null) {
				client.inGameHud.getChatHud().addMessage(prefix().append(body));
			}
		});
	}

	public static void info(String message) {
		send(Text.literal(message).formatted(Formatting.GRAY));
	}

	public static void good(String message) {
		send(Text.literal(message).formatted(Formatting.GREEN));
	}

	public static void warn(String message) {
		send(Text.literal(message).formatted(Formatting.YELLOW));
	}

	public static void error(String message) {
		send(Text.literal(message).formatted(Formatting.RED));
	}

	/** "Seed found: 123" where the number copies to the clipboard when clicked. */
	public static void seedFound(long seed, String how) {
		MutableText number = Text.literal(Long.toString(seed)).setStyle(Style.EMPTY
			.withColor(Formatting.GOLD)
			.withUnderline(true)
			.withClickEvent(new ClickEvent.CopyToClipboard(Long.toString(seed)))
			.withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to copy"))));
		MutableText line = Text.literal("Seed found: ").formatted(Formatting.GREEN).append(number);
		if (how != null && !how.isEmpty()) {
			line.append(Text.literal("  (" + how + ")").formatted(Formatting.DARK_GRAY));
		}
		send(line);
	}
}
