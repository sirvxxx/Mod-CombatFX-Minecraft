package dev.rpgcombat.ui;

import dev.rpgcombat.config.ModConfig;

/** Menyimpan config paling sering tiap 0,4 detik saat slider digeser, dan paksa simpan saat layar ditutup. */
public final class UiSave {
	private UiSave() {}

	private static boolean dirty;
	private static long last;

	public static void touch() {
		dirty = true;
	}

	public static void flush(boolean force) {
		long now = System.currentTimeMillis();
		if (dirty && (force || now - last > 400L)) {
			ModConfig.save();
			dirty = false;
			last = now;
		}
	}
}
