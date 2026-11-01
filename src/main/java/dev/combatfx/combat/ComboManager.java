package dev.combatfx.combat;

import dev.combatfx.Config;

/** Logika combo. Semua dipanggil dari render/client thread, jadi tidak perlu sinkronisasi. */
public final class ComboManager {
	private ComboManager() {}

	private static int count = 0;
	private static long lastHitMs = 0L;
	private static long lastIncrementMs = 0L;
	private static String label = "x0";

	public static void registerHit(long now) {
		if (count > 0 && now - lastHitMs > Config.COMBO_TIMEOUT_MS) {
			count = 0;
		}
		count++;
		lastHitMs = now;
		lastIncrementMs = now;
		label = "x" + count; // di-cache supaya render tidak alokasi string tiap frame
	}

	/** Jumlah combo saat ini; otomatis reset ke 0 kalau sudah timeout. */
	public static int count(long now) {
		if (count > 0 && now - lastHitMs > Config.COMBO_TIMEOUT_MS) {
			count = 0;
		}
		return count;
	}

	public static long remainingMs(long now) {
		return Math.max(0L, Config.COMBO_TIMEOUT_MS - (now - lastHitMs));
	}

	public static float remainingFraction(long now) {
		return remainingMs(now) / (float) Config.COMBO_TIMEOUT_MS;
	}

	public static long sinceIncrementMs(long now) {
		return now - lastIncrementMs;
	}

	public static String label() {
		return label;
	}

	public static void reset() {
		count = 0;
		label = "x0";
	}
}
