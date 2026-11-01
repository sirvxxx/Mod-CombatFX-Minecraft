package dev.rpgcombat.extras;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Penghubung: satu callback tick + satu elemen HUD untuk kedua fitur extras. */
public final class Extras {
	private Extras() {}

	private static long lastNs = System.nanoTime();

	public static void onTick(Minecraft mc) {
		BreakProgress.tick(mc);
		PickupDetector.tick(mc);
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		long nowNs = System.nanoTime();
		float dt = Math.max(0f, Math.min(0.1f, (nowNs - lastNs) / 1.0e9f));
		lastNs = nowNs;

		if (mc.player == null || mc.level == null) return;
		ExtrasConfig cfg = ExtrasConfig.get();

		if (cfg.breakEnabled) BreakProgress.render(g, mc, cfg, dt);
		if (cfg.pickupEnabled) PickupManager.render(g, mc, cfg, dt);
	}
}
