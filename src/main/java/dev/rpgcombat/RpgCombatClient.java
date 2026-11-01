package dev.rpgcombat;

import com.mojang.blaze3d.platform.InputConstants;
import dev.combatfx.combat.HitTracker;
import dev.combatfx.render.CombatHud;
import dev.rpgcombat.config.ModConfig;
import dev.rpgcombat.extras.Extras;
import dev.rpgcombat.extras.ExtrasConfig;
import dev.rpgcombat.extras.HotbarName;
import dev.rpgcombat.gui.HudCustomizerScreen;
import dev.rpgcombat.theme.PackTheme;
import dev.rpgcombat.gui.SettingsScreen;
import dev.rpghud.HudCommands;
import dev.rpghud.HudConfig;
import dev.rpghud.HudRenderer;
import dev.rpghud.McCompat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** Satu entrypoint untuk dua modul: RPG HUD (dev.rpghud) dan CombatFX (dev.combatfx). */
public final class RpgCombatClient implements ClientModInitializer {
	public static final String MOD_ID = "rpgcombat";

	private static KeyMapping openKey;
	private static KeyMapping editKey;
	private static volatile boolean openRequested;
	private static volatile boolean editRequested;

	public static void requestOpen() {
		openRequested = true;
	}

	public static void requestEdit() {
		editRequested = true;
	}

	@Override
	public void onInitializeClient() {
		// 0) satu config untuk semuanya
		ModConfig.load();

		// 1) modul RPG HUD
		hideVanilla(VanillaHudElements.HEALTH_BAR);
		hideVanilla(VanillaHudElements.ARMOR_BAR);
		hideVanilla(VanillaHudElements.FOOD_BAR);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(MOD_ID, "rpg_hud"), HudRenderer::render);

		// 2) modul CombatFX
		AttackEntityCallback.EVENT.register(HitTracker::onAttack);
		ClientTickEvents.END_CLIENT_TICK.register(HitTracker::onClientTick);
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR,
				Identifier.fromNamespaceAndPath(MOD_ID, "combat_fx"), CombatHud::extract);

		// 3) modul Extras: bar hancur blok + popup item pickup
		ClientTickEvents.END_CLIENT_TICK.register(Extras::onTick);
		hideVanillaHeldItemTooltip();
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR,
				Identifier.fromNamespaceAndPath(MOD_ID, "extras"), Extras::extract);

		// 4) tombol pintas: H = menu master, J = edit mode HUD
		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		openKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.rpgcombat.open_settings", InputConstants.Type.KEYSYM, 72, category)); // H
		editKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.rpgcombat.edit_mode", InputConstants.Type.KEYSYM, 74, category));     // J

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			PackTheme.tick(client); // tema otomatis dari resource pack
			boolean open = false;
			boolean edit = false;
			while (openKey.consumeClick()) open = true;
			while (editKey.consumeClick()) edit = true;
			if (openRequested) {
				openRequested = false;
				open = true;
			}
			if (editRequested) {
				editRequested = false;
				edit = true;
			}
			if (client.player == null) return;
			if (edit) {
				McCompat.openScreen(new HudCustomizerScreen());
			} else if (open) {
				McCompat.openScreen(new SettingsScreen());
			}
		});

		// 5) command: /rpghud ... dan /combatfx ...
		HudCommands.register();
		CombatCommands.register();
	}

	/**
	 * Tooltip nama item vanilla disembunyikan supaya popup nama kita tidak dobel.
	 * Kalau elemennya tidak ditemukan, popup kita dimatikan otomatis dan tooltip vanilla tetap dipakai.
	 */
	private static void hideVanillaHeldItemTooltip() {
		try {
			HudElementRegistry.replaceElement(Identifier.fromNamespaceAndPath("minecraft", "held_item_tooltip"),
					original -> (graphics, delta) -> {
						if (!ExtrasConfig.get().hotbarNameEnabled || !HotbarName.available) {
							original.extractRenderState(graphics, delta);
						}
					});
		} catch (Throwable t) {
			HotbarName.available = false;
		}
	}

	private static void hideVanilla(Identifier id) {
		HudElementRegistry.replaceElement(id, original -> (graphics, delta) -> {
			HudConfig c = HudConfig.get();
			if (!c.enabled || !c.hideVanilla) {
				original.extractRenderState(graphics, delta);
			}
		});
	}
}
