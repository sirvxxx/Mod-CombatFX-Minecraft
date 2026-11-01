package dev.rpgcombat;

import dev.combatfx.CombatConfig;
import dev.rpgcombat.config.ModConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;

/** /combatfx gui | toggle | reset */
public final class CombatCommands {
	private CombatCommands() {}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommands.literal("combatfx")
						.then(ClientCommands.literal("gui").executes(ctx -> {
							RpgCombatClient.requestOpen();
							return 1;
						}))
						.then(ClientCommands.literal("toggle").executes(ctx -> {
							CombatConfig c = CombatConfig.get();
							c.enabled = !c.enabled;
							ModConfig.save();
							ctx.getSource().sendFeedback(Component.translatable(
									c.enabled ? "rpgcombat.cmd.on" : "rpgcombat.cmd.off"));
							return 1;
						}))
						.then(ClientCommands.literal("reset").executes(ctx -> {
							CombatConfig.reset();
							ctx.getSource().sendFeedback(Component.translatable("rpgcombat.cmd.reset"));
							return 1;
						}))));
	}
}
