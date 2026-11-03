package dev.rpgcombat.ui;

import dev.rpgcombat.config.ModConfig;
import java.util.LinkedHashSet;
import java.util.Set;

/** Seksi "ui" di rpgcombat.json. */
public class UiConfig {
	public Set<String> favorites = new LinkedHashSet<>();
	public boolean sounds = true;
	public float soundVolume = 0.6f;     // 0.0 - 1.0
	public String lastCategory = "COMBAT";

	public static UiConfig get() {
		return ModConfig.get().ui;
	}

	public void sanitize() {
		if (favorites == null) favorites = new LinkedHashSet<>();
		soundVolume = Math.max(0f, Math.min(1f, soundVolume));
		if (lastCategory == null) lastCategory = "COMBAT";
	}
}
