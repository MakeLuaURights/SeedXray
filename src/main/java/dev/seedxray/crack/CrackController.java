package dev.seedxray.crack;

import com.mojang.logging.LogUtils;
import dev.seedxray.SeedXray;
import dev.seedxray.core.Chat;
import dev.seedxray.crack.scan.ClientBlockSource;
import dev.seedxray.crack.scan.StructureScanner;
import dev.seedxray.seed.SeedFinder;
import dev.seedxray.seed.SeedState;
import dev.seedxray.sim.Dim;
import dev.seedxray.sim.WorldgenData;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;

/**
 * Works the seed out from what the player sees (the idea and the finders are SeedcrackerX's, MIT license,
 * https://github.com/19MisterX98/SeedcrackerX): structures and the End pillars are recognised in the loaded chunks;
 * once there is enough information the structure seed is searched in the background and the world seed is read off
 * the hash the server sent.
 */
public final class CrackController {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final long TREASURE = 1L << 60;
	private static final double NEEDED_WITH_PILLARS = 30;
	private static final double NEEDED_LIFTING = 40;

	public static final CrackController INSTANCE = new CrackController();

	public final CrackData data = new CrackData();
	private final ArrayDeque<Long> queue = new ArrayDeque<>();
	private final Set<Long> queued = new HashSet<>();
	private final Set<Long> scanned = new HashSet<>();
	private ClientWorld scannerWorld;
	private StructureScanner scanner;
	private boolean loading;
	private int ticks;

	private Thread searchThread;
	private volatile boolean cancel;
	private int searchedSignature = 0;
	private boolean dirty;

	private CrackController() {
	}

	/** Collecting only makes sense while the seed is unknown, on a server that sent a hash to check against. */
	public boolean active() {
		SeedState s = SeedXray.seed;
		return SeedXray.config.crackFromStructures && s.hasHash && !s.known() && MinecraftClient.getInstance().getServer() == null;
	}

	/** New world / seed found: forget everything. */
	public synchronized void reset() {
		cancel = true;
		data.clear();
		queue.clear();
		queued.clear();
		scanned.clear();
		scanner = null;
		scannerWorld = null;
		searchedSignature = 0;
		SeedXray.seed.crack = "";
	}

	/** The dimension changed: loaded chunks are different, what was already seen stays. */
	public synchronized void worldChanged() {
		queue.clear();
		queued.clear();
		scanned.clear();
		scanner = null;
		scannerWorld = null;
	}

