package dev.seedxray.crack;

import dev.seedxray.seed.SeedHash;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * Finds the seed from sightings. Ported from SeedcrackerX's "time machine" (https://github.com/19MisterX98/SeedcrackerX,
 * MIT): the structure seed (48 bits) is searched with the observed structure placements as filters, then the world seed
 * is the structure seed plus 16 more bits, found by checking the hash the server sent.
 *
 * <ul>
 *   <li><b>pillar path</b>: the End pillars give 16 bits of the generator state, so only 2^32 candidates remain;</li>
 *   <li><b>lifting path</b> (no End visit needed): old structures (temples, huts, igloos) place themselves with
 *       {@code nextInt(24)}, whose low bits depend only on the low 19 bits of the seed, which cuts the search
 *       to a few survivors times 2^29.</li>
 * </ul>
 */
public final class Cracker {
	private static final long MULT = 0x5DEECE66DL;
	private static final long MASK = (1L << 48) - 1;
	private static final long INVERSE = BigInteger.valueOf(MULT).modInverse(BigInteger.ONE.shiftLeft(48)).longValue();

	private Cracker() {
	}

	/** Orders sightings so the most selective (cheapest to reject with) come first. */
	static List<Observation> ordered(List<Observation> all) {
		List<Observation> list = new ArrayList<>(all);
		list.sort(Comparator.comparingInt((Observation o) -> o.placement().hasFrequency ? 1 : 0).thenComparingDouble(o -> -o.placement().bits()));
		return list;
	}

	/**
	 * The sightings turned into plain arrays so the test of one candidate seed is a few dozen arithmetic operations
	 * and no allocation or lookup (it runs billions of times).
	 */
	static final class Compiled {
		private final int n;
		private final long[] base; // rx * REGION_X + rz * REGION_Z + salt
		private final int[] range;
		private final int[] ox;
		private final int[] oz;
		private final boolean[] simple; // linear and not a power of two: the inlined path
		private final List<Observation> slow = new ArrayList<>();
		private final Observation[] all;

		Compiled(List<Observation> ordered) {
			List<Observation> fast = new ArrayList<>();
			for (Observation o : ordered) {
				Placement p = o.placement();
				if (!p.hasFrequency) fast.add(o);
				else slow.add(o);
			}
			n = fast.size();
			base = new long[n];
			range = new int[n];
			ox = new int[n];
			oz = new int[n];
			simple = new boolean[n];
			all = fast.toArray(new Observation[0]);
			for (int i = 0; i < n; i++) {
				Observation o = fast.get(i);
				Placement p = o.placement();
				base[i] = p.regionX(o.chunkX()) * 341873128712L + p.regionX(o.chunkZ()) * 132897987541L + p.salt;
				range[i] = p.range;
				ox[i] = p.offset(o.chunkX());
				oz[i] = p.offset(o.chunkZ());
				simple[i] = !p.triangular && (p.range & -p.range) != p.range;
			}
		}

		boolean test(long seed) {
			for (int i = 0; i < n; i++) {
				if (simple[i]) {
					long state = ((base[i] + seed) ^ MULT) & MASK;
					int bound = range[i];
					int bits, val;
					do {
						state = (state * MULT + 0xBL) & MASK;
						bits = (int) (state >>> 17);
						val = bits % bound;
					} while (bits - val + (bound - 1) < 0);
					if (val != ox[i]) return false;
					do {
						state = (state * MULT + 0xBL) & MASK;
						bits = (int) (state >>> 17);
						val = bits % bound;
					} while (bits - val + (bound - 1) < 0);
					if (val != oz[i]) return false;
				} else if (!all[i].matches(seed)) {
					return false;
				}
			}
			for (Observation o : slow) {
				if (!o.matches(seed)) return false;
			}
			return true;
		}
	}

	static boolean testAll(List<Observation> obs, long seed) {
		for (Observation o : obs) {
			if (!o.matches(seed)) return false;
		}
		return true;
	}

	/** Undo one step of the generator: the state before {@code state}. */
	private static long previous(long state) {
		return ((state - 0xBL) * INVERSE) & MASK;
	}

	/** The 16 bit pillar seed a structure seed leads to (the game's {@code Random(seed).nextLong() & 65535}). */
	public static int pillarSeedOf(long structureSeed) {
		java.util.Random random = new java.util.Random(structureSeed);
		return (int) (random.nextLong() & 65535L);
	}

	// ----------------------------------------------------------------- pillar path

