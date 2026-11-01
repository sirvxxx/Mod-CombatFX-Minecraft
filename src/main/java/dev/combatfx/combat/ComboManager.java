package dev.combatfx.combat;

import dev.combatfx.CombatConfig;

public final class ComboManager {
	private ComboManager() {}

	private static int count = 0;
	private static long lastHitMs = 0L;
	private static long lastIncrementMs = 0L;
	private static String label = "x0";

	private static long timeout() {
		return CombatConfig.get().comboTimeoutMs;
	}

	public static void registerHit(long now) {
		if (count > 0 && now - lastHitMs > timeout()) count = 0;
		count++;
		lastHitMs = now;
		lastIncrementMs = now;
		label = "x" + count;
	}

	public static int count(long now) {
		if (count > 0 && now - lastHitMs > timeout()) count = 0;
		return count;
	}

	public static long remainingMs(long now) {
		return Math.max(0L, timeout() - (now - lastHitMs));
	}

	public static float remainingFraction(long now) {
		return remainingMs(now) / (float) timeout();
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
