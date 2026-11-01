package dev.combatfx.render;

import dev.combatfx.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class IndicatorManager {
	private IndicatorManager() {}

	private static final List<FloatingText> ACTIVE = new ArrayList<>(Config.MAX_INDICATORS);

	public static void add(double x, double y, double z, String text, Tier tier, long now) {
		if (ACTIVE.size() >= Config.MAX_INDICATORS) ACTIVE.remove(0);

		// sebaran acak horizontal: arah acak, jarak 40%-100% dari drift tier
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		double angle = rnd.nextDouble(0, Math.PI * 2);
		double dist = tier.driftBlocks * rnd.nextDouble(0.4, 1.0);
		float dx = (float) (Math.cos(angle) * dist);
		float dz = (float) (Math.sin(angle) * dist);

		ACTIVE.add(new FloatingText(x, y, z, dx, dz, text, tier, now));
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
