package dev.seedxray.core;

import com.mojang.logging.LogUtils;
import dev.seedxray.SeedXray;
import dev.seedxray.sim.Dim;
import dev.seedxray.sim.DimensionSim;
import dev.seedxray.sim.OreChunk;
import dev.seedxray.sim.OreSimulator;
import dev.seedxray.sim.StructureCatalog;
import dev.seedxray.sim.StructureHit;
import dev.seedxray.sim.StructureLocator;
import dev.seedxray.sim.TerrainCache;
import dev.seedxray.sim.WorldgenData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;

/**
 * Runs the world generation simulation on one low priority background thread, so the game thread never waits for it.
 * The client thread asks for structures / ore chunks around the player; results are published as immutable snapshots.
 */
public final class Predictor {
	private static final Logger LOGGER = LogUtils.getLogger();

	private record StructRequest(long seed, Dim dim, BlockPos center, int radius, Set<String> wanted) {
	}

	private record OreRequest(long seed, Dim dim, int cx, int cz, int radius) {
	}

	/** Everything built for one seed + dimension. */
	private static final class Ctx {
		final long seed;
		final Dim dim;
		final DimensionSim sim;
		final StructureLocator locator;
		final TerrainCache terrain;
		final OreSimulator ores;

		Ctx(long seed, Dim dim, boolean lowEnd) {
			this.seed = seed;
			this.dim = dim;
			this.sim = new DimensionSim(dim, seed);
			this.locator = new StructureLocator(sim);
			// the ore pass of one chunk reads its 3x3 neighbourhood, so keep a bit more than a full row around
			this.terrain = new TerrainCache(sim, lowEnd ? 60 : 140, true);
			this.ores = new OreSimulator(sim, terrain);
		}
	}

	private final Object lock = new Object();
	private StructRequest structRequest;
	private OreRequest oreRequest;
	private Thread worker;
	private volatile int epoch;

	private volatile StructureSnapshot structures = StructureSnapshot.EMPTY;
	private volatile ConcurrentHashMap<Long, OreChunk> oreStore = new ConcurrentHashMap<>();
	private volatile String status = "";
	private volatile String error;
	private volatile int oreDone;
	private volatile int oreTotal;

	/** Forget everything (new seed, new world). */
	public void reset() {
		synchronized (lock) {
			epoch++;
			structRequest = null;
			oreRequest = null;
			structures = StructureSnapshot.EMPTY;
			oreStore = new ConcurrentHashMap<>();
			error = null;
			status = "";
			oreDone = oreTotal = 0;
			lock.notifyAll();
		}
	}

	public void requestStructures(long seed, Dim dim, BlockPos center, int radius, Set<String> wanted) {
		synchronized (lock) {
			ensureWorker();
			structRequest = new StructRequest(seed, dim, center, radius, new HashSet<>(wanted));
			lock.notifyAll();
		}
	}

	public void requestOres(long seed, Dim dim, int cx, int cz, int radius) {
		synchronized (lock) {
			OreRequest r = new OreRequest(seed, dim, cx, cz, radius);
			if (r.equals(oreRequest)) return;
			ensureWorker();
			oreRequest = r;
			lock.notifyAll();
		}
	}

	public StructureSnapshot structures() {
		return structures;
	}

	public OreChunk ore(int cx, int cz) {
		return oreStore.get(ChunkPos.toLong(cx, cz));
	}

	/** Short line for the menu, empty when idle. */
	public String status() {
		String err = error;
		if (err != null) return "Error: " + err;
		return status;
	}

	private void ensureWorker() {
		if (worker == null || !worker.isAlive()) {
			worker = new Thread(this::run, "SeedXray-worker");
			worker.setDaemon(true);
			worker.setPriority(Thread.MIN_PRIORITY);
			worker.start();
		}
	}

	// ---------------------------------------------------------------- worker

	private Ctx ctx;

	private Ctx ctxFor(long seed, Dim dim) {
		if (ctx == null || ctx.seed != seed || ctx.dim != dim) {
			ctx = null; // let the old one be collected before building the new one
			status = WorldgenData.isLoaded() ? "Preparing " + dim.title + "..." : "Loading world generation data...";
			ctx = new Ctx(seed, dim, SeedXray.config.lowEnd);
		}
		return ctx;
	}

