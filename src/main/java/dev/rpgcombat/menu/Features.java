package dev.rpgcombat.menu;

import dev.combatfx.CombatConfig;
import dev.rpgcombat.extras.ExtrasConfig;
import dev.rpgcombat.fx.FxConfig;
import dev.rpgcombat.fx.FxStyles;
import dev.rpgcombat.preview.scenes.HitScene;
import dev.rpgcombat.preview.scenes.KillScene;
import dev.rpgcombat.preview.scenes.TrailScene;
import dev.rpgcombat.gui.HudCustomizerScreen;
import dev.rpgcombat.gui.SettingsScreen;
import dev.rpgcombat.ui.Icon;
import dev.rpgcombat.ui.UiConfig;
import dev.rpgcombat.ui.row.ButtonRow;
import dev.rpgcombat.ui.row.ChoiceRow;
import dev.rpgcombat.ui.row.ColorRow;
import dev.rpgcombat.ui.row.LabelRow;
import dev.rpgcombat.ui.row.StyleGridRow;
import dev.rpgcombat.ui.row.Row;
import dev.rpgcombat.ui.row.SliderRow;
import dev.rpgcombat.ui.row.ToggleRow;
import dev.rpghud.HudConfig;
import dev.rpghud.HudTheme;
import dev.rpghud.McCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Semua halaman menu didaftarkan di sini. Menambah fitur baru = menambah satu Feature + daftar Row;
 * tidak ada kode layout.
 */
public final class Features {
	private Features() {}

	public static void register() {
		FeatureRegistry.ALL.clear();
		combat();
		cosmetics();
		hud();
		qol();
	}

	// ------------------------------------------------------------------ helpers
	private static SliderRow pct(String key, DoubleSupplier get01, DoubleConsumer set01) {
		return new SliderRow(key, 10, 100, 5, "%.0f%%", () -> get01.getAsDouble() * 100.0, v -> set01.accept(v / 100.0));
	}

	private static void editLayout() {
		McCompat.openScreen(new HudCustomizerScreen());
	}

	private static void add(Feature f) {
		FeatureRegistry.register(f);
	}

