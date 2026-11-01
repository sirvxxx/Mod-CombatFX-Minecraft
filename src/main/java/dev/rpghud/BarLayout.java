package dev.rpghud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;

final class BarLayout {
    static final int FACE = 40;
    static final int LEVEL_H = 17;
    private static final int LEVEL_GAP = 3;
    static final int PANEL_X = 44;
    static final int PANEL_W = 170;
    static final int PANEL_H = 52;
    private static final int PANEL_Y = 4;
    static final int INFO_H = 19;
    private static final int GAP = 3;
    private static final int ICON = 8;
    private static final int HP_Y = 22;
    private static final int HP_H = 10;
    private static final int ROW2_Y = 35;
    private static final int ROW2_H = 9;
    private static final int HP_X = 18;
    private static final int HP_W = 145;
    private static final int HALF = 75;
    private static final int HUN_X = 18;
    private static final int HUN_W = 64;
    private static final int ARM_ICON_X = 88;
    private static final int ARM_X = 99;
    private static final int ARM_W = 64;
    private static final float[] SHADE_K = new float[]{1.25f, 1.0f, 0.78f};
    private static final RectBatch BARS = new RectBatch(128);
    private static long barsKey = Long.MIN_VALUE;
    private static final int[] SEP = new int[11];

    private BarLayout() {
    }

    private static boolean panelVisible(HudConfig c) {
        return c.showName || c.showHealth || c.showHunger || c.showArmor;
    }

    private static boolean infoVisible(HudConfig c) {
        return c.showCoords || c.showFps;
    }

    private static int topHeight(HudConfig c) {
        int left = c.showFace ? (c.showLevel ? 60 : 40) : 0;
        int panel = BarLayout.panelVisible(c) ? (c.showFace ? 4 : 0) + 52 : 0;
        return Math.max(left, panel);
    }

    private static int mainWidth(HudConfig c) {
        boolean panel = BarLayout.panelVisible(c);
        if (panel) {
            return (c.showFace ? 44 : 0) + 170;
        }
        return c.showFace ? 40 : 0;
    }

    private static int infoWidth(HudConfig c) {
        int need = 14;
        if (c.showCoords) {
            need += 11 + HudState.coordsLabelW + HudState.coordsValsW;
        }
        if (c.showFps) {
            need += 11 + HudState.fpsTextW;
        }
        if (c.showCoords && c.showFps) {
            need += 10;
        }
        return Math.max(BarLayout.mainWidth(c), need);
    }

    static int width(HudConfig c) {
        int w = BarLayout.mainWidth(c);
        if (BarLayout.infoVisible(c)) {
            w = Math.max(w, BarLayout.infoWidth(c));
        }
        return w;
    }

    static int height(HudConfig c) {
        int h = BarLayout.topHeight(c);
        if (BarLayout.infoVisible(c)) {
            h += (h > 0 ? 3 : 0) + 19;
        }
        return h;
    }

    static void draw(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, HudConfig c) {
        HudTheme t = HudState.T;
        boolean face = c.showFace;
        int top = BarLayout.topHeight(c);
        if (face) {
            HudDraw.frame(g, t, 0, 0, 40, 40);
            g.blit(RenderPipelines.GUI_TEXTURED, HudState.skin, 4, 4, 8.0f, 8.0f, 32, 32, 8, 8, 64, 64);
            g.blit(RenderPipelines.GUI_TEXTURED, HudState.skin, 4, 4, 40.0f, 8.0f, 32, 32, 8, 8, 64, 64);
            if (c.showLevel) {
                int bw = Math.max(34, Math.min(40, HudState.levelW + 14));
                int bx = (40 - bw) / 2;
                int by = 43;
                HudDraw.badge(g, t, bx, by, bw, 17);
                g.text(mc.font, HudState.levelStr, bx + (bw - HudState.levelW) / 2, by + 5, t.level, true);
            }
        }
        if (BarLayout.panelVisible(c)) {
            BarLayout.drawPanel(g, mc, c, t, face ? 44 : 0, face ? 4 : 0);
        }
        if (BarLayout.infoVisible(c)) {
            BarLayout.drawInfo(g, mc, c, t, top > 0 ? top + 3 : 0, BarLayout.infoWidth(c));
        }
    }

    private static void drawPanel(GuiGraphicsExtractor g, Minecraft mc, HudConfig c, HudTheme t, int px, int py) {
        long key;
        HudDraw.frame(g, t, px, py, 170, 52);
        if (c.showName) {
            g.text(mc.font, HudState.nameBar, px + 7, py + 9, t.text, true);
        }
        if (c.showHealth) {
            g.text(mc.font, HudState.hpStr, px + 170 - 7 - HudState.hpStrW, py + 9, -1, true);
        }
        if (c.showHealth) {
            HudDraw.icon(g, t.iconHeart, px + 7, py + 22 + 1);
        }
        if (c.showHunger) {
            HudDraw.icon(g, t.iconFood, px + 7, py + 35);
        }
        if (c.showArmor) {
            HudDraw.icon(g, t.iconArmor, px + 88, py + 35);
        }
        if ((key = HudState.statKey(c.showHealth, c.showHunger, c.showArmor)) != barsKey) {
            barsKey = key;
            BarLayout.rebuildBars(t, c.showHealth, c.showHunger, c.showArmor);
        }
        BARS.draw(g, px, py);
    }

