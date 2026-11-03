package dev.rpgcombat.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Primitif gambar menu. Hanya memakai fill/text/pose (sudah terbukti jalan di 26.2).
 * Sudut membulat dibuat dari inset per baris; biayanya O(radius) fill per bentuk.
 */
public final class Draw {
	private Draw() {}

	public static int argb(float alpha, int rgb) {
		int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
		return (a << 24) | (rgb & 0xFFFFFF);
	}

	// ------------------------------------------------------------ shapes
	private static int inset(int r, int i) {
		double dy = r - i - 0.5;
		return r - (int) Math.round(Math.sqrt(Math.max(0.0, r * r - dy * dy)));
	}

	public static void roundRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int argb) {
		if (w <= 0 || h <= 0) return;
		r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
		g.fill(x, y + r, x + w, y + h - r, argb);
		for (int i = 0; i < r; i++) {
			int in = inset(r, i);
			g.fill(x + in, y + i, x + w - in, y + i + 1, argb);
			g.fill(x + in, y + h - i - 1, x + w - in, y + h - i, argb);
		}
	}

	/** Garis tepi membulat setebal t px. */
	public static void roundBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int t, int argb) {
		if (w <= 0 || h <= 0) return;
		r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
		for (int i = 0; i < r; i++) {
			int in = inset(r, i);
			int prev = i == 0 ? r : inset(r, i - 1);
			int run = Math.max(t, prev - in + t);
			if (i < t) {
				g.fill(x + in, y + i, x + w - in, y + i + 1, argb);
				g.fill(x + in, y + h - i - 1, x + w - in, y + h - i, argb);
			} else {
				g.fill(x + in, y + i, x + in + run, y + i + 1, argb);
				g.fill(x + w - in - run, y + i, x + w - in, y + i + 1, argb);
				g.fill(x + in, y + h - i - 1, x + in + run, y + h - i, argb);
				g.fill(x + w - in - run, y + h - i - 1, x + w - in, y + h - i, argb);
			}
		}
		g.fill(x, y + r, x + t, y + h - r, argb);
		g.fill(x + w - t, y + r, x + w, y + h - r, argb);
	}

	/** Panel kaca: isi transparan + kilau atas + tepi tipis. (Blur sungguhan tidak tersedia di pass GUI.) */
	public static void glass(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, float alpha, int tint, int borderRgb) {
		roundRect(g, x, y, w, h, r, argb(0.66f * alpha, tint));
		int sheen = Math.max(2, Math.min(h / 3, 14));
		for (int i = 0; i < sheen; i++) {
			float k = 1f - i / (float) sheen;
			g.fill(x + r, y + 1 + i, x + w - r, y + 2 + i, argb(0.07f * k * alpha, 0xFFFFFF));
		}
		roundBorder(g, x, y, w, h, r, 1, argb(0.55f * alpha, borderRgb));
	}

	public static void shadow(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, float strength) {
		for (int i = 4; i >= 1; i--) {
			roundRect(g, x - i, y - i + 3, w + 2 * i, h + 2 * i, r + i, argb(0.06f * strength, 0x000000));
		}
	}

	// -------------------------------------------------------------- text
	public static void text(GuiGraphicsExtractor g, Font f, String s, int x, int y, int rgb, float alpha) {
		if (alpha <= 0.02f) return;
		g.text(f, s, x, y, argb(Math.max(alpha, 0.03f), rgb), true);
	}

	public static void textCentered(GuiGraphicsExtractor g, Font f, String s, int cx, int y, int rgb, float alpha) {
		text(g, f, s, cx - f.width(s) / 2, y, rgb, alpha);
	}

	/** Teks berskala; (x, y) = kiri-atas. */
	public static void textScaled(GuiGraphicsExtractor g, Font f, String s, float x, float y, float scale, int rgb, float alpha) {
		if (alpha <= 0.02f) return;
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.text(f, s, 0, 0, argb(Math.max(alpha, 0.03f), rgb), true);
		g.pose().popMatrix();
	}

	// ------------------------------------------------------------- lines
	public static void line(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int t, int argb) {
		int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
		int err = dx + dy;
		for (int guard = 0; guard < 512; guard++) {
			g.fill(x0, y0, x0 + t, y0 + t, argb);
			if (x0 == x1 && y0 == y1) break;
			int e2 = 2 * err;
			if (e2 >= dy) { err += dy; x0 += sx; }
			if (e2 <= dx) { err += dx; y0 += sy; }
		}
	}

	/** Busur elips (derajat searah jarum jam dari atas). */
	public static void arc(GuiGraphicsExtractor g, int cx, int cy, float rx, float ry, int t, int argb, float fromDeg, float spanDeg) {
		int n = Math.max(12, Math.min(240, (int) (Math.toRadians(Math.abs(spanDeg)) * Math.max(rx, ry) / 1.4f)));
		for (int i = 0; i <= n; i++) {
			double a = Math.toRadians(fromDeg + spanDeg * i / n);
			int px = cx + (int) Math.round(Math.sin(a) * rx);
			int py = cy - (int) Math.round(Math.cos(a) * ry);
			g.fill(px, py, px + t, py + t, argb);
		}
	}

	// ------------------------------------------------------------- icons
	/** Ikon vektor kecil dari garis (tanpa tekstur). size = jari-jari kira-kira. */
	public static void icon(GuiGraphicsExtractor g, Icon icon, int cx, int cy, int s, int rgb, float alpha) {
		int c = argb(alpha, rgb);
		int t = s >= 9 ? 2 : 1;
		switch (icon) {
			case SWORD -> {
				line(g, cx - s, cy + s, cx + s, cy - s, t, c);
				line(g, cx - s / 2, cy + s / 2 + 1, cx + s / 2 + 1, cy - s / 2, 1, c);
				line(g, cx - s, cy + s / 3, cx - s / 3, cy + s, t, c);
			}
			case STAR -> {
				line(g, cx - s, cy, cx + s, cy, t, c);
				line(g, cx, cy - s, cx, cy + s, t, c);
				line(g, cx - s * 2 / 3, cy - s * 2 / 3, cx + s * 2 / 3, cy + s * 2 / 3, 1, c);
				line(g, cx - s * 2 / 3, cy + s * 2 / 3, cx + s * 2 / 3, cy - s * 2 / 3, 1, c);
			}
			case HUD -> {
				g.fill(cx - s, cy - s * 2 / 3, cx + s, cy - s * 2 / 3 + t, c);
				g.fill(cx - s, cy + s * 2 / 3, cx + s, cy + s * 2 / 3 + t, c);
				g.fill(cx - s, cy - s * 2 / 3, cx - s + t, cy + s * 2 / 3 + t, c);
				g.fill(cx + s - t, cy - s * 2 / 3, cx + s, cy + s * 2 / 3 + t, c);
				g.fill(cx - s / 2, cy - s / 4, cx + s / 3, cy - s / 4 + t, c);
			}
			case GEAR -> {
				arc(g, cx, cy, s * 0.7f, s * 0.7f, t, c, 0f, 360f);
				for (int i = 0; i < 8; i++) {
					double a = Math.PI * i / 4.0;
					int px = cx + (int) Math.round(Math.sin(a) * (s + 1));
					int py = cy - (int) Math.round(Math.cos(a) * (s + 1));
					g.fill(px, py, px + t, py + t, c);
				}
				g.fill(cx, cy, cx + t, cy + t, c);
			}
			case BOLT -> {
				line(g, cx + s / 3, cy - s, cx - s / 2, cy + 1, t, c);
				line(g, cx - s / 2, cy + 1, cx + s / 2, cy, t, c);
				line(g, cx + s / 2, cy, cx - s / 3, cy + s, t, c);
			}
			case SPARK -> {
				line(g, cx, cy - s, cx, cy + s, t, c);
				line(g, cx - s, cy, cx + s, cy, t, c);
				g.fill(cx - s / 2, cy - s / 2, cx - s / 2 + 1, cy - s / 2 + 1, c);
				g.fill(cx + s / 2, cy + s / 2, cx + s / 2 + 1, cy + s / 2 + 1, c);
			}
		}
	}
}
