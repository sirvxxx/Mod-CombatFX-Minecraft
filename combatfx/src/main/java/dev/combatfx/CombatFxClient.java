package dev.combatfx;

import dev.combatfx.combat.HitTracker;
import dev.combatfx.render.CombatHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.resources.Identifier;

public final class CombatFxClient implements ClientModInitializer {
	public static final String MOD_ID = "combatfx";

	@Override
	public void onInitializeClient() {
		// 1) Player menyerang entity -> catat sebagai "pending hit"
		AttackEntityCallback.EVENT.register(HitTracker::onAttack);

		// 2) Tiap tick: konfirmasi hit lewat selisih HP, spawn angka, update combo
		ClientTickEvents.END_CLIENT_TICK.register(HitTracker::onClientTick);

		// 3) Render angka melayang + combo HUD (sesudah crosshair)
		HudElementRegistry.attachElementAfter(
				VanillaHudElements.CROSSHAIR,
				Identifier.fromNamespaceAndPath(MOD_ID, "combat_fx"),
				CombatHud::extract
		);
	}
}
