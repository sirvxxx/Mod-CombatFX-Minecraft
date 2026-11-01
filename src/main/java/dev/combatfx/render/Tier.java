package dev.combatfx.render;

import dev.combatfx.CombatConfig;

/** Tuning animasi per jenis hit. Warna dibaca dari config (bisa diubah di menu). */
public enum Tier {
	//     skala  puncak  naik    durasi  drift  outline
	//     dasar  pop     (blok)  (ms)    (blok)
	NORMAL(1.00f, 1.35f, 1.20f, 1200L, 0.35f, false),
	HEAVY (1.25f, 1.65f, 1.45f, 1350L, 0.50f, true),
	CRIT  (1.55f, 2.10f, 1.70f, 1500L, 0.70f, true);

	public final float baseScale;
	public final float popPeak;
	public final float riseBlocks;
	public final long lifeMs;
	public final float driftBlocks;
	public final boolean outline;

	Tier(float baseScale, float popPeak, float riseBlocks, long lifeMs, float driftBlocks, boolean outline) {
		this.baseScale = baseScale;
		this.popPeak = popPeak;
		this.riseBlocks = riseBlocks;
		this.lifeMs = lifeMs;
		this.driftBlocks = driftBlocks;
		this.outline = outline;
	}

	public int rgb() {
		CombatConfig c = CombatConfig.get();
		return switch (this) {
			case NORMAL -> c.colorNormal;
			case HEAVY -> c.colorHeavy;
			case CRIT -> c.colorCrit;
		};
	}
}
