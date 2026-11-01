package dev.rpghud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

final class HudState {
    static HudTheme T = HudTheme.byId(null);
    private static String themeRef;
    static boolean ready;
    private static int frame;
    static String nameCircle;
    static String nameBar;
    static int nameCircleW;
    static int nameBarW;
    static String levelStr;
    static int levelW;
    static String coordsLabel;
    static int coordsLabelW;
    static String coordsVals;
    static int coordsValsW;
    static String fpsNum;
    static int fpsNumW;
    static String fpsLabel;
    static int fpsLabelW;
    static int fpsTextW;
    static String hpStr;
    static int hpStrW;
    static int coordsPanelW;
    static int fpsPanelW;
    static Identifier skin;
    static int hpQ;
    static int gQ;
    static int food;
    static int satQ;
    static int arm;
    static int pulseQ;
    static float hpFrac;
    private static String rawName;
    private static int lastLevel;
    private static int lcx;
    private static int lcy;
    private static int lcz;
    private static int lastFps;
    private static int lastHp;
    private static int lastMax;

    private HudState() {
    }

    static void theme(HudConfig c) {
        if (c.theme != themeRef) {
            T = HudTheme.byId(c.theme);
            themeRef = c.theme;
        }
    }

    private static String tr(String key, Object ... args) {
        return Component.translatable((String)key, (Object[])args).getString();
    }

    static void update(Minecraft mc, LocalPlayer p, int fps, float ghostHp) {
        boolean refresh;
        boolean bl = refresh = !ready || (++frame & 0x7F) == 0;
        if (refresh) {
            rawName = p.getName().getString();
            nameCircle = HudDraw.fit(mc, rawName, 76);
            nameCircleW = mc.font.width(nameCircle);
            nameBar = HudDraw.fit(mc, rawName, 108);
            nameBarW = mc.font.width(nameBar);
            coordsLabel = HudState.tr("custom_rpg_hud.hud.coords", new Object[0]) + " ";
            coordsLabelW = mc.font.width(coordsLabel);
            fpsLabel = " " + HudState.tr("custom_rpg_hud.hud.fps", new Object[0]);
            fpsLabelW = mc.font.width(fpsLabel);
            fpsTextW = mc.font.width("000" + fpsLabel);
            skin = SkinHelper.bodyTexture(p);
            lastLevel = -1;
            lcx = Integer.MIN_VALUE;
            lastFps = -1;
            lastHp = -1;
        } else if ((frame & 0xF) == 0) {
            skin = SkinHelper.bodyTexture(p);
        }
        int lv = p.experienceLevel;
        if (lv != lastLevel) {
            lastLevel = lv;
            levelStr = HudState.tr("custom_rpg_hud.hud.level", lv);
            levelW = mc.font.width(levelStr);
        }
        int bx = (int)Math.floor(p.getX());
        int by = (int)Math.floor(p.getY());
        int bz = (int)Math.floor(p.getZ());
        if (bx != lcx || by != lcy || bz != lcz) {
            lcx = bx;
            lcy = by;
            lcz = bz;
            coordsVals = bx + " " + by + " " + bz;
            coordsValsW = mc.font.width(coordsVals);
        }
        if (fps != lastFps) {
            lastFps = fps;
            fpsNum = Integer.toString(fps);
            fpsNumW = mc.font.width(fpsNum);
        }
        float max = Math.max(1.0f, p.getMaxHealth());
        float hp = p.getHealth();
        int hpI = (int)Math.ceil(hp);
        int maxI = (int)Math.ceil(max);
        if (hpI != lastHp || maxI != lastMax) {
            lastHp = hpI;
            lastMax = maxI;
            hpStr = hpI + "/" + maxI;
            hpStrW = mc.font.width(hpStr);
        }
        hpFrac = HudDraw.clamp01(hp / max);
        hpQ = Math.round(hpFrac * 320.0f);
        gQ = Math.round(HudDraw.clamp01((ghostHp < 0.0f ? hp : ghostHp) / max) * 320.0f);
        food = p.getFoodData().getFoodLevel();
        float sat = Math.min(p.getFoodData().getSaturationLevel(), (float)food);
        satQ = Math.min(40, (int)(sat * 2.0f));
        arm = Math.min(20, p.getArmorValue());
        pulseQ = hpFrac < 0.3f ? 1 + (int)((Math.sin((double)System.currentTimeMillis() / 140.0) + 1.0) * 3.49) : 0;
        coordsPanelW = Math.max(110, coordsLabelW + coordsValsW + 7 + 8 + 3 + 8);
        fpsPanelW = Math.max(72, fpsTextW + 7 + 8 + 3 + 8);
        ready = true;
    }

    static long statKey(boolean sH, boolean sF, boolean sA) {
        return (long)hpQ | (long)gQ << 9 | (long)food << 18 | (long)satQ << 23 | (long)arm << 29 | (long)pulseQ << 34 | (sH ? 1L : 0L) << 37 | (sF ? 1L : 0L) << 38 | (sA ? 1L : 0L) << 39 | (long)HudState.T.index << 40;
    }

    static int hpBase() {
        return pulseQ == 0 ? HudState.T.hp : HudDraw.mul(HudState.T.hp, 0.8f + 0.2f * ((float)(pulseQ - 1) / 6.0f));
    }

    static {
        nameCircle = "";
        nameBar = "";
        levelStr = "";
        coordsLabel = "XYZ ";
        coordsLabelW = 18;
        coordsVals = "0 0 0";
        coordsValsW = 30;
        fpsNum = "0";
        fpsNumW = 6;
        fpsLabel = " FPS";
        fpsLabelW = 24;
        fpsTextW = 42;
        hpStr = "0/0";
        hpStrW = 18;
        coordsPanelW = 124;
        fpsPanelW = 72;
        rawName = "";
        lastLevel = -1;
        lcx = Integer.MIN_VALUE;
        lastFps = -1;
        lastHp = -1;
        lastMax = -1;
    }
}

