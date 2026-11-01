package dev.rpghud;

import dev.rpgcombat.config.ModConfig;

/** Seksi "hud" di rpgcombat.json. Hanya data; baca/tulis file ditangani ModConfig. */
public class HudConfig {
	public static final int TYPE_CIRCLE = 0;
	public static final int TYPE_BAR = 1;

	public boolean enabled = true;
	public boolean hideVanilla = true;
	public String theme = "netherite";
	public int layoutType = TYPE_BAR; // horizontal bar
	public int x = 8;
	public int y = 8;
	public float scale = 1.0f;
	public boolean showFace = true;
	public boolean showLevel = true;
	public boolean showName = true;
	public boolean showHealth = true;
	public boolean showHunger = true;
	public boolean showArmor = true;
	public boolean showCoords = true;
	public boolean showFps = true;

	public static HudConfig get() {
		return ModConfig.get().hud;
	}

	public static void save() {
		ModConfig.save();
	}

	public static void reset() {
		ModConfig.get().hud = new HudConfig();
		ModConfig.save();
	}

	public void sanitize() {
		theme = HudTheme.byId(theme).id;
		if (layoutType != TYPE_BAR) layoutType = TYPE_CIRCLE;
		scale = Math.max(0.5f, Math.min(3.0f, scale));
		x = Math.max(0, x);
		y = Math.max(0, y);
	}
}