	// ------------------------------------------------------------------ COMBAT
	private static void combat() {
		add(new Feature("trails", Category.COMBAT, "rpgcombat.feat.trails", "rpgcombat.feat.trails.sub", Icon.SPARK,
				() -> FxConfig.get().trailEnabled, v -> FxConfig.get().trailEnabled = v,
				() -> List.<Row>of(
						new StyleGridRow("rpgcombat.fx.style", FxStyles.TRAILS, () -> FxConfig.get().trailStyle, id -> FxConfig.get().trailStyle = id),
						new SliderRow("rpgcombat.fx.density", 25, 200, 5, "%.0f%%",
								() -> FxConfig.get().trailDensity * 100.0, v -> FxConfig.get().trailDensity = (float) (v / 100.0)),
						new ChoiceRow("rpgcombat.fx.mode", List.of("rpgcombat.fx.mode.move", "rpgcombat.fx.mode.sprint"),
								() -> FxConfig.get().trailSprintOnly ? 1 : 0, i -> FxConfig.get().trailSprintOnly = i == 1),
						new ToggleRow("rpgcombat.fx.first_person", () -> FxConfig.get().trailFirstPerson, v -> FxConfig.get().trailFirstPerson = v)),
				TrailScene::new));

		add(new Feature("hits", Category.COMBAT, "rpgcombat.feat.hits", "rpgcombat.feat.hits.sub", Icon.BOLT,
				() -> FxConfig.get().hitEnabled, v -> FxConfig.get().hitEnabled = v,
				() -> List.<Row>of(
						new StyleGridRow("rpgcombat.fx.style", FxStyles.HITS, () -> FxConfig.get().hitStyle, id -> FxConfig.get().hitStyle = id),
						new SliderRow("rpgcombat.fx.intensity", 25, 200, 5, "%.0f%%",
								() -> FxConfig.get().hitIntensity * 100.0, v -> FxConfig.get().hitIntensity = (float) (v / 100.0)),
						new ToggleRow("rpgcombat.fx.crit_only", () -> FxConfig.get().hitCritOnly, v -> FxConfig.get().hitCritOnly = v)),
				HitScene::new));

		add(new Feature("kills", Category.COMBAT, "rpgcombat.feat.kills", "rpgcombat.feat.kills.sub", Icon.SWORD,
				() -> FxConfig.get().killEnabled, v -> FxConfig.get().killEnabled = v,
				() -> List.<Row>of(
						new StyleGridRow("rpgcombat.fx.style", FxStyles.KILLS, () -> FxConfig.get().killStyle, id -> FxConfig.get().killStyle = id),
						new SliderRow("rpgcombat.fx.intensity", 25, 200, 5, "%.0f%%",
								() -> FxConfig.get().killIntensity * 100.0, v -> FxConfig.get().killIntensity = (float) (v / 100.0))),
				KillScene::new));

		add(new Feature("dmg", Category.COMBAT, "rpgcombat.feat.dmg", "rpgcombat.feat.dmg.sub", Icon.SWORD,
				() -> CombatConfig.get().showDamageNumbers, v -> CombatConfig.get().showDamageNumbers = v,
				() -> List.<Row>of(
						new ToggleRow("rpgcombat.opt.outline", () -> CombatConfig.get().numberOutline, v -> CombatConfig.get().numberOutline = v),
						new SliderRow("rpgcombat.opt.num_size", 0.5, 2.0, 0.05, "%.2f",
								() -> CombatConfig.get().numberScale, v -> CombatConfig.get().numberScale = (float) v),
						pct("rpgcombat.op.numbers", () -> CombatConfig.get().numberOpacity, v -> CombatConfig.get().numberOpacity = (float) v),
						new SliderRow("rpgcombat.opt.heavy", 2, 40, 1, "%.0f HP",
								() -> CombatConfig.get().heavyDamage, v -> CombatConfig.get().heavyDamage = (float) v),
						new LabelRow("rpgcombat.section.combat_colors", true),
						new ColorRow("rpgcombat.color.normal", () -> CombatConfig.get().colorNormal, v -> CombatConfig.get().colorNormal = v),
						new ColorRow("rpgcombat.color.heavy", () -> CombatConfig.get().colorHeavy, v -> CombatConfig.get().colorHeavy = v),
						new ColorRow("rpgcombat.color.crit", () -> CombatConfig.get().colorCrit, v -> CombatConfig.get().colorCrit = v))));

		add(new Feature("combo", Category.COMBAT, "rpgcombat.feat.combo", "rpgcombat.feat.combo.sub", Icon.BOLT,
				() -> CombatConfig.get().showCombo, v -> CombatConfig.get().showCombo = v,
				() -> List.<Row>of(
						new ToggleRow("rpgcombat.opt.combo_bar", () -> CombatConfig.get().showComboBar, v -> CombatConfig.get().showComboBar = v),
						new ToggleRow("rpgcombat.opt.pulse", () -> CombatConfig.get().comboPulse, v -> CombatConfig.get().comboPulse = v),
						new SliderRow("rpgcombat.opt.combo_size", 0.5, 2.0, 0.05, "%.2f",
								() -> CombatConfig.get().comboScale, v -> CombatConfig.get().comboScale = (float) v),
						new SliderRow("rpgcombat.opt.timeout", 500, 5000, 100, "%.0f ms",
								() -> CombatConfig.get().comboTimeoutMs, v -> CombatConfig.get().comboTimeoutMs = (int) Math.round(v)),
						pct("rpgcombat.op.combo", () -> CombatConfig.get().comboOpacity, v -> CombatConfig.get().comboOpacity = (float) v),
						new ButtonRow("rpgcombat.btn.open_editor", Features::editLayout))));

		add(new Feature("combat_master", Category.COMBAT, "rpgcombat.feat.master", "rpgcombat.feat.master.sub", Icon.SPARK,
				() -> CombatConfig.get().enabled, v -> CombatConfig.get().enabled = v,
				() -> List.<Row>of(
						new ToggleRow("rpgcombat.opt.combat_enable", () -> CombatConfig.get().enabled, v -> CombatConfig.get().enabled = v),
						new LabelRow("rpgcombat.menu.master_note", false),
						new ButtonRow("rpgcombat.btn.reset_combat", () -> CombatConfig.reset()))));
	}

	// ------------------------------------------------------------------ COSMETICS (fondasi; isi fitur menyusul)
	private static void cosmetics() {
		add(soon("capes", "rpgcombat.feat.capes", "rpgcombat.feat.capes.sub", Icon.STAR));
		add(soon("wings", "rpgcombat.feat.wings", "rpgcombat.feat.wings.sub", Icon.SPARK));
		add(soon("auras", "rpgcombat.feat.auras", "rpgcombat.feat.auras.sub", Icon.BOLT));
	}

