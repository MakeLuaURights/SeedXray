package dev.seedxray.seed;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The server only tells clients {@code first 8 bytes of SHA-256(seed)} (since 1.15). That cannot be inverted,
 * but any candidate seed can be checked against it, which is exactly what this class does, quickly.
 * Matches {@code BiomeAccess.hashSeed}.
 */
public final class SeedHash {
	private final MessageDigest digest;
	private final byte[] input = new byte[8];

	public SeedHash() {
		try {
			this.digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public long hash(long seed) {
		for (int i = 0; i < 8; i++) {
			input[i] = (byte) (seed >>> (8 * i));
		}
		digest.update(input);
		byte[] out = digest.digest();
		long result = 0;
		for (int i = 0; i < 8; i++) {
			result |= (out[i] & 0xFFL) << (8 * i);
		}
		return result;
	}

	public boolean matches(long seed, long hashedSeed) {
		return hash(seed) == hashedSeed;
	}
}
