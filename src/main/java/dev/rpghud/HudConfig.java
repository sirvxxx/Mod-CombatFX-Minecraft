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

	// --- Layout bar: tiap bagian bisa digeser (offset dari posisi bawaan, satuan layout), diskalakan, dan diberi opacity ---
	public int faceDx = 0;
	public int faceDy = 0;
	public float faceScale = 1.0f;
	public float faceOpacity = 1.0f;
	public int barsDx = 0;
	public int barsDy = 0;
	public float barsScale = 1.0f;
	public float barsOpacity = 1.0f;   // layout lingkaran: opacity seluruh HUD
	public int infoDx = 0;
	public int infoDy = 0;
	public float infoScale = 1.0f;
	public float infoOpacity = 1.0f;

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

	public void resetSections() {
		faceDx = faceDy = barsDx = barsDy = infoDx = infoDy = 0;
		faceScale = barsScale = infoScale = 1.0f;
	}

	public void sanitize() {
		theme = HudTheme.byId(theme).id;
		if (layoutType != TYPE_BAR) layoutType = TYPE_CIRCLE;
		scale = Math.max(0.5f, Math.min(3.0f, scale));
		faceScale = Math.max(0.5f, Math.min(3.0f, faceScale));
		barsScale = Math.max(0.5f, Math.min(3.0f, barsScale));
		infoScale = Math.max(0.5f, Math.min(3.0f, infoScale));
		faceOpacity = Math.max(0.1f, Math.min(1.0f, faceOpacity));
		barsOpacity = Math.max(0.1f, Math.min(1.0f, barsOpacity));
		infoOpacity = Math.max(0.1f, Math.min(1.0f, infoOpacity));
		faceDx = Math.max(-2000, Math.min(2000, faceDx));
		faceDy = Math.max(-2000, Math.min(2000, faceDy));
		barsDx = Math.max(-2000, Math.min(2000, barsDx));
		barsDy = Math.max(-2000, Math.min(2000, barsDy));
		infoDx = Math.max(-2000, Math.min(2000, infoDx));
		infoDy = Math.max(-2000, Math.min(2000, infoDy));
		x = Math.max(0, x);
		y = Math.max(0, y);
	}
}
