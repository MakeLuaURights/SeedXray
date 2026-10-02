package dev.seedxray.seed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Looks for a seed whose hash equals the hashed seed sent by the server. It tries, in order: seeds the user
 * saved, a list of well known seeds, plain numbers around zero (what most servers use), then text seeds
 * ({@code String.hashCode}, the way the game converts a typed seed). A world seed can be any 64 bit number, so a
 * random one cannot be found this way; for those the seed has to be typed in once.
 */
public final class SeedSearch {
	private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

	private SeedSearch() {
	}

	/**
	 * @param extra candidate seeds known up front (saved per server, user file, built in list)
	 * @param cancelled polled often; return true to stop
	 * @param progress receives short status lines
	 */
	public static OptionalLong run(long hashedSeed, List<Long> extra, List<String> words, int range, BooleanSupplier cancelled, Consumer<String> progress) {
		SeedHash hash = new SeedHash();
		progress.accept("known seeds");
		for (long candidate : extra) {
			if (hash.matches(candidate, hashedSeed)) return OptionalLong.of(candidate);
		}
		// numbers, growing outward from 0 so small seeds are found first
		progress.accept("numbers 0.." + range);
		for (long i = 0; i <= range; i++) {
			if ((i & 0xFFFF) == 0) {
				if (cancelled.getAsBoolean()) return OptionalLong.empty();
				if ((i & 0xFFFFF) == 0 && i > 0) progress.accept("numbers " + (i * 100L / Math.max(1, range)) + "%");
			}
			if (hash.matches(i, hashedSeed)) return OptionalLong.of(i);
			if (i != 0 && hash.matches(-i, hashedSeed)) return OptionalLong.of(-i);
		}
		// text seeds: words, then every 1..4 character lowercase/digit string
		progress.accept("text seeds");
		for (String word : words) {
			if (hash.matches(word.hashCode(), hashedSeed)) return OptionalLong.of(word.hashCode());
			if (cancelled.getAsBoolean()) return OptionalLong.empty();
		}
		StringBuilder sb = new StringBuilder();
		for (int length = 1; length <= 4; length++) {
			if (cancelled.getAsBoolean()) return OptionalLong.empty();
			OptionalLong hit = text(hash, hashedSeed, sb, length, cancelled);
			if (hit.isPresent()) return hit;
		}
		return OptionalLong.empty();
	}

	private static OptionalLong text(SeedHash hash, long target, StringBuilder sb, int remaining, BooleanSupplier cancelled) {
		if (remaining == 0) {
			String s = sb.toString();
			return hash.matches(s.hashCode(), target) ? OptionalLong.of(s.hashCode()) : OptionalLong.empty();
		}
		for (int i = 0; i < ALPHABET.length(); i++) {
			sb.append(ALPHABET.charAt(i));
			OptionalLong hit = text(hash, target, sb, remaining - 1, cancelled);
			sb.setLength(sb.length() - 1);
			if (hit.isPresent()) return hit;
		}
		return OptionalLong.empty();
	}

	/** Seeds listed one per line in a text file (numbers; anything else is treated as a text seed). */
	public static List<Long> readSeedFile(Path file) {
		List<Long> out = new ArrayList<>();
		if (!Files.exists(file)) return out;
		try {
			for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
				addSeedLine(line, out);
			}
		} catch (IOException ignored) {
			// optional file
		}
		return out;
	}

	public static List<Long> readSeedResource(String resource) {
		List<Long> out = new ArrayList<>();
		for (String line : readResourceLines(resource)) {
			addSeedLine(line, out);
		}
		return out;
	}

	public static List<String> readResourceLines(String resource) {
		List<String> lines = new ArrayList<>();
		try (InputStream in = SeedSearch.class.getResourceAsStream(resource)) {
			if (in == null) return lines;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (!line.isEmpty() && !line.startsWith("#")) lines.add(line);
				}
			}
		} catch (IOException ignored) {
			// optional resource
		}
		return lines;
	}

	private static void addSeedLine(String line, List<Long> out) {
		String s = line.split("#", 2)[0].trim();
		if (s.isEmpty()) return;
		out.add(parseSeed(s));
	}

	/** Same conversion the game uses for the seed text field: a number if it is one, otherwise its hash code. */
	public static long parseSeed(String text) {
		try {
			return Long.parseLong(text.trim());
		} catch (NumberFormatException e) {
			return text.hashCode();
		}
	}
}
