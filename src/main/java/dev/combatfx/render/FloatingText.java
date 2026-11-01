package dev.combatfx.render;

/** Satu angka melayang. Immutable setelah dibuat. */
public final class FloatingText {
	public final double x, y, z;
	public final float driftX, driftZ; // total sebaran horizontal (blok) selama hidup
	public final String text;
	public final Tier tier;
	public final long spawnMs;
	public final long lifeMs;

	public FloatingText(double x, double y, double z, float driftX, float driftZ,
			String text, Tier tier, long spawnMs) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.driftX = driftX;
		this.driftZ = driftZ;
		this.text = text;
		this.tier = tier;
		this.spawnMs = spawnMs;
		this.lifeMs = tier.lifeMs;
	}
}
