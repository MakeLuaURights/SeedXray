package dev.seedxray.crack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** What the cracker has seen so far: structure sightings and the End pillars. Thread safe. */
public final class CrackData {
	private final List<Observation> observations = new CopyOnWriteArrayList<>();
	private volatile PillarData pillars;

	public synchronized boolean add(Observation o) {
		if (observations.contains(o)) return false;
		observations.add(o);
		return true;
	}

	public synchronized boolean setPillars(PillarData data) {
		if (pillars != null) return false;
		pillars = data;
		return true;
	}

	public List<Observation> observations() {
		return new ArrayList<>(observations);
	}

	public PillarData pillars() {
		return pillars;
	}

	public double bits() {
		double sum = 0;
		for (Observation o : observations) sum += o.placement().bits();
		return sum;
	}

	public double liftingBits() {
		return Cracker.liftingBits(observations);
	}

	public int count() {
		return observations.size();
	}

	public synchronized void clear() {
		observations.clear();
		pillars = null;
	}

	/** Changes whenever the data changes. */
	public synchronized int signature() {
		return observations.hashCode() * 31 + (pillars == null ? 0 : pillars.heights().hashCode());
	}
}
