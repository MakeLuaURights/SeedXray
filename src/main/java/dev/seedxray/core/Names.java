package dev.seedxray.core;

import java.util.Locale;

/** English display names derived from registry ids, so the menu reads the same in every game language. */
public final class Names {
	private Names() {
	}

	/** {@code deepslate_diamond_ore} becomes {@code Deepslate Diamond Ore}. */
	public static String title(String path) {
		StringBuilder sb = new StringBuilder(path.length());
		boolean upper = true;
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if (c == '_' || c == '/') {
				sb.append(' ');
				upper = true;
			} else {
				sb.append(upper ? Character.toUpperCase(c) : c);
				upper = false;
			}
		}
		return sb.toString();
	}

	public static String lower(String s) {
		return s.toLowerCase(Locale.ROOT);
	}
}
