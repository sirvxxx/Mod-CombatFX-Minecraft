package dev.rpghud;

import java.lang.reflect.Method;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Opacity per elemen HUD.
 *
 * Sebelum menggambar sebuah elemen, panggil set(opacity); semua fill / teks / blit yang lewat
 * helper ini otomatis ikut transparan. Setelah selesai panggil set(1f).
 *
 * Teks dan fill: alpha warna dikalikan opacity.
 * Tekstur (bingkai, ikon, wajah): memakai overload blit dengan parameter warna tint (ARGB) yang dicari
 * lewat reflection. Kalau overload itu tidak ada di versi game ini, tekstur digambar penuh
 * (hanya teks dan isi yang transparan).
 */
public final class HudAlpha {
	private HudAlpha() {}

	private static float a = 1f;
	private static boolean searched;
	private static Method tintBlit;

	public static void set(float opacity) {
		a = opacity < 0f ? 0f : (opacity > 1f ? 1f : opacity);
	}

	public static float get() {
		return a;
	}

	public static int c(int argb) {
		if (a >= 0.999f) return argb;
		int al = Math.round((argb >>> 24) * a);
		return (al << 24) | (argb & 0xFFFFFF);
	}

	public static void fill(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
		g.fill(x1, y1, x2, y2, c(color));
	}

	public static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
		g.outline(x, y, w, h, c(color));
	}

	public static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color, boolean shadow) {
		g.text(font, s, x, y, c(color), shadow);
	}

	public static void blit(GuiGraphicsExtractor g, Identifier tex, int x, int y, float u, float v,
			int w, int h, int rw, int rh, int tw, int th) {
		if (a < 0.999f) {
			if (!searched) {
				searched = true;
				find();
			}
			if (tintBlit != null) {
				try {
					int tint = (Math.round(a * 255f) << 24) | 0xFFFFFF;
					tintBlit.invoke(g, RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, rw, rh, tw, th, tint);
					return;
				} catch (Throwable t) {
					tintBlit = null;
				}
			}
		}
		g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, rw, rh, tw, th);
	}

	/** blit(pipeline, Identifier, int x, int y, float u, float v, int w, int h, int rw, int rh, int tw, int th, int color) */
	private static void find() {
		for (Method m : GuiGraphicsExtractor.class.getMethods()) {
			if (!m.getName().equals("blit")) continue;
			Class<?>[] p = m.getParameterTypes();
			if (p.length != 13) continue;
			if (!p[0].isInstance(RenderPipelines.GUI_TEXTURED)) continue;
			if (p[1] != Identifier.class) continue;
			if (p[2] != int.class || p[3] != int.class || p[4] != float.class || p[5] != float.class) continue;
			boolean ints = true;
			for (int i = 6; i <= 12; i++) {
				if (p[i] != int.class) ints = false;
			}
			if (ints) {
				tintBlit = m;
				return;
			}
		}
	}
}
