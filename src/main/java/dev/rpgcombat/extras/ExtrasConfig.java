package dev.rpgcombat.extras;

import dev.rpgcombat.config.ModConfig;

/** Seksi "extras" di rpgcombat.json: bar hancur blok + popup item pickup. */
public class ExtrasConfig {
	// --- Breaking block progress ---
	public boolean breakEnabled = true;
	public int breakStyle = 0;               // 0 = linear, 1 = circular
	public boolean breakShowPercent = true;
	public boolean breakColorByProgress = true;
	public float breakScale = 1.0f;          // 0.5 - 2.0
	public int breakOffsetY = 14;            // jarak dari tengah layar (linear)

	// --- Item pickup popup ---
	public boolean pickupEnabled = true;
	public boolean pickupEnchants = true;
	public boolean pickupDurability = true;
	public boolean pickupMergeSame = true;
	public int pickupDurationMs = 2800;      // 1000 - 8000
	public int pickupMaxVisible = 6;         // 1 - 10
	public float pickupScale = 1.0f;         // 0.6 - 1.6
	public int pickupMarginRight = 10;       // dari tepi kanan layar
	public int pickupOffsetY = 0;            // dari tengah layar

	public static ExtrasConfig get() {
		return ModConfig.get().extras;
	}

	public static void reset() {
		ModConfig.get().extras = new ExtrasConfig();
		ModConfig.save();
	}

	public void sanitize() {
		breakStyle = breakStyle == 1 ? 1 : 0;
		breakScale = Math.max(0.5f, Math.min(2.0f, breakScale));
		breakOffsetY = Math.max(-100, Math.min(120, breakOffsetY));
		pickupDurationMs = Math.max(1000, Math.min(8000, pickupDurationMs));
		pickupMaxVisible = Math.max(1, Math.min(10, pickupMaxVisible));
		pickupScale = Math.max(0.6f, Math.min(1.6f, pickupScale));
		pickupMarginRight = Math.max(0, Math.min(200, pickupMarginRight));
		pickupOffsetY = Math.max(-200, Math.min(200, pickupOffsetY));
	}
}
