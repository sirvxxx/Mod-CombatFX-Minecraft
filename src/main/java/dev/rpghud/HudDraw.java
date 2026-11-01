package dev.rpghud;

import dev.rpgcombat.theme.PackTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class HudDraw {
    public static final int ICON = 8;

    private HudDraw() {
    }

    public static void frame(GuiGraphicsExtractor g, HudTheme t, int x, int y, int w, int h) {
        if (t.auto) {
            PackTheme.panel(g, x, y, w, h);
            return;
        }
        g.fill(x + 1, y + 1, x + w + 3, y + h + 3, 0x22000000);
        g.fill(x + 2, y + 3, x + w + 2, y + h + 2, 0x44000000);
        HudDraw.slice(g, t.frame, 20, 20, 5, x, y, w, h);
    }

    public static void badge(GuiGraphicsExtractor g, HudTheme t, int x, int y, int w, int h) {
        if (t.auto) {
            PackTheme.panel(g, x, y, w, h);
            return;
        }
        g.fill(x + 1, y + 2, x + w + 1, y + h + 2, 0x55000000);
        HudDraw.slice(g, t.badge, 16, 16, 4, x, y, w, h);
    }

    public static void icon(GuiGraphicsExtractor g, Identifier tex, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0.0f, 0.0f, 8, 8, 8, 8, 8, 8);
    }

    public static void slice(GuiGraphicsExtractor g, Identifier tex, int tw, int th, int b, int x, int y, int w, int h) {
        int cw = tw - 2 * b;
        int ch = th - 2 * b;
        int mw = w - 2 * b;
        int mh = h - 2 * b;
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0.0f, 0.0f, b, b, b, b, tw, th);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x + w - b, y, (float)(tw - b), 0.0f, b, b, b, b, tw, th);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y + h - b, 0.0f, (float)(th - b), b, b, b, b, tw, th);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x + w - b, y + h - b, (float)(tw - b), (float)(th - b), b, b, b, b, tw, th);
        if (mw > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, tex, x + b, y, (float)b, 0.0f, mw, b, cw, b, tw, th);
            g.blit(RenderPipelines.GUI_TEXTURED, tex, x + b, y + h - b, (float)b, (float)(th - b), mw, b, cw, b, tw, th);
        }
        if (mh > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y + b, 0.0f, (float)b, b, mh, b, ch, tw, th);
            g.blit(RenderPipelines.GUI_TEXTURED, tex, x + w - b, y + b, (float)(tw - b), (float)b, b, mh, b, ch, tw, th);
        }
        if (mw > 0 && mh > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, tex, x + b, y + b, (float)b, (float)b, mw, mh, cw, ch, tw, th);
        }
    }

    public static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    public static int mul(int c, float k) {
        int r = Math.min(255, (int)((float)(c >> 16 & 0xFF) * k));
        int g = Math.min(255, (int)((float)(c >> 8 & 0xFF) * k));
        int b = Math.min(255, (int)((float)(c & 0xFF) * k));
        return c & 0xFF000000 | r << 16 | g << 8 | b;
    }

    public static String fit(Minecraft mc, String s, int maxW) {
        while (s.length() > 1 && mc.font.width(s) > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /** Bingkai panel untuk menu: tema "auto" mengambil tepi dari resource pack aktif. */
    public static void panelBorder(GuiGraphicsExtractor g, HudTheme t, int x, int y, int w, int h) {
        if (t.auto) {
            PackTheme.border(g, x, y, w, h);
        } else {
            HudDraw.border(g, t.frame, 20, 20, 5, x, y, w, h);
        }
    }

    /** Seperti slice(), tapi hanya bingkai (4 sudut + 4 sisi), bagian tengah tidak digambar. */
    public static void border(GuiGraphicsExtractor g, Identifier tex, int tw, int th, int b, int x, int y, int w, int h) {
        int cw = tw - 2 * b;
        int ch = th - 2 * b;
        int mw = w - 2 * b;
        int mh = h - 2 * b;
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0.0f, 0.0f, b, b, b, b, tw, th);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x + w - b, y, (float)(tw - b), 0.0f, b, b, b, b, tw, th);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y + h - b, 0.0f, (float)(th - b), b, b, b, b, tw, th);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x + w - b, y + h - b, (float)(tw - b), (float)(th - b), b, b, b, b, tw, th);
        if (mw > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, tex, x + b, y, (float)b, 0.0f, mw, b, cw, b, tw, th);
            g.blit(RenderPipelines.GUI_TEXTURED, tex, x + b, y + h - b, (float)b, (float)(th - b), mw, b, cw, b, tw, th);
        }
        if (mh > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y + b, 0.0f, (float)b, b, mh, b, ch, tw, th);
            g.blit(RenderPipelines.GUI_TEXTURED, tex, x + w - b, y + b, (float)(tw - b), (float)b, b, mh, b, ch, tw, th);
        }
    }
}
