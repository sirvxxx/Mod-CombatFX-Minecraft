package dev.rpgcombat.extras;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;

/**
 * Progress hancur blok di dekat crosshair.
 *
 * Sumber data (tanpa mixin, jadi tidak bisa membuat game crash saat start):
 *  1. field privat MultiPlayerGameMode#destroyProgress (0..1) dibaca lewat reflection,
 *  2. cadangan: getDestroyStage() (0..9) kalau field tidak ketemu.
 * isDestroying() public dipakai sebagai penanda "sedang menambang".
 *
 * Nilai diperbarui per tick (20x/detik); tampilan dihaluskan per frame dengan delta time.
 */
public final class BreakProgress {
	private BreakProgress() {}

	private static Field progressField;
	private static Method stageMethod;
	private static boolean reflected;

	private static float target;    // dari tick
	private static boolean active;
	private static float shown;     // nilai tampil (smooth)
	private static float alpha;     // 0..1

	// cache titik cincin untuk gaya circular
	private static final int RING_N = 72;
	private static int ringR = -1;
	private static final int[] RING_X = new int[RING_N];
	private static final int[] RING_Y = new int[RING_N];

	// ---------------------------------------------------------------- tick
	public static void tick(Minecraft mc) {
		MultiPlayerGameMode gm = mc.gameMode;
		if (gm == null || mc.player == null || mc.level == null) {
			active = false;
			target = 0f;
			return;
		}
		boolean destroying = gm.isDestroying();
		float p = destroying ? readProgress(gm) : 0f;
		boolean nowActive = destroying && p > 0.001f;

		// blok baru selesai dihancurkan: progress di-reset vanilla, tampilkan 100% sebentar
		if (active && !nowActive && shown >= 0.75f) {
			shown = 1f;
		}
		active = nowActive;
		target = Fx.clamp01(p);
	}

	private static float readProgress(MultiPlayerGameMode gm) {
		if (!reflected) {
			reflected = true;
			try {
				Field f = MultiPlayerGameMode.class.getDeclaredField("destroyProgress");
				f.setAccessible(true);
				progressField = f;
			} catch (Throwable t) {
				progressField = null;
			}
			try {
				stageMethod = MultiPlayerGameMode.class.getMethod("getDestroyStage");
			} catch (Throwable t) {
				stageMethod = null;
			}
		}
		if (progressField != null) {
			try {
				return progressField.getFloat(gm);
			} catch (Throwable t) {
				progressField = null;
			}
		}
		if (stageMethod != null) {
			try {
				int stage = (Integer) stageMethod.invoke(gm);
				return stage < 0 ? 0f : (stage + 0.5f) / 10f;
			} catch (Throwable t) {
				stageMethod = null;
			}
		}
		return 0f;
	}

	// -------------------------------------------------------------- render
	// Warna TETAP monokrom: trek abu-abu, isi putih. Tidak ada gradasi / pergantian warna.
	private static final int TRACK = 0x4A4A4A;
	private static final int FILL = 0xFFFFFF;
	private static final int FILL_SHADE = 0xCFCFCF;

	public static void render(GuiGraphicsExtractor g, Minecraft mc, ExtrasConfig cfg, float dt) {
		// smoothing berbasis delta time
		if (active) {
			if (target < shown - 0.25f) shown = target; // pindah ke blok lain
			shown += (target - shown) * Fx.damp(dt, 18f);
			alpha = Math.min(1f, alpha + dt * 10f);
		} else {
			alpha = Math.max(0f, alpha - dt * 5f);
			if (alpha <= 0f) {
				shown = 0f;
				return;
			}
		}
		if (alpha <= 0.01f) return;

		var window = mc.getWindow();
		int cx = window.getGuiScaledWidth() / 2 + cfg.breakOffsetX;
		int cy = window.getGuiScaledHeight() / 2;
		float p = Fx.clamp01(shown);

		if (cfg.breakStyle == 1) {
			drawCircular(g, mc, cfg, cx, cy, p);
		} else {
			drawLinear(g, mc, cfg, cx, cy, p);
		}
	}

	private static void drawLinear(GuiGraphicsExtractor g, Minecraft mc, ExtrasConfig cfg, int cx, int cy, float p) {
		int w = Math.max(20, Math.round(44 * cfg.breakScale));
		int h = Math.max(2, Math.round(3 * cfg.breakScale));
		int x0 = cx - w / 2;
		int y0 = cy + cfg.breakOffsetY;
		int filled = Math.round(w * p);

		g.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + h + 1, Fx.argb(alpha * 0.60f, 0x000000));
		g.fill(x0, y0, x0 + w, y0 + h, Fx.argb(alpha * 0.85f, TRACK));
		if (filled > 0) {
			g.fill(x0, y0, x0 + filled, y0 + h, Fx.argb(alpha, FILL));
			if (h >= 3) g.fill(x0, y0 + h - 1, x0 + filled, y0 + h, Fx.argb(alpha, FILL_SHADE));
		}
		if (cfg.breakShowPercent) {
			drawPercent(g, mc, cfg, cx, y0 + h + 3, p);
		}
	}

	private static void drawCircular(GuiGraphicsExtractor g, Minecraft mc, ExtrasConfig cfg, int cx, int cy, float p) {
		int r = Math.max(8, Math.round(13 * cfg.breakScale));
		if (r != ringR) buildRing(r);
		int t = Math.max(2, Math.round(2 * cfg.breakScale));

		int bg = Fx.argb(alpha * 0.70f, TRACK);
		for (int i = 0; i < RING_N; i++) {
			g.fill(cx + RING_X[i], cy + RING_Y[i], cx + RING_X[i] + t, cy + RING_Y[i] + t, bg);
		}
		int k = Math.round(RING_N * p);
		int fg = Fx.argb(alpha, FILL);
		for (int i = 0; i < k; i++) {
			g.fill(cx + RING_X[i], cy + RING_Y[i], cx + RING_X[i] + t, cy + RING_Y[i] + t, fg);
		}
		if (cfg.breakShowPercent) {
			drawPercent(g, mc, cfg, cx, cy + r + t + 3, p);
		}
	}

	private static void drawPercent(GuiGraphicsExtractor g, Minecraft mc, ExtrasConfig cfg, int cx, int y, float p) {
		String s = Math.round(p * 100f) + "%";
		Font font = mc.font;
		float sc = 0.8f * cfg.breakScale;
		g.pose().pushMatrix();
		g.pose().translate(cx, y);
		g.pose().scale(sc, sc);
		g.text(font, s, -font.width(s) / 2, 0, Fx.argb(alpha, FILL), true);
		g.pose().popMatrix();
	}

	/** Titik cincin dimulai dari atas, searah jarum jam (dihitung sekali per ukuran). */
	private static void buildRing(int r) {
		ringR = r;
		for (int i = 0; i < RING_N; i++) {
			double a = (Math.PI * 2.0 * i) / RING_N;
			RING_X[i] = (int) Math.round(Math.sin(a) * r) - 1;
			RING_Y[i] = (int) Math.round(-Math.cos(a) * r) - 1;
		}
	}
}
