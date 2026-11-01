package dev.combatfx.render;

import dev.combatfx.Config;

/** Satu angka melayang. Immutable setelah dibuat. */
public final class FloatingText {
	public final double x, y, z;
	public final String text;
	public final boolean crit;
	public final long spawnMs;
	public final long lifeMs;

	public FloatingText(double x, double y, double z, String text, boolean crit, long spawnMs) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.text = text;
		this.crit = crit;
		this.spawnMs = spawnMs;
		this.lifeMs = crit ? Config.CRIT_LIFETIME_MS : Config.NORMAL_LIFETIME_MS;
	}
}