	/**
	 * All structure seeds consistent with the pillar seed and every sighting. The pillar seed fixes 16 bits of the
	 * generator state two steps after the seed, which leaves 2^32 candidates.
	 */
	public static Set<Long> pillarSearch(int pillarSeed, List<Observation> sightings, int threads, BooleanSupplier cancelled, DoubleConsumer progress) {
		Compiled obs = new Compiled(ordered(sightings));
		Set<Long> found = ConcurrentHashMap.newKeySet();
		AtomicLong next = new AtomicLong();
		AtomicLong done = new AtomicLong();
		long total = (1L << 32) / (1L << 20);
		Thread[] workers = new Thread[threads];
		for (int t = 0; t < threads; t++) {
			workers[t] = new Thread(() -> {
				long block;
				while ((block = next.getAndIncrement()) < total && !cancelled.getAsBoolean()) {
					long base = block << 20;
					for (long i = 0; i < (1L << 20); i++) {
						long partial = base | i;
						long s2 = ((partial & 0xFFFF0000L) << 16) | ((long) pillarSeed << 16) | (partial & 0xFFFFL);
						long s0 = previous(previous(s2));
						long seed = s0 ^ MULT;
						if (obs.test(seed)) found.add(seed);
					}
					progress.accept(done.incrementAndGet() / (double) total);
				}
			}, "SeedXray-crack-pillar");
			workers[t].setDaemon(true);
			workers[t].setPriority(Thread.MIN_PRIORITY);
			workers[t].start();
		}
		join(workers);
		return found;
	}

	// ----------------------------------------------------------------- lifting path

	/** Candidates for the low 19 bits that agree with every liftable sighting modulo 4. */
	public static List<Long> liftLowerBits(List<Observation> sightings) {
		List<Observation> lift = new ArrayList<>();
		for (Observation o : sightings) {
			if (o.kind().liftable() && o.placement().range % 4 == 0) lift.add(o);
		}
		List<Long> out = new ArrayList<>();
		for (long lower = 0; lower < (1L << 19); lower++) {
			boolean ok = true;
			for (Observation o : lift) {
				Placement p = o.placement();
				long packed = p.startOffsets(lower, p.regionX(o.chunkX()), p.regionX(o.chunkZ()));
				int x = (int) (packed >> 32), z = (int) packed;
				if ((x & 3) != (p.offset(o.chunkX()) & 3) || (z & 3) != (p.offset(o.chunkZ()) & 3)) {
					ok = false;
					break;
				}
			}
			if (ok) out.add(lower);
		}
		return out;
	}

	public static double liftingBits(List<Observation> sightings) {
		double bits = 0;
		for (Observation o : sightings) {
			if (o.kind().liftable()) bits += o.placement().bits();
		}
		return bits;
	}

	public static Set<Long> liftingSearch(List<Observation> sightings, List<Long> lowers, int threads, BooleanSupplier cancelled, DoubleConsumer progress) {
		Compiled obs = new Compiled(ordered(sightings));
		Set<Long> found = ConcurrentHashMap.newKeySet();
		long perLower = 1L << 29;
		long blockSize = 1L << 20;
		long blocksPerLower = perLower / blockSize;
		long total = blocksPerLower * lowers.size();
		AtomicLong next = new AtomicLong();
		AtomicLong done = new AtomicLong();
		Thread[] workers = new Thread[threads];
		for (int t = 0; t < threads; t++) {
			workers[t] = new Thread(() -> {
				long block;
				while ((block = next.getAndIncrement()) < total && !cancelled.getAsBoolean()) {
					long lower = lowers.get((int) (block / blocksPerLower));
					long upperBase = (block % blocksPerLower) * blockSize;
					for (long i = 0; i < blockSize; i++) {
						long seed = ((upperBase + i) << 19) | lower;
						if (obs.test(seed)) found.add(seed);
					}
					progress.accept(done.incrementAndGet() / (double) total);
				}
			}, "SeedXray-crack-lift");
			workers[t].setDaemon(true);
			workers[t].setPriority(Thread.MIN_PRIORITY);
			workers[t].start();
		}
		join(workers);
		return found;
	}

	// ----------------------------------------------------------------- world seed

	/** World seeds (all 64 bits) whose low 48 bits are {@code structureSeed} and whose hash is {@code hashed}. */
	public static List<Long> worldSeedsFromHash(long structureSeed, long hashed) {
		SeedHash hash = new SeedHash();
		List<Long> out = new ArrayList<>();
		for (long upper = 0; upper < (1L << 16); upper++) {
			long seed = (upper << 48) | structureSeed;
			if (hash.matches(seed, hashed)) out.add(seed);
		}
		return out;
	}

	private static void join(Thread[] workers) {
		for (Thread w : workers) {
			try {
				w.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}
}
