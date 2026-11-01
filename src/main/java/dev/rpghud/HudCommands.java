package dev.rpghud;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.rpgcombat.RpgCombatClient;
import java.util.Locale;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public final class HudCommands {
    private HudCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal((String)"rpghud").then(ClientCommands.literal((String)"gui").executes(ctx -> {
            RpgCombatClient.requestOpen();
            return 1;
        }))).then(ClientCommands.literal((String)"edit").executes(ctx -> {
            RpgCombatClient.requestEdit();
            return 1;
        }))).then(ClientCommands.literal((String)"x").then(ClientCommands.argument((String)"value", (ArgumentType)IntegerArgumentType.integer((int)0, (int)4000)).executes(ctx -> {
            HudConfig.get().x = IntegerArgumentType.getInteger((CommandContext)ctx, (String)"value");
            HudConfig.save();
            ((FabricClientCommandSource)ctx.getSource()).sendFeedback((Component)Component.translatable((String)"custom_rpg_hud.cmd.x", (Object[])new Object[]{HudConfig.get().x}));
            return 1;
        })))).then(ClientCommands.literal((String)"y").then(ClientCommands.argument((String)"value", (ArgumentType)IntegerArgumentType.integer((int)0, (int)4000)).executes(ctx -> {
            HudConfig.get().y = IntegerArgumentType.getInteger((CommandContext)ctx, (String)"value");
            HudConfig.save();
            ((FabricClientCommandSource)ctx.getSource()).sendFeedback((Component)Component.translatable((String)"custom_rpg_hud.cmd.y", (Object[])new Object[]{HudConfig.get().y}));
            return 1;
        })))).then(ClientCommands.literal((String)"scale").then(ClientCommands.argument((String)"value", (ArgumentType)FloatArgumentType.floatArg((float)0.5f, (float)3.0f)).executes(ctx -> {
            HudConfig.get().scale = FloatArgumentType.getFloat((CommandContext)ctx, (String)"value");
            HudConfig.save();
            ((FabricClientCommandSource)ctx.getSource()).sendFeedback((Component)Component.translatable((String)"custom_rpg_hud.cmd.scale", (Object[])new Object[]{Float.valueOf(HudConfig.get().scale)}));
            return 1;
        })))).then(ClientCommands.literal((String)"type").then(ClientCommands.argument((String)"name", (ArgumentType)StringArgumentType.word()).executes(ctx -> {
            String n = StringArgumentType.getString((CommandContext)ctx, (String)"name").toLowerCase(Locale.ROOT);
            boolean bar = n.equals("bar");
            if (!bar && !n.equals("circle")) {
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((Component)Component.translatable((String)"custom_rpg_hud.cmd.type_unknown"));
                return 0;
            }
            HudConfig.get().layoutType = bar ? 1 : 0;
            HudConfig.save();
            ((FabricClientCommandSource)ctx.getSource()).sendFeedback((Component)Component.translatable((String)"custom_rpg_hud.cmd.type", (Object[])new Object[]{Component.translatable((String)(bar ? "custom_rpg_hud.layout.bar" : "custom_rpg_hud.layout.circle"))}));
            return 1;
        })))).then(ClientCommands.literal((String)"theme").then(ClientCommands.argument((String)"id", (ArgumentType)StringArgumentType.word()).executes(ctx -> {
            String id = StringArgumentType.getString((CommandContext)ctx, (String)"id").toLowerCase(Locale.ROOT);
            for (HudTheme t : HudTheme.ALL) {
                if (!t.id.equals(id)) continue;
                HudConfig.get().theme = t.id;
                HudConfig.save();
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((Component)Component.translatable((String)"custom_rpg_hud.cmd.theme", (Object[])new Object[]{Component.translatable((String)t.translationKey())}));
                return 1;
            }
            StringBuilder all = new StringBuilder();
            for (HudTheme t : HudTheme.ALL) {
                all.append(all.length() == 0 ? "" : ", ").append(t.id);
            }
            ((FabricClientCommandSource)ctx.getSource()).sendFeedback((Component)Component.translatable((String)"custom_rpg_hud.cmd.theme_unknown", (Object[])new Object[]{all.toString()}));
            return 0;
        })))).then(ClientCommands.literal((String)"hidevanilla").then(ClientCommands.argument((String)"value", (ArgumentType)BoolArgumentType.bool()).executes(ctx -> {
            HudConfig.get().hideVanilla = BoolArgumentType.getBool((CommandContext)ctx, (String)"value");
            HudConfig.save();
            ((FabricClientCommandSource)ctx.getSource()).sendFeedback((Component)Component.translatable((String)"custom_rpg_hud.cmd.hide_vanilla", (Object[])new Object[]{HudConfig.get().hideVanilla}));
            return 1;
        })))).then(ClientCommands.literal((String)"toggle").executes(ctx -> {
            HudConfig.get().enabled = !HudConfig.get().enabled;
            HudConfig.save();
            ((FabricClientCommandSource)ctx.getSource()).sendFeedback((Component)Component.translatable((String)(HudConfig.get().enabled ? "custom_rpg_hud.cmd.on" : "custom_rpg_hud.cmd.off")));
            return 1;
        }))).then(ClientCommands.literal((String)"reset").executes(ctx -> {
            HudConfig.reset();
            ((FabricClientCommandSource)ctx.getSource()).sendFeedback((Component)Component.translatable((String)"custom_rpg_hud.cmd.reset"));
            return 1;
        }))));
    }
}

