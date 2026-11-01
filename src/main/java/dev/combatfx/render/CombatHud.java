package dev.combatfx.render;

import dev.combatfx.Config;
import dev.combatfx.combat.ComboManager;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class CombatHud {
	private CombatHud() {}

	private static final Projector PROJ = new Projector();

	public static void extract(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) return;

		long now = Util.getMillis();
		drawIndicators(g, mc, player, deltaTracker.getGameTimeDeltaPartialTick(false), now);
		drawCombo(g, mc, now);
	}

	// ------------------------------------------------------------------
	// Floating damage numbers
	// ------------------------------------------------------------------
	private static void drawIndicators(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer player,
			float partialTick, long now) {
		List<FloatingText> list = IndicatorManager.list();
		if (list.isEmpty()) return;
		if (!mc.options.getCameraType().isFirstPerson()) return; // proyeksi hanya akurat di first-person

		Font font = mc.font;
		PROJ.begin(mc, player, partialTick);

		for (int i = 0, n = list.size(); i < n; i++) {
			FloatingText t = list.get(i);
			long age = now - t.spawnMs;
			if (age < 0 || age >= t.lifeMs) continue;

			float prog = (float) age / (float) t.lifeMs;

			// naik halus (ease-out cubic)
			float inv = 1f - prog;
			float ease = 1f - inv * inv * inv;
			float rise = ease * (t.crit ? Config.CRIT_RISE_BLOCKS : Config.NORMAL_RISE_BLOCKS);

			if (!PROJ.project(t.x, t.y + rise, t.z)) continue;

			// fade out di 45% terakhir
			float alpha = prog < 0.55f ? 1f : 1f - (prog - 0.55f) / 0.45f;
			int a = Math.max(5, Math.min(255, (int) (alpha * 255f))); // alpha <= 3 dibuang vanilla

			// ukuran mengikuti jarak, tapi di-clamp supaya tetap terbaca
			float scale = Mth.clamp(5.5f / PROJ.depth, 0.75f, 2.3f);

			// animasi "pop"
			float popT = Math.min(age / (t.crit ? 260f : 140f), 1f);
			float popEase = 1f - (1f - popT) * (1f - popT);
			scale *= t.crit ? Mth.lerp(popEase, 2.0f, 1.35f) : Mth.lerp(popEase, 0.55f, 1.0f);

			// crit: getar kecil di awal
			float shake = (t.crit && age < 220)
					? (float) Math.sin(age * 0.09) * 2.5f * (1f - age / 220f) : 0f;

			int w = font.width(t.text);
			int rgb = t.crit ? Config.COLOR_CRIT : Config.COLOR_NORMAL;

			g.pose().pushMatrix();
			g.pose().translate(PROJ.sx + shake, PROJ.sy);
			g.pose().scale(scale, scale);

			if (t.crit) {
				// outline hitam supaya lebih mencolok
				int ob = a << 24;
				g.text(font, t.text, -w / 2 - 1, -4, ob, false);
				g.text(font, t.text, -w / 2 + 1, -4, ob, false);
				g.text(font, t.text, -w / 2, -5, ob, false);
				g.text(font, t.text, -w / 2, -3, ob, false);
			}
			g.text(font, t.text, -w / 2, -4, (a << 24) | rgb, true); // true = drop shadow

			g.pose().popMatrix();
		}
	}

	// ------------------------------------------------------------------
	// Combo counter
	// ------------------------------------------------------------------
	private static void drawCombo(GuiGraphicsExtractor g, Minecraft mc, long now) {
		int combo = ComboManager.count(now);
		if (combo <= 0) return;

		Font font = mc.font;
		var window = mc.getWindow();

		float fade = Math.min(1f, ComboManager.remainingMs(now) / 250f);
		int a = Math.max(5, (int) (fade * 255f));

		// pop saat combo naik
		float popT = Math.min(ComboManager.sinceIncrementMs(now) / 220f, 1f);
		float pop = 1f + 0.55f * (1f - popT) * (1f - popT);

		// makin tinggi combo -> makin besar (cap di 40)
		float scale = (1.5f + Math.min(combo, 40) * 0.03f) * pop;
		int rgb = comboColor(combo, now);

		String label = ComboManager.label();
		float x = window.getGuiScaledWidth() / 2f + 34f; // di kanan crosshair
		float y = window.getGuiScaledHeight() / 2f - 8f;
		float labelW = font.width(label) * scale;

		// angka "x12"
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.text(font, label, 0, 0, (a << 24) | rgb, true);
		g.pose().popMatrix();

		// tulisan kecil "COMBO"
		g.pose().pushMatrix();
		g.pose().translate(x + labelW + 3f, y + 2f * scale);
		g.pose().scale(0.75f, 0.75f);
		g.text(font, "COMBO", 0, 0, (a << 24) | 0xFFFFFF, true);
		g.pose().popMatrix();

		// progress bar sisa waktu combo
		int barY = (int) (y + 9f * scale + 2f);
		int barX = (int) x;
		int barW = Math.max(12, (int) labelW);
		int filled = (int) (barW * ComboManager.remainingFraction(now));
		g.fill(barX, barY, barX + barW, barY + 2, ((a * 140 / 255) << 24));
		g.fill(barX, barY, barX + filled, barY + 2, (a << 24) | rgb);
	}

	private static int comboColor(int combo, long now) {
		if (combo < 5) return 0xFFFFFF;   // putih
		if (combo < 10) return 0xFFE066;  // kuning
		if (combo < 20) return 0xFFA032;  // oranye
		if (combo < 30) return 0xFF4B3C;  // merah
		return rainbow(now);              // 30+ : pelangi
	}

	private static int rainbow(long now) {
		float h6 = ((now % 1500L) / 1500f) * 6f;
		int i = (int) h6;
		float f = h6 - i;
		float q = 1f - f;
		float r, gr, b;
		switch (i % 6) {
			case 0 -> { r = 1; gr = f; b = 0; }
			case 1 -> { r = q; gr = 1; b = 0; }
			case 2 -> { r = 0; gr = 1; b = f; }
			case 3 -> { r = 0; gr = q; b = 1; }
			case 4 -> { r = f; gr = 0; b = 1; }
			default -> { r = 1; gr = 0; b = q; }
		}
		return ((int) (r * 255) << 16) | ((int) (gr * 255) << 8) | (int) (b * 255);
	}
}