	private static Feature soon(String id, String title, String sub, Icon icon) {
		return new Feature(id, Category.COSMETICS, title, sub, icon, null, null,
				() -> List.<Row>of(new LabelRow("rpgcombat.menu.soon", false)));
	}

	// ------------------------------------------------------------------ HUD
	private static void hud() {
		List<String> themeKeys = new ArrayList<>();
		for (HudTheme t : HudTheme.ALL) themeKeys.add(t.translationKey());

		add(new Feature("hud_layout", Category.HUD, "rpgcombat.feat.hud_layout", "rpgcombat.feat.hud_layout.sub", Icon.HUD,
				() -> HudConfig.get().enabled, v -> HudConfig.get().enabled = v,
				() -> List.<Row>of(
						new ToggleRow("rpgcombat.opt.hide_vanilla", () -> HudConfig.get().hideVanilla, v -> HudConfig.get().hideVanilla = v),
						new ChoiceRow("rpgcombat.menu.layout", List.of("custom_rpg_hud.layout.bar", "custom_rpg_hud.layout.circle"),
								() -> HudConfig.get().layoutType == HudConfig.TYPE_BAR ? 0 : 1,
								i -> HudConfig.get().layoutType = i == 0 ? HudConfig.TYPE_BAR : HudConfig.TYPE_CIRCLE),
						new ChoiceRow("rpgcombat.menu.theme", themeKeys,
								() -> {
									for (int i = 0; i < HudTheme.ALL.length; i++) {
										if (HudTheme.ALL[i].id.equals(HudConfig.get().theme)) return i;
									}
									return 0;
								},
								i -> HudConfig.get().theme = HudTheme.ALL[Math.max(0, Math.min(HudTheme.ALL.length - 1, i))].id),
						new SliderRow("rpgcombat.opt.scale", 0.5, 3.0, 0.05, "%.2f",
								() -> HudConfig.get().scale, v -> HudConfig.get().scale = (float) v),
						new ButtonRow("rpgcombat.btn.open_editor", Features::editLayout),
						new ButtonRow("rpgcombat.btn.reset_hud", () -> HudConfig.reset()))));

		add(new Feature("hud_elements", Category.HUD, "rpgcombat.feat.hud_elements", "rpgcombat.feat.hud_elements.sub", Icon.GEAR,
				null, null,
				() -> List.<Row>of(
						new ToggleRow("rpgcombat.opt.face", () -> HudConfig.get().showFace, v -> HudConfig.get().showFace = v),
						new ToggleRow("rpgcombat.opt.level", () -> HudConfig.get().showLevel, v -> HudConfig.get().showLevel = v),
						new ToggleRow("rpgcombat.opt.name", () -> HudConfig.get().showName, v -> HudConfig.get().showName = v),
						new ToggleRow("rpgcombat.opt.health", () -> HudConfig.get().showHealth, v -> HudConfig.get().showHealth = v),
						new ToggleRow("rpgcombat.opt.hunger", () -> HudConfig.get().showHunger, v -> HudConfig.get().showHunger = v),
						new ToggleRow("rpgcombat.opt.armor", () -> HudConfig.get().showArmor, v -> HudConfig.get().showArmor = v),
						new ToggleRow("rpgcombat.opt.coords", () -> HudConfig.get().showCoords, v -> HudConfig.get().showCoords = v),
						new ToggleRow("rpgcombat.opt.fps", () -> HudConfig.get().showFps, v -> HudConfig.get().showFps = v))));

		add(new Feature("hud_opacity", Category.HUD, "rpgcombat.feat.hud_opacity", "rpgcombat.feat.hud_opacity.sub", Icon.SPARK,
				null, null,
				() -> List.<Row>of(
						pct("rpgcombat.op.face", () -> HudConfig.get().faceOpacity, v -> HudConfig.get().faceOpacity = (float) v),
						pct("rpgcombat.op.bars", () -> HudConfig.get().barsOpacity, v -> HudConfig.get().barsOpacity = (float) v),
						pct("rpgcombat.op.info", () -> HudConfig.get().infoOpacity, v -> HudConfig.get().infoOpacity = (float) v))));
	}

