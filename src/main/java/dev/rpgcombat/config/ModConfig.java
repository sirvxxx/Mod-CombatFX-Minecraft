package dev.rpgcombat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.combatfx.CombatConfig;
import dev.rpgcombat.extras.ExtrasConfig;
import dev.rpgcombat.spatial.SpatialSettings;
import dev.rpgcombat.ui.UiConfig;
import dev.rpghud.HudConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Satu file config untuk semua fitur: .minecraft/config/rpgcombat.json
 * <pre>{ "hud": {...}, "combat": {...} }</pre>
 * Kalau file baru belum ada tapi custom_rpg_hud.json lama ada, pengaturan HUD lama diimpor.
 */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path DIR = FabricLoader.getInstance().getConfigDir();
	private static final Path FILE = DIR.resolve("rpgcombat.json");
	private static final Path LEGACY_HUD = DIR.resolve("custom_rpg_hud.json");

	private static ModConfig instance = new ModConfig();

	public int configVersion = 1;
	public HudConfig hud = new HudConfig();
	public CombatConfig combat = new CombatConfig();
	public ExtrasConfig extras = new ExtrasConfig();
	/** Rotasi 2.5D + kedalaman per elemen: face, bars, info, combo, pickup, mining, hotbar, rpg_hud. */
	public Map<String, SpatialSettings> spatial = new HashMap<>();
	public UiConfig ui = new UiConfig();

	private ModConfig() {}

	public static ModConfig get() {
		return instance;
	}

	public static void load() {
		try {
			if (Files.exists(FILE)) {
				ModConfig c = GSON.fromJson(Files.readString(FILE), ModConfig.class);
				if (c != null) instance = c;
			} else if (Files.exists(LEGACY_HUD)) {
				HudConfig h = GSON.fromJson(Files.readString(LEGACY_HUD), HudConfig.class);
				if (h != null) instance.hud = h;
			}
		} catch (Exception e) {
			System.err.println("[RPG Combat] Gagal baca config, pakai default: " + e);
		}
		if (instance.hud == null) instance.hud = new HudConfig();
		if (instance.combat == null) instance.combat = new CombatConfig();
		if (instance.extras == null) instance.extras = new ExtrasConfig();
		if (instance.spatial == null) instance.spatial = new HashMap<>();
		if (instance.ui == null) instance.ui = new UiConfig();
		instance.ui.sanitize();
		instance.hud.sanitize();
		instance.combat.sanitize();
		instance.extras.sanitize();
		instance.spatial.values().removeIf(v -> v == null);
		for (SpatialSettings v : instance.spatial.values()) v.sanitize();
		save();
	}

	public static void save() {
		try {
			Files.writeString(FILE, GSON.toJson(instance));
		} catch (Exception e) {
			System.err.println("[RPG Combat] Gagal simpan config: " + e);
		}
	}

	public static void resetAll() {
		instance.hud = new HudConfig();
		instance.combat = new CombatConfig();
		instance.extras = new ExtrasConfig();
		instance.spatial = new HashMap<>();
		instance.ui = new UiConfig();
		save();
	}
}