    private static void drawInfo(GuiGraphicsExtractor g, Minecraft mc, HudConfig c, HudTheme t, int y, int w) {
        HudDraw.frame(g, t, 0, y, w, 19);
        int ty = y + 6;
        if (c.showCoords) {
            HudDraw.icon(g, t.iconCoords, 7, y + 5);
            int tx = 18;
            g.text(mc.font, HudState.coordsLabel, tx, ty, t.text, true);
            g.text(mc.font, HudState.coordsVals, tx + HudState.coordsLabelW, ty, -1, true);
        }
        if (c.showFps) {
            int groupW = 11 + HudState.fpsNumW + HudState.fpsLabelW;
            int fx = w - 7 - groupW;
            if (c.showCoords) {
                g.fill(fx - 6, y + 5, fx - 5, y + 19 - 5, t.empty[3]);
                g.fill(fx - 5, y + 5, fx - 4, y + 19 - 5, 0x60FFFFFF & (t.text | 0xFF000000));
            }
            HudDraw.icon(g, t.iconFps, fx, y + 5);
            int tx = fx + 8 + 3;
            g.text(mc.font, HudState.fpsNum, tx, ty, HudRenderer.fpsColor(), true);
            g.text(mc.font, HudState.fpsLabel, tx + HudState.fpsNumW, ty, t.text, true);
        }
    }

    private static void rebuildBars(HudTheme t, boolean showHp, boolean showFood, boolean showArmor) {
        BARS.clear();
        if (showHp) {
            BarLayout.addBar(BARS, t, 18, 22, 145, 10, (float)HudState.hpQ / 320.0f, (float)HudState.gQ / 320.0f, 0.0f, HudState.hpBase(), t.ghost);
        }
        if (showFood) {
            BarLayout.addBar(BARS, t, 18, 35, 64, 9, (float)HudState.food / 20.0f, 0.0f, (float)HudState.satQ / 40.0f, t.food, 0);
        }
        if (showArmor) {
            BarLayout.addBar(BARS, t, 99, 35, 64, 9, (float)HudState.arm / 20.0f, 0.0f, 0.0f, t.armor, 0);
        }
    }

    private static int extent(float v, int[] sep) {
        if (v <= 0.0f) {
            return sep[0] + 1;
        }
        if (v >= 1.0f) {
            return sep[10];
        }
        float s = v * 10.0f;
        int k = (int)s;
        float r = s - (float)k;
        return sep[k] + 1 + Math.round(r * (float)(sep[k + 1] - sep[k] - 1));
    }

    private static void rows3(RectBatch rb, int x0, int x1, int y, int h, int light, int mid, int dark) {
        if (x1 <= x0) {
            return;
        }
        rb.add(x0, y + 1, x1, y + 2, light);
        rb.add(x0, y + 2, x1, y + h - 2, mid);
        rb.add(x0, y + h - 2, x1, y + h - 1, dark);
    }

    private static void addBar(RectBatch rb, HudTheme t, int x, int y, int w, int h, float v, float ghost, float acc, int base, int ghostCol) {
        int ae;
        for (int i = 0; i <= 10; ++i) {
            BarLayout.SEP[i] = x + Math.round((float)(i * (w - 1)) / 10.0f);
        }
        int ix0 = x + 1;
        int ix1 = SEP[10];
        int edge = t.empty[3];
        rb.add(x, y, x + w, y + h, edge);
        BarLayout.rows3(rb, ix0, ix1, y, h, t.empty[2], t.empty[1], t.empty[0]);
        int fill = BarLayout.extent(v, SEP);
        if (ghostCol != 0 && ghost > v) {
            int ge = BarLayout.extent(ghost, SEP);
            BarLayout.rows3(rb, fill, ge, y, h, HudDraw.mul(ghostCol, SHADE_K[0]), ghostCol, HudDraw.mul(ghostCol, SHADE_K[2]));
        }
        if (v > 0.0f) {
            BarLayout.rows3(rb, ix0, fill, y, h, HudDraw.mul(base, SHADE_K[0]), base, HudDraw.mul(base, SHADE_K[2]));
        }
        if (acc > 0.0f && (ae = Math.min(fill, BarLayout.extent(acc, SEP))) > ix0) {
            rb.add(ix0, y + h - 2, ae, y + h - 1, t.sat);
        }
        for (int i = 1; i < 10; ++i) {
            rb.add(SEP[i], y + 1, SEP[i] + 1, y + h - 1, edge);
        }
    }
}

