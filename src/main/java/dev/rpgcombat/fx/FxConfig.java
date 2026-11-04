package dev.rpgcombat.fx;

import dev.rpgcombat.config.ModConfig;

/** Seksi "fx" di rpgcombat.json: trail, efek hit, efek kill. Default mati (opt-in). */
public class FxConfig {
	public boolean trailEnabled = false;
	public String trailStyle = "sparkle";
	public float trailDensity = 1.0f;       // 0.25 - 2.0
	public boolean trailSprintOnly = false;
	public boolean trailFirstPerson = false; // tampilkan trail juga di kamera orang pertama

	public boolean hitEnabled = false;
	public String hitStyle = "spark";
	public float hitIntensity = 1.0f;       // 0.25 - 2.0
	public boolean hitCritOnly = false;

	public boolean killEnabled = false;
	public String killStyle = "burst";
	public float killIntensity = 1.0f;      // 0.25 - 2.0

	public static FxConfig get() {
		return ModConfig.get().fx;
	}

	public void sanitize() {
		trailDensity = Math.max(0.25f, Math.min(2.0f, trailDensity));
		hitIntensity = Math.max(0.25f, Math.min(2.0f, hitIntensity));
		killIntensity = Math.max(0.25f, Math.min(2.0f, killIntensity));
		if (trailStyle == null) trailStyle = "sparkle";
		if (hitStyle == null) hitStyle = "spark";
		if (killStyle == null) killStyle = "burst";
	}
}