	public void onChunkLoad(ClientWorld world, WorldChunk chunk) {
		if (!active()) return;
		int cx = chunk.getPos().x, cz = chunk.getPos().z;
		synchronized (this) {
			enqueue(TREASURE | key(cx, cz));
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) enqueue(key(cx + dx, cz + dz));
			}
		}
	}

	private void enqueue(long e) {
		if (queue.size() < 6000 && queued.add(e)) queue.add(e);
	}

	private static long key(int x, int z) {
		return ((long) (x & 0xFFFFFFF) << 28) | (z & 0xFFFFFFF);
	}

	private static int keyX(long k) {
		return (int) ((k >> 28) & 0xFFFFFFF) << 4 >> 4;
	}

	private static int keyZ(long k) {
		return (int) (k & 0xFFFFFFF) << 4 >> 4;
	}

	/** Called every client tick; does a couple of milliseconds of scanning at most. */
	public void tick(MinecraftClient client) {
		if (failed) return;
		try {
			tickInner(client);
		} catch (Throwable t) {
			// never let the cracker take the game down
			failed = true;
			LOGGER.error("[SeedXray] cracker stopped after an error", t);
			SeedXray.seed.crack = "stopped (error): " + t;
		}
	}

	private boolean failed;

	private void tickInner(MinecraftClient client) {
		if (client.world == null || !active()) return;
		ticks++;
		if (scanner == null || scannerWorld != client.world) {
			if (!WorldgenData.isLoaded()) {
				if (!loading) {
					loading = true;
					Thread t = new Thread(() -> {
						WorldgenData.get();
						loading = false;
					}, "SeedXray-worldgen-data");
					t.setDaemon(true);
					t.start();
				}
				return;
			}
			synchronized (this) {
				scannerWorld = client.world;
				scanner = new StructureScanner(new ClientBlockSource(client.world));
			}
		}
		Dim dim = Dim.of(client.world.getRegistryKey());
		long deadline = System.nanoTime() + 2_000_000L;
		ClientBlockSource loaded = new ClientBlockSource(client.world);
		while (System.nanoTime() < deadline) {
			Long e;
			synchronized (this) {
				e = queue.poll();
				if (e != null) queued.remove(e);
			}
			if (e == null) break;
			try {
				process(e, dim, loaded);
			} catch (Exception ex) {
				LOGGER.warn("[SeedXray] scan failed: {}", ex.toString());
			}
		}
		if (dim == Dim.END && data.pillars() == null && ticks % 20 == 0) {
			List<Integer> heights = scanner.scanPillars();
			if (heights != null) {
				PillarData pd = new PillarData(heights);
				if (!pd.candidates().isEmpty() && data.setPillars(pd)) {
					Chat.good("End pillars recorded (+16 bits).");
					changed();
				}
			}
		}
	}

	private void process(long e, Dim dim, ClientBlockSource world) {
		boolean treasure = (e & TREASURE) != 0;
		long k = e & ~TREASURE;
		int x = keyX(k), z = keyZ(k);
		if (treasure) {
			if (dim != Dim.OVERWORLD) return;
			add(scanner.scanTreasure(x, z));
			return;
		}
		if (scanned.contains(k)) return;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (!world.chunkLoaded(x + dx, z + dz)) return;
			}
		}
		scanned.add(k);
		for (Observation o : scanner.scan(dim, x, z)) add(o);
	}

	private void add(Observation o) {
		if (o == null) return;
		if (data.add(o)) {
			Chat.info("Spotted " + o.kind().title + " at chunk " + o.chunkX() + ", " + o.chunkZ() + ".  " + summary());
			changed();
		}
	}

	public String summary() {
		StringBuilder sb = new StringBuilder();
		sb.append(data.count()).append(" structures, ").append(String.format("%.0f", data.bits())).append(" bits");
		sb.append(data.pillars() != null ? ", End pillars: yes" : ", End pillars: no");
		return sb.toString();
	}

	/** One line for the menu: what is known and what is still missing. */
	public String needLine() {
		boolean pillars = data.pillars() != null;
		double bits = data.bits();
		double lift = data.liftingBits();
		if (pillars) {
			return bits >= NEEDED_WITH_PILLARS ? "enough data" : String.format("need %.0f more bits (with the End pillars)", NEEDED_WITH_PILLARS - bits);
		}
		return lift >= NEEDED_LIFTING ? "enough data" : String.format("need the End pillars, or %.0f more bits of temples/igloos/huts", NEEDED_LIFTING - lift);
	}

	private void changed() {
		SeedXray.seed.crack = summary() + " - " + needLine();
		search();
	}

	// ---------------------------------------------------------------- search

	private synchronized void search() {
		if (searchThread != null && searchThread.isAlive()) {
			dirty = true;
			return;
		}
		cancel = false;
		dirty = false;
		searchThread = new Thread(this::runSearch, "SeedXray-crack");
		searchThread.setDaemon(true);
		searchThread.setPriority(Thread.MIN_PRIORITY);
		searchThread.start();
	}

	private void runSearch() {
		try {
			while (!cancel) {
				doSearch();
				synchronized (this) {
					if (!dirty) return;
					dirty = false;
				}
			}
		} catch (Throwable t) {
			LOGGER.error("[SeedXray] seed cracker failed", t);
			SeedXray.seed.crack = "error: " + t;
		}
	}

	private void doSearch() {
		SeedState state = SeedXray.seed;
		int signature = data.signature();
		if (signature == searchedSignature) return;
		List<Observation> obs = data.observations();
		PillarData pillars = data.pillars();
		int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - (SeedXray.config.lowEnd ? 2 : 1));
		Set<Long> candidates = new HashSet<>();

		List<Integer> pillarSeeds = pillars == null ? List.of() : pillars.candidates();
		if (!pillarSeeds.isEmpty() && data.bits() >= NEEDED_WITH_PILLARS) {
			for (int p : pillarSeeds) {
				state.crack = "searching the structure seed (End pillars)...";
				candidates.addAll(Cracker.pillarSearch(p, obs, threads, () -> cancel, f -> state.crack = String.format("searching the structure seed %.0f%%", f * 100)));
			}
		} else if (data.liftingBits() >= NEEDED_LIFTING) {
			state.crack = "narrowing the seed down (temples, igloos, huts)...";
			List<Long> lowers = Cracker.liftLowerBits(obs);
			if (lowers.size() > 64) {
				state.crack = "not enough data yet - " + needLine();
				searchedSignature = signature;
				return;
			}
			Set<Long> found = Cracker.liftingSearch(obs, lowers, threads, () -> cancel, f -> state.crack = String.format("searching the structure seed %.0f%%", f * 100));
			if (!pillarSeeds.isEmpty()) {
				found = found.stream().filter(s -> pillarSeeds.contains(Cracker.pillarSeedOf(s))).collect(Collectors.toSet());
			}
			candidates.addAll(found);
		} else {
			state.crack = summary() + " - " + needLine();
			searchedSignature = signature;
			return;
		}
		if (cancel) return;
		searchedSignature = signature;
		if (candidates.isEmpty()) {
			state.crack = "no seed fits the data - a sighting may be wrong (/seedxray crack clear to start over)";
			Chat.warn("The structure data does not fit any seed. If a sighting was a false alarm, /seedxray crack clear starts over.");
			return;
		}
		if (candidates.size() > 200_000) {
			state.crack = candidates.size() + " candidates - more structures needed";
			return;
		}
		if (!state.hasHash) {
			state.crack = candidates.size() + " structure seed(s) but no hash from the server to finish with";
			return;
		}
		state.crack = "checking " + candidates.size() + " candidate(s) against the server's hash...";
		long hashed = state.hashedSeed;
		OptionalLong world = candidates.parallelStream().flatMap(s -> Cracker.worldSeedsFromHash(s, hashed).stream()).mapToLong(Long::longValue).findFirst();
		if (world.isPresent() && !cancel) {
			long seed = world.getAsLong();
			state.crack = "";
			SeedFinder.onCracked(seed);
		} else if (!cancel) {
			state.crack = "none of " + candidates.size() + " candidate(s) matches the server's hash";
		}
	}
}
