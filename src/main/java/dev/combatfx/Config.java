package dev.combatfx;

/** Semua angka tuning ada di sini. Ubah lalu build ulang. */
public final class Config {
	private Config() {}

	// Combo
	public static final long COMBO_TIMEOUT_MS = 1500L;

	// Floating text
	public static final long NORMAL_LIFETIME_MS = 1200L;
	public static final long CRIT_LIFETIME_MS = 1500L;
	public static final float NORMAL_RISE_BLOCKS = 1.2f;
	public static final float CRIT_RISE_BLOCKS = 1.7f;
	public static final int MAX_INDICATORS = 64;

	// Deteksi damage (client tidak tahu damage asli, jadi dibaca dari selisih HP target)
	public static final int PENDING_TIMEOUT_TICKS = 10;
	public static final int MAX_PENDING = 16;

	// Warna RGB (alpha ditambahkan saat render)
	public static final int COLOR_NORMAL = 0xFF3B3B;  // merah terang
	public static final int COLOR_CRIT = 0xFFB300;    // kuning keemasan / oranye
}
