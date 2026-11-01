package dev.combatfx;

/** Batas internal (bukan pengaturan user). Pengaturan user ada di CombatConfig. */
public final class Config {
	private Config() {}

	public static final int MAX_INDICATORS = 64;
	public static final int PENDING_TIMEOUT_TICKS = 10;
	public static final int MAX_PENDING = 16;
}