	// ------------------------------------------------------------------ QoL
	private static void qol() {
		add(new Feature("pickup", Category.QOL, "rpgcombat.feat.pickup", "rpgcombat.feat.pickup.sub", Icon.STAR,
				() -> ExtrasConfig.get().pickupEnabled, v -> ExtrasConfig.get().pickupEnabled = v,
				() -> List.<Row>of(
						new ToggleRow("rpgcombat.opt.pickup_enchants", () -> ExtrasConfig.get().pickupEnchants, v -> ExtrasConfig.get().pickupEnchants = v),
						new ToggleRow("rpgcombat.opt.pickup_durability", () -> ExtrasConfig.get().pickupDurability, v -> ExtrasConfig.get().pickupDurability = v),
						new ToggleRow("rpgcombat.opt.pickup_merge", () -> ExtrasConfig.get().pickupMergeSame, v -> ExtrasConfig.get().pickupMergeSame = v),
						new SliderRow("rpgcombat.opt.pickup_time", 1000, 8000, 100, "%.0f ms",
								() -> ExtrasConfig.get().pickupDurationMs, v -> ExtrasConfig.get().pickupDurationMs = (int) Math.round(v)),
						new SliderRow("rpgcombat.opt.pickup_max", 1, 10, 1, "%.0f",
								() -> ExtrasConfig.get().pickupMaxVisible, v -> ExtrasConfig.get().pickupMaxVisible = (int) Math.round(v)),
						new SliderRow("rpgcombat.opt.pickup_scale", 0.6, 1.6, 0.05, "%.2f",
								() -> ExtrasConfig.get().pickupScale, v -> ExtrasConfig.get().pickupScale = (float) v),
						pct("rpgcombat.op.pickup", () -> ExtrasConfig.get().pickupOpacity, v -> ExtrasConfig.get().pickupOpacity = (float) v),
						new ButtonRow("rpgcombat.btn.open_editor", Features::editLayout))));

		add(new Feature("mining", Category.QOL, "rpgcombat.feat.mining", "rpgcombat.feat.mining.sub", Icon.GEAR,
				() -> ExtrasConfig.get().breakEnabled, v -> ExtrasConfig.get().breakEnabled = v,
				() -> List.<Row>of(
						new ChoiceRow("rpgcombat.menu.bar_style", List.of("rpgcombat.style.linear", "rpgcombat.style.circular"),
								() -> ExtrasConfig.get().breakStyle, i -> ExtrasConfig.get().breakStyle = i),
						new ToggleRow("rpgcombat.opt.break_percent", () -> ExtrasConfig.get().breakShowPercent, v -> ExtrasConfig.get().breakShowPercent = v),
						new SliderRow("rpgcombat.opt.break_size", 0.5, 2.0, 0.05, "%.2f",
								() -> ExtrasConfig.get().breakScale, v -> ExtrasConfig.get().breakScale = (float) v),
						pct("rpgcombat.op.mining", () -> ExtrasConfig.get().breakOpacity, v -> ExtrasConfig.get().breakOpacity = (float) v),
						new ButtonRow("rpgcombat.btn.open_editor", Features::editLayout))));

		add(new Feature("hotbar", Category.QOL, "rpgcombat.feat.hotbar", "rpgcombat.feat.hotbar.sub", Icon.HUD,
				() -> ExtrasConfig.get().hotbarNameEnabled, v -> ExtrasConfig.get().hotbarNameEnabled = v,
				() -> List.<Row>of(
						new SliderRow("rpgcombat.opt.pickup_scale", 0.6, 2.0, 0.05, "%.2f",
								() -> ExtrasConfig.get().hotbarNameScale, v -> ExtrasConfig.get().hotbarNameScale = (float) v),
						pct("rpgcombat.op.hotbar", () -> ExtrasConfig.get().hotbarNameOpacity, v -> ExtrasConfig.get().hotbarNameOpacity = (float) v),
						new ButtonRow("rpgcombat.btn.open_editor", Features::editLayout))));

		add(new Feature("menu_prefs", Category.QOL, "rpgcombat.feat.prefs", "rpgcombat.feat.prefs.sub", Icon.SPARK,
				() -> UiConfig.get().sounds, v -> UiConfig.get().sounds = v,
				() -> List.<Row>of(
						new ToggleRow("rpgcombat.menu.sounds", () -> UiConfig.get().sounds, v -> UiConfig.get().sounds = v),
						new SliderRow("rpgcombat.menu.volume", 0, 100, 5, "%.0f%%",
								() -> UiConfig.get().soundVolume * 100.0, v -> UiConfig.get().soundVolume = (float) (v / 100.0)),
						new ButtonRow("rpgcombat.menu.classic", () -> McCompat.openScreen(new SettingsScreen())))));
	}
}
