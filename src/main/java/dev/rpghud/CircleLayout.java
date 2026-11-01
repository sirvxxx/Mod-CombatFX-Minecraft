package dev.rpghud;

import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;

final class CircleLayout {
    static final int D = 92;
    private static final int R = 46;
    private static final int RING_TEX = 96;
    static final int NAME_H = 17;
    private static final int NAME_GAP = 4;
    static final int LEVEL_H = 17;
    private static final int LEVEL_GAP = 4;
    static final int INFO_H = 19;
    private static final int SIDE_Y = 25;
    private static final int SIDE_GAP = 3;
    private static final int PANEL_GAP = 4;
    private static final int BAND_RI = 33;
    private static final int BAND_RO = 40;
    private static final double CELL_DEG = 9.0;
    private static final float[] SHADE_K = new float[]{0.78f, 1.0f, 1.25f};
    private static int[] bandX;
    private static int[] bandY;
    private static int[] bandSec;
    private static int[] bandCell;
    private static int[] bandShade;
    private static float[] bandPf;
    private static int bandCount;
    private static final float[] HP_C;
    private static final float[] GH_C;
    private static final float[] FO_C;
    private static final float[] SA_C;
    private static final float[] AR_C;
    private static final RectBatch RING;
    private static long ringKey;

    private CircleLayout() {
    }

    static boolean circleVisible(HudConfig c) {
        return c.showFace || c.showHealth || c.showHunger || c.showArmor;
    }

    private static int circleTop(HudConfig c) {
        return c.showName ? 21 : 0;
    }

    static int width(HudConfig c) {
        boolean circle = CircleLayout.circleVisible(c);
        int w = circle || c.showName ? 92 : 0;
        int side = 0;
        if (c.showCoords) {
            side = Math.max(side, HudState.coordsPanelW);
        }
        if (c.showFps) {
            side = Math.max(side, HudState.fpsPanelW);
        }
        if (side > 0) {
            w = Math.max(w, (circle ? 95 : 0) + side);
        }
        return w;
    }

    static int height(HudConfig c) {
        int h;
        boolean circle = CircleLayout.circleVisible(c);
        int ctop = CircleLayout.circleTop(c);
        int n = h = c.showName ? 17 : 0;
        if (circle) {
            h = ctop + 92 + (c.showLevel ? 21 : 0);
        }
        int sy = circle ? ctop + 25 : ctop;
        int bottom = 0;
        if (c.showCoords) {
            bottom = sy + 19;
            sy += 23;
        }
        if (c.showFps) {
            bottom = sy + 19;
        }
        return Math.max(h, bottom);
    }

    static void draw(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, HudConfig c) {
        int tx;
        int sy;
        HudTheme t = HudState.T;
        boolean circle = CircleLayout.circleVisible(c);
        int ctop = CircleLayout.circleTop(c);
        if (c.showName) {
            HudDraw.badge(g, t, 0, 0, 92, 17);
            g.text(mc.font, HudState.nameCircle, (92 - HudState.nameCircleW) / 2, 5, t.text, true);
        }
        if (circle) {
            CircleLayout.drawCircle(g, mc, c, t, ctop);
        }
        int sx = circle ? 95 : 0;
        int n = sy = circle ? ctop + 25 : ctop;
        if (c.showCoords) {
            HudDraw.frame(g, t, sx, sy, HudState.coordsPanelW, 19);
            HudDraw.icon(g, t.iconCoords, sx + 7, sy + 5);
            tx = sx + 7 + 8 + 3;
            g.text(mc.font, HudState.coordsLabel, tx, sy + 6, t.text, true);
            g.text(mc.font, HudState.coordsVals, tx + HudState.coordsLabelW, sy + 6, -1, true);
            sy += 23;
        }
        if (c.showFps) {
            HudDraw.frame(g, t, sx, sy, HudState.fpsPanelW, 19);
            HudDraw.icon(g, t.iconFps, sx + 7, sy + 5);
            tx = sx + 7 + 8 + 3;
            g.text(mc.font, HudState.fpsNum, tx, sy + 6, HudRenderer.fpsColor(), true);
            g.text(mc.font, HudState.fpsLabel, tx + HudState.fpsNumW, sy + 6, t.text, true);
        }
    }

    private static void drawCircle(GuiGraphicsExtractor g, Minecraft mc, HudConfig c, HudTheme t, int top) {
        int cx = 46;
        int cy = top + 46;
        g.blit(RenderPipelines.GUI_TEXTURED, t.ring, 0, top, 0.0f, 0.0f, 96, 96, 96, 96, 96, 96);
        long key = HudState.statKey(c.showHealth, c.showHunger, c.showArmor);
        if (key != ringKey) {
            ringKey = key;
            CircleLayout.rebuildRing(t, c.showHealth, c.showHunger, c.showArmor);
        }
        RING.draw(g, 46, cy);
        if (c.showFace) {
            g.blit(RenderPipelines.GUI_TEXTURED, HudState.skin, 26, cy - 20, 8.0f, 8.0f, 40, 40, 8, 8, 64, 64);
            g.blit(RenderPipelines.GUI_TEXTURED, HudState.skin, 26, cy - 20, 40.0f, 8.0f, 40, 40, 8, 8, 64, 64);
        }
        if (c.showLevel) {
            int bw = Math.max(56, HudState.levelW + 22);
            int bx = (92 - bw) / 2;
            int by = top + 92 + 4;
            HudDraw.badge(g, t, bx, by, bw, 17);
            g.text(mc.font, HudState.levelStr, bx + (bw - HudState.levelW) / 2, by + 5, t.level, true);
        }
    }

