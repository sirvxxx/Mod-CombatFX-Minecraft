package dev.rpgcombat.preview;

import dev.rpgcombat.ui.Draw;
import dev.rpgcombat.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Bingkai preview: latar gradasi, lantai, label "live", lalu menjalankan sebuah PreviewScene. */
public final class PreviewCanvas {
	private PreviewCanvas() {}

	/** Jarak lantai dari tepi bawah kanvas. */
	public static final int FLOOR_PAD = 26;

	public static void render(GuiGraphicsExtractor g, Minecraft mc, PreviewScene scene, int x, int y, int w, int h,
			float a, float dt, float time) {
		Font font = mc.font;
		Draw.shadow(g, x, y, w, h, Theme.RADIUS, a * 0.7f);
		Draw.glass(g, x, y, w, h, Theme.RADIUS, a, 0x080612, 0x6E6788);

		// latar gradasi vertikal (gelap di atas, ungu redup di dekat lantai)
		int floor = y + h - FLOOR_PAD;
		for (int yy = y + 3; yy < y + h - 3; yy += 3) {
			float k = (yy - y) / (float) h;
			g.fill(x + 3, yy, x + w - 3, Math.min(yy + 3, y + h - 3), Draw.argb(a * (0.04f + 0.16f * k * k), Theme.ACCENT_DIM));
		}
		g.fill(x + 6, floor, x + w - 6, floor + 1, Draw.argb(0.45f * a, Theme.ACCENT));
		g.fill(x + 6, floor + 1, x + w - 6, floor + 2, Draw.argb(0.12f * a, Theme.ACCENT));

		scene.render(g, mc, x, y, w, h, dt, time, a);

		// label
		Draw.text(g, font, Component.translatable("rpgcombat.menu.preview").getString(), x + 10, y + 8, Theme.GOLD, a);
		float pulse = 0.55f + 0.45f * (float) Math.sin(time * 4f);
		int lx = x + w - 10 - font.width("LIVE");
		Draw.roundRect(g, lx - 10, y + 10, 5, 5, 2, Draw.argb(a * pulse, Theme.GOOD));
		Draw.text(g, font, "LIVE", lx, y + 8, Theme.TEXT_DIM, a);
	}
}
