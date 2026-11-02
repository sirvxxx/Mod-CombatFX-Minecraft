package dev.rpghud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import org.joml.Matrix3x2fStack;

public final class HudRenderer {
    public static boolean suppressHud = false;
    private static long fpsWindowStart = System.currentTimeMillis();
    private static int frames;
    private static int fps;
    static float shownHp;
    private static long lastNs;

    private HudRenderer() {
    }

    private static void ensure(HudConfig c) {
        if (HudState.ready) {
            HudState.theme(c);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        HudState.theme(c);
        HudState.update(mc, mc.player, fps, shownHp);
    }

    public static int width(HudConfig c) {
        HudRenderer.ensure(c);
        return c.layoutType == 1 ? BarLayout.width(c) : CircleLayout.width(c);
    }

    public static int height(HudConfig c) {
        HudRenderer.ensure(c);
        return c.layoutType == 1 ? BarLayout.height(c) : CircleLayout.height(c);
    }

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        HudRenderer.tick(p);
        HudConfig c = HudConfig.get();
        if (suppressHud || !c.enabled || p == null) {
            return;
        }
        HudRenderer.draw(g, mc, p, c);
    }

    public static void preview(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        HudRenderer.draw(g, mc, mc.player, HudConfig.get());
    }

    static int fpsColor() {
        return fps >= 60 ? -8586116 : (fps >= 30 ? -8090 : -38037);
    }

    private static void tick(LocalPlayer p) {
        long now = System.currentTimeMillis();
        ++frames;
        if (now - fpsWindowStart >= 1000L) {
            fps = (int)((long)frames * 1000L / (now - fpsWindowStart));
            frames = 0;
            fpsWindowStart = now;
        }
        if (p != null) {
            float hp = p.getHealth();
            long ns = System.nanoTime();
            float dt = Math.min(0.25f, (float)(ns - lastNs) / 1.0E9f);
            lastNs = ns;
            shownHp = shownHp < 0.0f || hp >= shownHp ? hp : (shownHp -= (shownHp - hp) * Math.min(1.0f, dt * 2.5f));
        }
    }

    private static void draw(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, HudConfig c) {
        HudState.theme(c);
        HudState.update(mc, p, fps, shownHp);
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate((float)c.x, (float)c.y);
        pose.scale(c.scale, c.scale);
        if (c.layoutType == 1) {
            BarLayout.draw(g, mc, p, c);
        } else {
            HudAlpha.set(c.barsOpacity);
            CircleLayout.draw(g, mc, p, c);
            HudAlpha.set(1.0f);
        }
        pose.popMatrix();
    }

    static {
        shownHp = -1.0f;
        lastNs = System.nanoTime();
    }
}