	private void run() {
		while (true) {
			StructRequest sr;
			OreRequest or = null;
			int myEpoch;
			synchronized (lock) {
				while (structRequest == null && oreRequest == null) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
						return;
					}
				}
				myEpoch = epoch;
				sr = structRequest;
				structRequest = null;
				if (sr == null) {
					or = oreRequest;
					oreRequest = null;
				}
			}
			try {
				if (sr != null) {
					doStructures(sr, myEpoch);
				} else if (or != null) {
					doOres(or, myEpoch);
				}
				if (error != null && epoch == myEpoch) error = null;
			} catch (Throwable t) {
				LOGGER.error("[SeedXray] World generation prediction failed", t);
				error = t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : "");
				ctx = null;
			}
			if (epoch == myEpoch && !hasPending()) status = "";
		}
	}

	private boolean hasPending() {
		synchronized (lock) {
			return structRequest != null || oreRequest != null;
		}
	}

	private boolean stale(int myEpoch) {
		return epoch != myEpoch;
	}

	private void doStructures(StructRequest req, int myEpoch) {
		Ctx c = ctxFor(req.seed, req.dim);
		status = "Locating structures...";
		List<Marker> markers = new ArrayList<>();
		List<StructureHit> strongholds = new ArrayList<>();
		List<StructureHit> cities = new ArrayList<>();
		BlockPos center = req.center;
		int[] sinceLastPublish = {0};
		c.locator.locate(center, req.radius, req.wanted, hit -> {
			if (req.wanted.contains(hit.id)) {
				markers.add(new Marker(hit.id, hit.pos, hit.box));
			}
			if (hit.id.equals("stronghold")) strongholds.add(hit);
			if (hit.id.equals("end_city")) cities.add(hit);
			if (++sinceLastPublish[0] >= 6) {
				sinceLastPublish[0] = 0;
				publish(req, myEpoch, markers, false);
			}
		}, () -> stale(myEpoch) || hasStructRequest());
		if (stale(myEpoch)) return;

		// pseudo structures that need the structure's pieces generated: the end portal and the elytra frame
		if (req.wanted.contains(StructureCatalog.END_PORTAL)) {
			strongholds.sort(Comparator.comparingDouble(h -> h.pos.getSquaredDistance(center)));
			for (int i = 0; i < Math.min(4, strongholds.size()); i++) {
				StructureHit h = strongholds.get(i);
				c.locator.detail(h);
				if (h.detail() != null && h.detail().portal() != null) {
					BlockPos p = h.detail().portal();
					markers.add(new Marker(StructureCatalog.END_PORTAL, p, new net.minecraft.util.math.BlockBox(p.getX() - 2, p.getY(), p.getZ() - 2, p.getX() + 2, p.getY() + 1, p.getZ() + 2)));
				}
			}
		}
		if (req.wanted.contains(StructureCatalog.END_SHIP_ELYTRA)) {
			for (StructureHit h : cities) {
				if (stale(myEpoch)) return;
				c.locator.detail(h);
				if (h.detail() != null && h.detail().elytra() != null) {
					BlockPos p = h.detail().elytra();
					markers.add(new Marker(StructureCatalog.END_SHIP_ELYTRA, p, new net.minecraft.util.math.BlockBox(p.getX() - 1, p.getY() - 1, p.getZ() - 1, p.getX() + 1, p.getY() + 1, p.getZ() + 1)));
				}
			}
		}
		publish(req, myEpoch, markers, true);
	}

	private boolean hasStructRequest() {
		synchronized (lock) {
			return structRequest != null;
		}
	}

	private void publish(StructRequest req, int myEpoch, List<Marker> markers, boolean complete) {
		if (stale(myEpoch)) return;
		structures = new StructureSnapshot(req.seed, req.dim, req.center, req.radius, List.copyOf(markers), complete);
	}

	private void doOres(OreRequest req, int myEpoch) throws InterruptedException {
		Ctx c = ctxFor(req.seed, req.dim);
		ConcurrentHashMap<Long, OreChunk> store = oreStore;
		List<ChunkPos> todo = new ArrayList<>();
		int total = 0;
		for (int dx = -req.radius; dx <= req.radius; dx++) {
			for (int dz = -req.radius; dz <= req.radius; dz++) {
				total++;
				if (!store.containsKey(ChunkPos.toLong(req.cx + dx, req.cz + dz))) todo.add(new ChunkPos(req.cx + dx, req.cz + dz));
			}
		}
		oreTotal = total;
		oreDone = total - todo.size();
		todo.sort(Comparator.comparingInt(p -> (p.x - req.cx) * (p.x - req.cx) + (p.z - req.cz) * (p.z - req.cz)));
		for (ChunkPos pos : todo) {
			if (stale(myEpoch) || hasOreRequest()) return;
			status = "Predicting ores " + oreDone + "/" + oreTotal + "...";
			OreChunk chunk = c.ores.simulate(pos.x, pos.z);
			if (stale(myEpoch)) return;
			store.put(pos.toLong(), chunk);
			oreDone++;
			if (SeedXray.config.lowEnd) Thread.sleep(3);
		}
		// forget chunks the player has left far behind
		int keep = req.radius + 3;
		store.keySet().removeIf(key -> {
			ChunkPos p = new ChunkPos(key);
			return Math.abs(p.x - req.cx) > keep || Math.abs(p.z - req.cz) > keep;
		});
	}

	private boolean hasOreRequest() {
		synchronized (lock) {
			return oreRequest != null;
		}
	}
}
