package dev.rpghud;

import java.util.Locale;
import dev.rpgcombat.gui.SettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HudEditScreen
extends Screen {
    private static final int BAR_H = 30;
    private boolean dragging;
    private boolean wasDown;
    private int dragOffX;
    private int dragOffY;

    public HudEditScreen() {
        super((Component)Component.translatable((String)"custom_rpg_hud.edit.title"));
    }

    protected void init() {
        HudRenderer.suppressHud = true;
        int bw = 62;
        int gap = 4;
        int total = 6 * bw + 5 * gap;
        int x = (this.width - total) / 2;
        int y = this.height - 30 + 5;
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"custom_rpg_hud.edit.scale_minus"), b -> {
            HudConfig c = HudConfig.get();
            c.scale = Math.max(0.5f, (float)Math.round((c.scale - 0.1f) * 10.0f) / 10.0f);
            this.clampPos();
        }).bounds(x, y, bw, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"custom_rpg_hud.edit.scale_plus"), b -> {
            HudConfig c = HudConfig.get();
            c.scale = Math.min(3.0f, (float)Math.round((c.scale + 0.1f) * 10.0f) / 10.0f);
            this.clampPos();
        }).bounds(x + (bw + gap), y, bw, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"custom_rpg_hud.edit.center"), b -> {
            HudConfig c = HudConfig.get();
            c.x = Math.max(0, (this.width - this.hudW()) / 2);
            c.y = Math.max(0, (this.height - this.hudH()) / 2);
        }).bounds(x + 2 * (bw + gap), y, bw, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"custom_rpg_hud.edit.reset_pos"), b -> {
            HudConfig c = HudConfig.get();
            c.x = 8;
            c.y = 8;
            this.clampPos();
        }).bounds(x + 3 * (bw + gap), y, bw, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"custom_rpg_hud.edit.settings"), b -> {
            HudConfig.save();
            McCompat.openScreen(new SettingsScreen());
        }).bounds(x + 4 * (bw + gap), y, bw, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"custom_rpg_hud.config.done"), b -> this.onClose()).bounds(x + 5 * (bw + gap), y, bw, 20).build());
    }

    private int hudW() {
        return Math.max(1, Math.round((float)HudRenderer.width(HudConfig.get()) * HudConfig.get().scale));
    }

    private int hudH() {
        return Math.max(1, Math.round((float)HudRenderer.height(HudConfig.get()) * HudConfig.get().scale));
    }

    private void clampPos() {
        HudConfig c = HudConfig.get();
        c.x = Math.max(0, Math.min(c.x, Math.max(0, this.width - this.hudW())));
        c.y = Math.max(0, Math.min(c.y, Math.max(0, this.height - this.hudH())));
    }

    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        boolean overBar;
        super.extractRenderState(g, mouseX, mouseY, delta);
        HudConfig c = HudConfig.get();
        int w = this.hudW();
        int h = this.hudH();
        boolean down = Minecraft.getInstance().mouseHandler.isLeftPressed();
        boolean overHud = mouseX >= c.x && mouseX < c.x + w && mouseY >= c.y && mouseY < c.y + h;
        boolean bl = overBar = mouseY >= this.height - 30;
        if (down && !this.wasDown && overHud && !overBar) {
            this.dragging = true;
            this.dragOffX = mouseX - c.x;
            this.dragOffY = mouseY - c.y;
        }
        if (!down) {
            this.dragging = false;
        }
        if (this.dragging) {
            c.x = Math.max(0, Math.min(mouseX - this.dragOffX, Math.max(0, this.width - w)));
            c.y = Math.max(0, Math.min(mouseY - this.dragOffY, Math.max(0, this.height - h)));
        }
        this.wasDown = down;
        HudRenderer.preview(g);
        int frameCol = this.dragging ? -1 : (overHud ? -11413 : -1711287445);
        g.outline(c.x - 2, c.y - 2, w + 4, h + 4, frameCol);
        String title = Component.translatable((String)"custom_rpg_hud.edit.title").getString();
        String hint = Component.translatable((String)"custom_rpg_hud.edit.hint").getString();
        g.text(this.font, title, (this.width - this.font.width(title)) / 2, 8, -11413, true);
        g.text(this.font, hint, (this.width - this.font.width(hint)) / 2, 20, -1, true);
        String info = Component.translatable((String)"custom_rpg_hud.edit.info", (Object[])new Object[]{c.x, c.y, String.format(Locale.ROOT, "%.1f", Float.valueOf(c.scale))}).getString();
        g.text(this.font, info, (this.width - this.font.width(info)) / 2, this.height - 30 - 12, -6488198, true);
    }

    public void removed() {
        HudRenderer.suppressHud = false;
        HudConfig.save();
        super.removed();
    }
}

