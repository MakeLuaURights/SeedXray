package dev.seedxray.seed;

/** What the mod currently knows about the world seed. Written by the seed finder, read everywhere. */
public final class SeedState {
	public enum Status {
		/** Not in a world, or no information yet. */
		UNKNOWN,
		/** Server world: looking for a seed that matches the hashed seed the server sent. */
		SEARCHING,
		/** Singleplayer / LAN host: read straight from the integrated server. */
		EXACT,
		/** Matches the hashed seed the server sent, so it is certainly the real seed. */
		VERIFIED,
		/** Entered by the user without a way to verify it. */
		UNVERIFIED,
		/** The automatic search came up empty; waiting for the user to supply a seed. */
		NOT_FOUND
	}

	public volatile Status status = Status.UNKNOWN;
	public volatile long seed;
	/** The first 8 bytes of SHA-256(seed) the server sends on join, if we saw it. */
	public volatile boolean hasHash;
	public volatile long hashedSeed;
	/** Human readable progress of the search, for the menu. */
	public volatile String progress = "";
	/** What the structure based cracker is doing, for the menu and the HUD. */
	public volatile String crack = "";

	public boolean known() {
		Status s = status;
		return s == Status.EXACT || s == Status.VERIFIED || s == Status.UNVERIFIED;
	}

	public void reset() {
		status = Status.UNKNOWN;
		seed = 0;
		hasHash = false;
		hashedSeed = 0;
		progress = "";
		crack = "";
	}
}
