package dev.combatfx.render;

import dev.combatfx.Config;
import java.util.ArrayList;
import java.util.List;

public final class IndicatorManager {
	private IndicatorManager() {}

	private static final List<FloatingText> ACTIVE = new ArrayList<>(Config.MAX_INDICATORS);

	public static void add(double x, double y, double z, String text, boolean crit, long now) {
		if (ACTIVE.size() >= Config.MAX_INDICATORS) ACTIVE.remove(0);
		ACTIVE.add(new FloatingText(x, y, z, text, crit, now));
	}

	/** Dipanggil tiap tick (bukan tiap frame) untuk membuang yang sudah habis. */
	public static void prune(long now) {
		for (int i = ACTIVE.size() - 1; i >= 0; i--) {
			FloatingText t = ACTIVE.get(i);
			if (now - t.spawnMs >= t.lifeMs) ACTIVE.remove(i);
		}
	}

	public static List<FloatingText> list() {
		return ACTIVE;
	}

	public static void clear() {
		ACTIVE.clear();
	}
}
