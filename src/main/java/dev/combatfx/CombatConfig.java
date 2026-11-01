package dev.combatfx;

import dev.rpgcombat.config.ModConfig;

/** Seksi "combat" di rpgcombat.json. */
public class CombatConfig {
	public static final int DEF_NORMAL = 0xFF3B3B;
	public static final int DEF_HEAVY = 0xFF8A1F;
	public static final int DEF_CRIT = 0xFFC400;

	public boolean enabled = true;
	public boolean showDamageNumbers = true;
	public boolean showCombo = true;
	public boolean showComboBar = true;
	public boolean numberOutline = true;
	public boolean comboPulse = true;

	public float numberScale = 1.0f;   // 0.5 - 2.0
	public float comboScale = 1.0f;    // 0.5 - 2.0
	public float heavyDamage = 8.0f;   // HP; >= ini (tanpa crit) = Heavy
	public int comboTimeoutMs = 1500;  // 500 - 5000
	public int comboOffsetX = 34;      // dari tengah layar
	public int comboOffsetY = -8;

	public int colorNormal = DEF_NORMAL;
	public int colorHeavy = DEF_HEAVY;
	public int colorCrit = DEF_CRIT;

	public static CombatConfig get() {
		return ModConfig.get().combat;
	}

	public static void reset() {
		ModConfig.get().combat = new CombatConfig();
		ModConfig.save();
	}

	public void resetColors() {
		colorNormal = DEF_NORMAL;
		colorHeavy = DEF_HEAVY;
		colorCrit = DEF_CRIT;
	}

	public void sanitize() {
		numberScale = Math.max(0.5f, Math.min(2.0f, numberScale));
		comboScale = Math.max(0.5f, Math.min(2.0f, comboScale));
		heavyDamage = Math.max(1.0f, Math.min(100.0f, heavyDamage));
		comboTimeoutMs = Math.max(300, Math.min(10000, comboTimeoutMs));
		comboOffsetX = Math.max(-400, Math.min(400, comboOffsetX));
		comboOffsetY = Math.max(-300, Math.min(300, comboOffsetY));
		colorNormal &= 0xFFFFFF;
		colorHeavy &= 0xFFFFFF;
		colorCrit &= 0xFFFFFF;
	}
}