    private static void cells(float[] out, int n, float frac) {
        for (int k = 0; k < n; ++k) {
            out[k] = HudDraw.clamp01(frac * (float)n - (float)k);
        }
    }

    private static void initBand() {
        if (bandX != null) {
            return;
        }
        int RO = 40;
        int RI = 33;
        int cap = 6400;
        int[] xs = new int[cap];
        int[] ys = new int[cap];
        int[] secs = new int[cap];
        int[] cls = new int[cap];
        int[] shs = new int[cap];
        float[] pfs = new float[cap];
        int n = 0;
        for (int dy = -40; dy < 40; ++dy) {
            double fy = (double)dy + 0.5;
            for (int dx = -40; dx < 40; ++dx) {
                double l;
                int cellsN;
                int sec;
                double fx = (double)dx + 0.5;
                double d2 = fx * fx + fy * fy;
                if (d2 < 1089.0 || d2 >= 1600.0) continue;
                double r = Math.sqrt(d2);
                double theta = Math.toDegrees(Math.atan2(fx, -fy));
                if (theta < 0.0) {
                    theta += 360.0;
                }
                if (theta >= 180.0) {
                    sec = 0;
                    cellsN = 20;
                    l = theta - 180.0;
                } else if (theta < 90.0) {
                    sec = 1;
                    cellsN = 10;
                    l = 90.0 - theta;
                } else {
                    sec = 2;
                    cellsN = 10;
                    l = 180.0 - theta;
                }
                int k = Math.min(cellsN - 1, (int)Math.floor(l / 9.0));
                double p = l - (double)k * 9.0;
                double edgeDeg = Math.toDegrees(1.0 / r);
                boolean edge = r < 34.0 || r >= 39.0 || p < edgeDeg || k == cellsN - 1 && p > 9.0 - edgeDeg;
                xs[n] = dx;
                ys[n] = dy;
                secs[n] = sec;
                cls[n] = k;
                pfs[n] = (float)Math.max(0.0, Math.min(1.0, (p - edgeDeg) / (9.0 - edgeDeg)));
                shs[n] = edge ? 3 : (r < 35.0 ? 0 : (r >= 38.0 ? 2 : 1));
                ++n;
            }
        }
        bandX = Arrays.copyOf(xs, n);
        bandY = Arrays.copyOf(ys, n);
        bandSec = Arrays.copyOf(secs, n);
        bandCell = Arrays.copyOf(cls, n);
        bandShade = Arrays.copyOf(shs, n);
        bandPf = Arrays.copyOf(pfs, n);
        bandCount = n;
    }

    private static void rebuildRing(HudTheme t, boolean showHp, boolean showFood, boolean showArmor) {
        CircleLayout.initBand();
        CircleLayout.cells(HP_C, 20, (float)HudState.hpQ / 320.0f);
        CircleLayout.cells(GH_C, 20, (float)HudState.gQ / 320.0f);
        CircleLayout.cells(FO_C, 10, (float)HudState.food / 20.0f);
        CircleLayout.cells(SA_C, 10, (float)HudState.satQ / 40.0f);
        CircleLayout.cells(AR_C, 10, (float)HudState.arm / 20.0f);
        int hpBase = HudState.hpBase();
        RING.clear();
        int runY = 0;
        int runX0 = 0;
        int runX1 = 0;
        int runCol = 0;
        for (int i = 0; i < bandCount; ++i) {
            boolean on;
            int sec = bandSec[i];
            int col = 0;
            boolean bl = sec == 0 ? showHp : (on = sec == 1 ? showFood : showArmor);
            if (!on) {
                col = t.bandBack;
            } else {
                int sh = bandShade[i];
                if (sh != 3) {
                    int k = bandCell[i];
                    float pf = bandPf[i];
                    int base = 0;
                    boolean accent = false;
                    switch (sec) {
                        case 0: {
                            float f = HP_C[k];
                            if (f >= 0.999f || pf < f) {
                                base = hpBase;
                                break;
                            }
                            if (!(GH_C[k] >= 0.999f) && !(pf < GH_C[k])) break;
                            base = t.ghost;
                            break;
                        }
                        case 1: {
                            float f = FO_C[k];
                            if (!(f >= 0.999f) && !(pf < f)) break;
                            base = t.food;
                            accent = sh == 2 && (SA_C[k] >= 0.999f || pf < SA_C[k]);
                            break;
                        }
                        default: {
                            float f = AR_C[k];
                            if (!(f >= 0.999f) && !(pf < f)) break;
                            base = t.armor;
                        }
                    }
                    if (base != 0) {
                        col = accent ? t.sat : HudDraw.mul(base, SHADE_K[sh]);
                    }
                }
            }
            int y = bandY[i];
            int x = bandX[i];
            if (runCol != 0 && col == runCol && y == runY && x == runX1) {
                runX1 = x + 1;
                continue;
            }
            if (runCol != 0) {
                RING.addRun(runX0, runX1, runY, runCol);
            }
            runCol = col;
            runY = y;
            runX0 = x;
            runX1 = x + 1;
        }
        if (runCol != 0) {
            RING.addRun(runX0, runX1, runY, runCol);
        }
    }

    static {
        HP_C = new float[20];
        GH_C = new float[20];
        FO_C = new float[10];
        SA_C = new float[10];
        AR_C = new float[10];
        RING = new RectBatch(1100);
        ringKey = Long.MIN_VALUE;
    }
}

