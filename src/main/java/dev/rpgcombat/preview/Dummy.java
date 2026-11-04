package dev.rpgcombat.preview;

import dev.rpgcombat.ui.Draw;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Target latihan sederhana (kepala, badan, lengan, kaki) dari persegi berwarna, dengan kilatan dan goyangan. */
public final class Dummy {
	private Dummy() {}

	/**
	 * @param cx    tengah
	 * @param footY telapak kaki
	 * @param s     skala (piksel layar per satuan; tinggi total 32)
	 * @param rot   rotasi (radian) di sekitar kaki
	 * @param flash 0..1 menuju putih
	 * @param tint  warna dasar badan (RGB)
	 */
	public static void draw(GuiGraphicsExtractor g, float cx, float footY, float s, float rot, float alpha, float flash, int tint) {
		if (alpha <= 0.02f) return;
		g.pose().pushMatrix();
		g.pose().translate(cx, footY);
		g.pose().rotate(rot);
		g.pose().scale(s, s);

		int body = mix(tint, 0xFFFFFF, flash);
		int dark = mix(darker(tint), 0xFFFFFF, flash);
		int head = mix(lighter(tint), 0xFFFFFF, flash);

		part(g, -4, -12, 0, 0, dark, alpha);      // kaki
		part(g, 0, -12, 4, 0, dark, alpha);
		part(g, -4, -24, 4, -12, body, alpha);    // badan
		part(g, -8, -24, -4, -12, dark, alpha);   // lengan
		part(g, 4, -24, 8, -12, dark, alpha);
		part(g, -4, -32, 4, -24, head, alpha);    // kepala
		part(g, -2, -29, -1, -28, 0x1B1530, alpha); // mata
		part(g, 1, -29, 2, -28, 0x1B1530, alpha);
		part(g, -4, -32, 4, -31, lighter(head), alpha * 0.6f);

		g.pose().popMatrix();
	}

	private static void part(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int rgb, float alpha) {
		g.fill(x0, y0, x1, y1, Draw.argb(alpha, rgb));
	}

	private static int mix(int a, int b, float t) {
		int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
		int gg = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
		int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
		return (r << 16) | (gg << 8) | bl;
	}

	private static int darker(int c) {
		return mix(c, 0x000000, 0.35f);
	}

	private static int lighter(int c) {
		return mix(c, 0xFFFFFF, 0.25f);
	}
}
