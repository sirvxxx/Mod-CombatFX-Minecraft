package dev.rpgcombat.preview;

import dev.rpghud.HudAlpha;
import dev.rpghud.SkinHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;

/**
 * Karakter pemain tampak depan, disusun dari tekstur skin (format 64x64) dengan matriks pose 2D.
 * Anggota badan berputar di sendi (bahu/pinggul), jadi animasi diam (napas, goyang) dan berjalan
 * bisa dibuat tanpa API render entity. Mengikuti skin pemain dan resource pack aktif.
 *
 * Koordinat dalam satuan piksel skin: kepala 8x8, badan 8x12, lengan 4x12, kaki 4x12 (tinggi total 32).
 */
public final class DollStage {
	private DollStage() {}

	private static final int TEX = 64;
	public static final int HEIGHT = 32; // piksel skin
	public static final int WIDTH = 16;

	/**
	 * @param cx    tengah horizontal
	 * @param footY y telapak kaki
	 * @param scale piksel layar per piksel skin
	 * @param time  detik, untuk animasi diam
	 * @param swing amplitudo ayun berjalan (radian), 0 = berdiri
	 * @param phase fase ayun berjalan (radian)
	 */
	public static void draw(GuiGraphicsExtractor g, Minecraft mc, float cx, float footY, float scale,
			float time, float swing, float phase, float alpha) {
		draw(g, mc, cx, footY, scale, time, swing, phase, alpha, 0f);
	}

	/** attack 0..1: lengan kiri (sisi kanan layar) terayun ke depan untuk menyerang. */
	public static void draw(GuiGraphicsExtractor g, Minecraft mc, float cx, float footY, float scale,
			float time, float swing, float phase, float alpha, float attack) {
		LocalPlayer p = mc.player;
		if (p == null || alpha <= 0.02f) return;
		Identifier skin = SkinHelper.bodyTexture(p);

		float bob = swing > 0f ? Math.abs((float) Math.sin(phase)) * 0.9f : (float) Math.sin(time * 1.6f) * 0.5f;
		float sway = (float) Math.sin(time * 0.7f) * 0.025f;
		float idleArm = (float) Math.sin(time * 1.3f) * 0.07f;
		float legSwing = swing * (float) Math.sin(phase);
		float armSwing = swing * 0.8f * (float) Math.sin(phase);

		HudAlpha.set(alpha);
		g.pose().pushMatrix();
		g.pose().translate(cx, footY);
		g.pose().rotate(sway);
		g.pose().translate(0f, -HEIGHT * scale - bob * scale);
		g.pose().scale(scale, scale);

		// urutan gambar: kaki, badan, lengan, kepala (lapisan luar menyusul tiap bagian)
		limb(g, skin, 4, 20, 4, 12, 4, 36, -2f, 20f, legSwing);       // kaki kanan (kiri di layar)
		limb(g, skin, 20, 52, 4, 12, 4, 52, 2f, 20f, -legSwing);      // kaki kiri
		g.pose().pushMatrix();
		HudAlpha.blit(g, skin, -4, 8, 20f, 20f, 8, 12, 8, 12, TEX, TEX);
		HudAlpha.blit(g, skin, -4, 8, 20f, 36f, 8, 12, 8, 12, TEX, TEX);
		g.pose().popMatrix();
		limb(g, skin, 44, 20, 4, 12, 44, 36, -6f, 8f, -armSwing - idleArm);  // lengan kanan
		limb(g, skin, 36, 52, 4, 12, 52, 52, 6f, 8f, armSwing + idleArm - attack * 2.2f); // lengan kiri (menyerang)
		HudAlpha.blit(g, skin, -4, 0, 8f, 8f, 8, 8, 8, 8, TEX, TEX);         // kepala
		HudAlpha.blit(g, skin, -4, 0, 40f, 8f, 8, 8, 8, 8, TEX, TEX);        // topi

		g.pose().popMatrix();
		HudAlpha.set(1.0f);
	}

	private static void limb(GuiGraphicsExtractor g, Identifier skin, int u, int v, int w, int h,
			int ou, int ov, float pivotX, float pivotY, float angle) {
		g.pose().pushMatrix();
		g.pose().translate(pivotX, pivotY);
		g.pose().rotate(angle);
		HudAlpha.blit(g, skin, -w / 2, 0, (float) u, (float) v, w, h, w, h, TEX, TEX);
		HudAlpha.blit(g, skin, -w / 2, 0, (float) ou, (float) ov, w, h, w, h, TEX, TEX);
		g.pose().popMatrix();
	}
}
