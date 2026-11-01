package dev.combatfx.render;

/** Tuning visual per jenis hit. Ubah angkanya untuk mengatur "rasa" animasinya. */
public enum Tier {
	//     warna RGB  skala  puncak  naik    durasi  drift  outline
	//                dasar  pop     (blok)  (ms)    (blok)
	NORMAL(0xFF3B3B, 1.00f, 1.35f, 1.20f, 1200L, 0.35f, false), // merah standar
	HEAVY (0xFF8A1F, 1.25f, 1.65f, 1.45f, 1350L, 0.50f, true),  // oranye menyala
	CRIT  (0xFFC400, 1.55f, 2.10f, 1.70f, 1500L, 0.70f, true);  // kuning keemasan

	public final int rgb;
	public final float baseScale;
	public final float popPeak;
	public final float riseBlocks;
	public final long lifeMs;
	public final float driftBlocks;
	public final boolean outline;

	Tier(int rgb, float baseScale, float popPeak, float riseBlocks, long lifeMs,
			float driftBlocks, boolean outline) {
		this.rgb = rgb;
		this.baseScale = baseScale;
		this.popPeak = popPeak;
		this.riseBlocks = riseBlocks;
		this.lifeMs = lifeMs;
		this.driftBlocks = driftBlocks;
		this.outline = outline;
	}
}
