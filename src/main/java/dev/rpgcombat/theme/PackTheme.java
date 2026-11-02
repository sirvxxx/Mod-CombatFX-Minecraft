package dev.rpgcombat.theme;

import dev.rpghud.HudAlpha;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

/**
 * Tema otomatis dari resource pack aktif.
 *
 * Yang dibaca: textures/gui/container/inventory.png (panel GUI utama; hampir semua pack mengubahnya).
 *  - warna dominan panel  -> dipakai sebagai warna latar HUD (digelapkan seperlunya supaya teks terbaca)
 *  - tepi 4px panel        -> digambar ulang sebagai bingkai HUD (jadi tekstur kayu/slate/dll identik dengan pack)
 *  - klasifikasi gaya      -> LIGHT (vanilla abu terang), SLATE (gelap), WOOD (cokelat)
 *
 * Dibaca sekali saat start dan setiap kali daftar pack aktif berubah (tunda 4 detik sampai reload selesai).
 * Kalau gagal membaca apa pun, dipakai tampilan gelap bawaan.
 */
public final class PackTheme {
	private PackTheme() {}

	public enum Style { LIGHT, SLATE, WOOD }

	private static final Identifier INVENTORY = Identifier.withDefaultNamespace("textures/gui/container/inventory.png");
	private static final int PANEL_W = 176;
	private static final int PANEL_H = 166;
	private static final int B = 4; // tebal bingkai di layar (px GUI)

	private static Style style = Style.SLATE;
	private static int baseRgb = 0x2A2F38;
	private static int fillArgb = 0xE0262B33;
	private static boolean haveTex;
	private static int texW = 256;
	private static int texH = 256;
	private static int bt = 4; // tebal bingkai dalam piksel tekstur
	private static float scale = 1f;

	private static boolean loaded;
	private static int lastSig = Integer.MIN_VALUE;
	private static long reloadAt = -1;
	private static long ticks;

	public static Style style() {
		return style;
	}

	public static int baseRgb() {
		return baseRgb;
	}

	// ---------------------------------------------------------------- refresh
	public static void tick(Minecraft mc) {
		ticks++;
		if (!loaded && mc.getResourceManager() != null) {
			refresh(mc);
		}
		if (ticks % 20 == 0) {
			int sig = packSignature(mc);
			if (sig != lastSig) {
				if (lastSig != Integer.MIN_VALUE) reloadAt = ticks + 80; // tunggu reload resource selesai
				lastSig = sig;
			}
		}
		if (reloadAt > 0 && ticks >= reloadAt) {
			reloadAt = -1;
			refresh(mc);
		}
	}

	/** Hash daftar pack aktif lewat reflection (nama API ini sering berubah antar versi). */
	private static int packSignature(Minecraft mc) {
		try {
			Method repoM = Minecraft.class.getMethod("getResourcePackRepository");
			Object repo = repoM.invoke(mc);
			Method idsM = repo.getClass().getMethod("getSelectedIds");
			Object ids = idsM.invoke(repo);
			return ids instanceof Collection<?> c ? c.hashCode() : 0;
		} catch (Throwable t) {
			return 0;
		}
	}

	public static void refresh(Minecraft mc) {
		loaded = true;
		haveTex = false;
		try {
			Optional<Resource> res = mc.getResourceManager().getResource(INVENTORY);
			if (res.isEmpty()) {
				useDefaults();
				return;
			}
			try (InputStream in = res.get().open()) {
				BufferedImage img = ImageIO.read(in);
				if (img == null) {
					useDefaults();
					return;
				}
				analyze(img);
			}
		} catch (Throwable t) {
			useDefaults();
		}
	}

	private static void useDefaults() {
		style = Style.SLATE;
		baseRgb = 0x2A2F38;
		fillArgb = 0xE0262B33;
		haveTex = false;
	}

	private static void analyze(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();
		scale = w / 256f;
		texW = w;
		texH = h;
		bt = Math.max(1, Math.round(B * scale));

		// 1) warna dominan di bagian dalam panel (histogram 5-bit per kanal)
		int x0 = Math.round(8 * scale), x1 = Math.round((PANEL_W - 8) * scale);
		int y0 = Math.round(8 * scale), y1 = Math.round((PANEL_H - 8) * scale);
		int[] count = new int[32768];
		long[] sumR = new long[32768];
		long[] sumG = new long[32768];
		long[] sumB = new long[32768];
		for (int y = y0; y < y1 && y < h; y++) {
			for (int x = x0; x < x1 && x < w; x++) {
				int argb = img.getRGB(x, y);
				if (((argb >>> 24) & 0xFF) < 200) continue;
				int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
				int key = ((r >> 3) << 10) | ((g >> 3) << 5) | (b >> 3);
				count[key]++;
				sumR[key] += r;
				sumG[key] += g;
				sumB[key] += b;
			}
		}
		int best = -1;
		for (int i = 0; i < count.length; i++) {
			if (best < 0 || count[i] > count[best]) best = i;
		}
		if (best < 0 || count[best] == 0) {
			useDefaults();
			return;
		}
		int r = (int) (sumR[best] / count[best]);
		int g = (int) (sumG[best] / count[best]);
		int b = (int) (sumB[best] / count[best]);
		baseRgb = (r << 16) | (g << 8) | b;

		// 2) klasifikasi gaya
		float lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
		float max = Math.max(r, Math.max(g, b)) / 255f;
		float min = Math.min(r, Math.min(g, b)) / 255f;
		float sat = max <= 0f ? 0f : (max - min) / max;
		float hue = hueDeg(r, g, b);
		if (sat >= 0.20f && hue >= 10f && hue <= 55f && lum >= 0.15f && lum <= 0.75f) {
			style = Style.WOOD;
		} else if (lum < 0.50f) {
			style = Style.SLATE;
		} else {
			style = Style.LIGHT;
		}

		// 3) warna isi HUD: dijaga cukup gelap supaya teks terang tetap terbaca
		float k = lum > 0.26f ? 0.26f / lum : 1f;
		int fr = Math.min(255, Math.round(r * k));
		int fg = Math.min(255, Math.round(g * k));
		int fb = Math.min(255, Math.round(b * k));
		fillArgb = 0xE0000000 | (fr << 16) | (fg << 8) | fb;

		haveTex = w >= 128 && h >= 128;
	}

