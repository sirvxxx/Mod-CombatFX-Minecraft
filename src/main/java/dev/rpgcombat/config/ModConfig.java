package dev.rpgcombat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.combatfx.CombatConfig;
import dev.rpghud.HudConfig;
import java.nio.file.Files;
import java.nio.file.Path;
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
		instance.hud.sanitize();
		instance.combat.sanitize();
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
		save();
	}
}
