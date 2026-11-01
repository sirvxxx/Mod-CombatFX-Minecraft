package dev.combatfx;

/** Angka tuning global. Tuning per-tier (warna, ukuran, durasi) ada di render/Tier.java. */
public final class Config {
	private Config() {}

	// Combo
	public static final long COMBO_TIMEOUT_MS = 1500L;

	// Damage >= nilai ini (tanpa crit) dianggap "Heavy". 8 HP = 4 hati.
	public static final float HEAVY_DAMAGE = 8.0f;

	public static final int MAX_INDICATORS = 64;

	// Deteksi damage (client tidak tahu damage asli, dibaca dari selisih HP target)
	public static final int PENDING_TIMEOUT_TICKS = 10;
	public static final int MAX_PENDING = 16;
}