	private static float hueDeg(int r, int g, int b) {
		float rf = r / 255f, gf = g / 255f, bf = b / 255f;
		float max = Math.max(rf, Math.max(gf, bf));
		float min = Math.min(rf, Math.min(gf, bf));
		float d = max - min;
		if (d < 1e-4f) return 0f;
		float h;
		if (max == rf) h = ((gf - bf) / d) % 6f;
		else if (max == gf) h = (bf - rf) / d + 2f;
		else h = (rf - gf) / d + 4f;
		h *= 60f;
		return h < 0 ? h + 360f : h;
	}

	// ---------------------------------------------------------------- drawing
	/** Panel HUD lengkap: bayangan + isi + bingkai dari resource pack. */
	public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		HudAlpha.fill(g, x + 1, y + 1, x + w + 3, y + h + 3, 0x22000000);
		HudAlpha.fill(g, x + 2, y + 3, x + w + 2, y + h + 2, 0x44000000);
		HudAlpha.fill(g, x, y, x + w, y + h, fillArgb);
		border(g, x, y, w, h);
	}

	/** Hanya bingkai (tidak menutup isi). */
	public static void border(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		if (!haveTex || w <= 2 * B || h <= 2 * B) {
			int light = lighten(baseRgb, 0.35f);
			int dark = darken(baseRgb, 0.45f);
			HudAlpha.fill(g, x, y, x + w, y + 1, 0xFF000000 | light);
			HudAlpha.fill(g, x, y, x + 1, y + h, 0xFF000000 | light);
			HudAlpha.fill(g, x, y + h - 1, x + w, y + h, 0xFF000000 | dark);
			HudAlpha.fill(g, x + w - 1, y, x + w, y + h, 0xFF000000 | dark);
			return;
		}
		int rw = Math.round(PANEL_W * scale); // lebar/tinggi panel dalam piksel tekstur
		int rh = Math.round(PANEL_H * scale);
		int right = rw - bt;
		int bottom = rh - bt;

		// sudut
		HudAlpha.blit(g, INVENTORY, x, y, 0f, 0f, B, B, bt, bt, texW, texH);
		HudAlpha.blit(g, INVENTORY, x + w - B, y, (float) right, 0f, B, B, bt, bt, texW, texH);
		HudAlpha.blit(g, INVENTORY, x, y + h - B, 0f, (float) bottom, B, B, bt, bt, texW, texH);
		HudAlpha.blit(g, INVENTORY, x + w - B, y + h - B, (float) right, (float) bottom, B, B, bt, bt, texW, texH);
		// sisi
		HudAlpha.blit(g, INVENTORY, x + B, y, (float) bt, 0f, w - 2 * B, B, rw - 2 * bt, bt, texW, texH);
		HudAlpha.blit(g, INVENTORY, x + B, y + h - B, (float) bt, (float) bottom, w - 2 * B, B, rw - 2 * bt, bt, texW, texH);
		HudAlpha.blit(g, INVENTORY, x, y + B, 0f, (float) bt, B, h - 2 * B, bt, rh - 2 * bt, texW, texH);
		HudAlpha.blit(g, INVENTORY, x + w - B, y + B, (float) right, (float) bt, B, h - 2 * B, bt, rh - 2 * bt, texW, texH);
	}

	/** Warna latar menu (sedikit lebih terang dari isi HUD). */
	public static int menuFill() {
		return 0xE6000000 | (fillArgb & 0xFFFFFF);
	}

	private static int lighten(int rgb, float t) {
		int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
		return ((int) (r + (255 - r) * t) << 16) | ((int) (g + (255 - g) * t) << 8) | (int) (b + (255 - b) * t);
	}

	private static int darken(int rgb, float t) {
		int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
		return ((int) (r * (1 - t)) << 16) | ((int) (g * (1 - t)) << 8) | (int) (b * (1 - t));
	}
}
