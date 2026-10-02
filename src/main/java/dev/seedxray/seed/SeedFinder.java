package dev.seedxray.seed;

import dev.seedxray.SeedXray;
import dev.seedxray.core.Chat;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.server.MinecraftServer;

/**
 * Finds the world seed and reports it in the local chat:
 * <ul>
 *   <li>singleplayer / LAN host: reads it from the integrated server, exact;</li>
 *   <li>multiplayer: takes the hashed seed the server sends on join and searches for the seed behind it
 *       (see {@link SeedSearch}); a seed typed with {@code /seedxray seed} is checked against the same hash.</li>
 * </ul>
 */
public final class SeedFinder {
	private static volatile int generation;

	private SeedFinder() {
	}

	/** Called on the client thread right after the join packet was handled. */
	public static void onJoin(MinecraftClient client, long hashedSeed) {
		SeedState state = SeedXray.seed;
		MinecraftServer server = client.getServer();
		if (server != null) {
			long seed = server.getOverworld().getSeed();
			if (state.known() && state.seed == seed && state.status == SeedState.Status.EXACT) return;
			generation++;
			state.seed = seed;
			state.hasHash = true;
			state.hashedSeed = hashedSeed;
			state.status = SeedState.Status.EXACT;
			state.progress = "";
			Chat.seedFound(seed, "singleplayer");
			SeedXray.predictor.reset();
			return;
		}
		if (state.hasHash && state.hashedSeed == hashedSeed && state.status != SeedState.Status.UNKNOWN) {
			return; // same world again (respawn / dimension change)
		}
		generation++;
		state.reset();
		state.hasHash = true;
		state.hashedSeed = hashedSeed;
		SeedXray.predictor.reset();

		String key = serverKey(client);
		Long saved = SeedXray.config.knownSeeds.get(key);
		if (saved != null && new SeedHash().matches(saved, hashedSeed)) {
			found(saved, SeedState.Status.VERIFIED, "saved for this server");
			return;
		}
		if (SeedXray.config.autoSeedSearch) {
			startSearch(client);
		} else {
			state.status = SeedState.Status.NOT_FOUND;
		}
	}

	public static void onDisconnect() {
		generation++;
		SeedXray.seed.reset();
		SeedXray.predictor.reset();
	}

	/** (Re)starts the background search for the current server's hashed seed. */
	public static void startSearch(MinecraftClient client) {
		SeedState state = SeedXray.seed;
		if (!state.hasHash || client.getServer() != null) return;
		final int myGeneration = ++generation;
		final long hashed = state.hashedSeed;
		final String key = serverKey(client);
		state.status = SeedState.Status.SEARCHING;
		state.progress = "starting";
		Chat.info("Looking for the seed (the server only sent its hash " + Long.toHexString(hashed) + ")...");

		List<Long> extra = new ArrayList<>();
		Long saved = SeedXray.config.knownSeeds.get(key);
		if (saved != null) extra.add(saved);
		extra.addAll(SeedSearch.readSeedFile(userSeedFile()));
		extra.addAll(SeedSearch.readSeedResource("/assets/seedxray/known_seeds.txt"));
		List<String> words = SeedSearch.readResourceLines("/assets/seedxray/seed_words.txt");
		int range = SeedXray.config.seedSearchRange;

		Thread t = new Thread(() -> {
			OptionalLong result = SeedSearch.run(hashed, extra, words, range, () -> generation != myGeneration, p -> {
				if (generation == myGeneration) state.progress = p;
			});
			client.execute(() -> {
				if (generation != myGeneration) return;
				if (result.isPresent()) {
					found(result.getAsLong(), SeedState.Status.VERIFIED, "matches the server's hash");
					SeedXray.config.knownSeeds.put(key, result.getAsLong());
					SeedXray.config.save();
				} else {
					state.status = SeedState.Status.NOT_FOUND;
					state.progress = "";
					Chat.warn("Seed not found automatically (it is not a small number or a common word).");
					Chat.warn("If you know it, type /seedxray seed <seed>. It is checked against the server's hash.");
					Chat.warn("(Some servers scramble that hash; then /seedxray seed <seed> force skips the check.)");
				}
			});
		}, "SeedXray-seed-search");
		t.setDaemon(true);
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	/**
	 * Applies a seed typed by the user.
	 *
	 * @return a message for the user
	 */
	public static String setManual(MinecraftClient client, String text, boolean force) {
		SeedState state = SeedXray.seed;
		long seed = SeedSearch.parseSeed(text);
		if (client.getServer() != null) {
			return "Singleplayer: the seed is read from the world, nothing to set.";
		}
		if (state.hasHash && !force) {
			if (!new SeedHash().matches(seed, state.hashedSeed)) {
				return "That seed does not match this server's hash, so it is not the seed of this world. (Add 'force' to use it anyway.)";
			}
			generation++;
			found(seed, SeedState.Status.VERIFIED, "matches the server's hash");
			SeedXray.config.knownSeeds.put(serverKey(client), seed);
			SeedXray.config.save();
			return "Seed verified and saved for this server.";
		}
		generation++;
		found(seed, SeedState.Status.UNVERIFIED, state.hasHash ? "forced, NOT verified" : "not verified");
		return "Seed set (unverified).";
	}

	private static void found(long seed, SeedState.Status status, String how) {
		SeedState state = SeedXray.seed;
		state.seed = seed;
		state.status = status;
		state.progress = "";
		SeedXray.predictor.reset();
		Chat.seedFound(seed, how);
	}

	public static String serverKey(MinecraftClient client) {
		ServerInfo info = client.getCurrentServerEntry();
		return info != null && info.address != null ? info.address.toLowerCase(java.util.Locale.ROOT) : "unknown";
	}

	private static Path userSeedFile() {
		return FabricLoader.getInstance().getConfigDir().resolve("seedxray_seeds.txt");
	}
}
