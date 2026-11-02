package dev.combatfx.render;

import dev.combatfx.CombatConfig;
import dev.combatfx.combat.ComboManager;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Semua animasi hanya interpolasi sederhana berdasarkan umur (ms).
 * Tidak ada alokasi objek per frame.
 */
public final class CombatHud {
	private CombatHud() {}

	private static final Projector PROJ = new Projector();

	public static void extract(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
		CombatConfig cfg = CombatConfig.get();
		if (!cfg.enabled) return;

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) return;

		long now = System.currentTimeMillis();
		if (cfg.showDamageNumbers) {
			drawIndicators(g, mc, player, cfg, deltaTracker.getGameTimeDeltaPartialTick(false), now);
		}
		if (cfg.showCombo) {
			drawCombo(g, mc, cfg, now);
		}
	}

	// ==================================================================
	// Floating damage numbers
	// ==================================================================
	private static void drawIndicators(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer player,
			CombatConfig cfg, float partialTick, long now) {
		List<FloatingText> list = IndicatorManager.list();
		if (list.isEmpty()) return;
		if (!mc.options.getCameraType().isFirstPerson()) return;

		Font font = mc.font;
		PROJ.begin(mc, player, partialTick);

		for (int i = 0, n = list.size(); i < n; i++) {
			FloatingText t = list.get(i);
			Tier tier = t.tier;
			long age = now - t.spawnMs;
			if (age < 0 || age >= t.lifeMs) continue;

			float prog = (float) age / (float) t.lifeMs;

			float inv = 1f - prog;
			float ease = 1f - inv * inv * inv; // ease-out kubik

			double wx = t.x + t.driftX * ease;
			double wy = t.y + tier.riseBlocks * ease;
			double wz = t.z + t.driftZ * ease;
			if (!PROJ.project(wx, wy, wz)) continue;

			float alpha = (prog < 0.6f ? 1f : 1f - (prog - 0.6f) / 0.4f) * cfg.numberOpacity;
			int a = Math.max(5, Math.min(255, (int) (alpha * 255f)));

			// pop dua fase: 0-70ms membesar cepat, 70-230ms menyusut ke normal
			float pop;
			if (age < 70L) {
				pop = Mth.lerp(age / 70f, 0.35f, tier.popPeak);
			} else if (age < 230L) {
				pop = Mth.lerp((age - 70L) / 160f, tier.popPeak, 1f);
			} else {
				pop = 1f;
			}

			float distScale = Mth.clamp(5.5f / PROJ.depth, 0.75f, 2.3f);
			float scale = distScale * tier.baseScale * pop * cfg.numberScale;

			float shake = 0f;
			if (tier == Tier.CRIT && age < 240L) {
				shake = (float) Math.sin(age * 0.1) * 2.5f * (1f - age / 240f);
			}

			int w = font.width(t.text);
			int color = (a << 24) | tier.rgb();

			g.pose().pushMatrix();
			g.pose().translate(PROJ.sx + shake, PROJ.sy);
			g.pose().scale(scale, scale);

			if (tier.outline && cfg.numberOutline) {
				int ob = a << 24;
				g.text(font, t.text, -w / 2 - 1, -4, ob, false);
				g.text(font, t.text, -w / 2 + 1, -4, ob, false);
				g.text(font, t.text, -w / 2, -5, ob, false);
				g.text(font, t.text, -w / 2, -3, ob, false);
			}
			g.text(font, t.text, -w / 2, -4, color, true);

			g.pose().popMatrix();
		}
	}

	// ==================================================================
	// Combo counter
	// ==================================================================
	private static void drawCombo(GuiGraphicsExtractor g, Minecraft mc, CombatConfig cfg, long now) {
		int combo = ComboManager.count(now);
		if (combo <= 0) return;

		Font font = mc.font;
		var window = mc.getWindow();

		long remain = ComboManager.remainingMs(now);
		float fade = Math.min(1f, remain / 250f);
		int a = Math.max(5, (int) (fade * cfg.comboOpacity * 255f));

		float pulse = 1f;
		if (cfg.comboPulse) {
			float pt = Math.min(ComboManager.sinceIncrementMs(now) / 260f, 1f);
			pulse = 1f + 0.55f * (1f - pt) * (1f - pt);
			if (combo >= 10) pulse += 0.04f * (float) Math.sin(now * 0.014);
		}

		float scale = (1.5f + Math.min(combo, 40) * 0.03f) * pulse * cfg.comboScale;
		int rgb = comboColor(combo, now);

		String label = ComboManager.label();
		float x = window.getGuiScaledWidth() / 2f + cfg.comboOffsetX;
		float y = window.getGuiScaledHeight() / 2f + cfg.comboOffsetY;
		float labelW = font.width(label) * scale;

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		if (combo >= 10) {
			int ob = (a * 200 / 255) << 24;
			g.text(font, label, -1, 0, ob, false);
			g.text(font, label, 1, 0, ob, false);
			g.text(font, label, 0, -1, ob, false);
			g.text(font, label, 0, 1, ob, false);
		}
		g.text(font, label, 0, 0, (a << 24) | rgb, true);
		g.pose().popMatrix();

		g.pose().pushMatrix();
		g.pose().translate(x + labelW + 3f, y + 2f * scale);
		g.pose().scale(0.75f, 0.75f);
		g.text(font, "COMBO", 0, 0, (a << 24) | 0xFFFFFF, true);
		g.pose().popMatrix();

		if (!cfg.showComboBar) return;

		int barW = Math.max(40, (int) labelW);
		int barH = 3;
		int barX = (int) x;
		int barY = (int) (y + 9f * scale + 3f);
		float frac = ComboManager.remainingFraction(now);
		int filled = Math.round(barW * frac);

		int barRgb = rgb;
		if (frac < 0.3f) {
			barRgb = lerpColor(0xFF3030, rgb, frac / 0.3f);
		}

		int bgA = a * 150 / 255;
		g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, (bgA << 24));
		g.fill(barX, barY, barX + barW, barY + barH, ((bgA * 2 / 3) << 24) | 0x202020);
		if (filled > 0) {
			g.fill(barX, barY, barX + filled, barY + barH, (a << 24) | barRgb);
			g.fill(barX, barY, barX + filled, barY + 1, (a << 24) | brighten(barRgb));
		}
	}

	private static int comboColor(int combo, long now) {
		if (combo < 5) return 0xB8C0D0;
		if (combo < 10) return 0xFFE066;
		if (combo < 20) return 0xFFA032;
		if (combo < 30) return 0xFF5A3C;
		return rainbow(now);
	}

	private static int lerpColor(int from, int to, float t) {
		int r = (int) Mth.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
		int gr = (int) Mth.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
		int b = (int) Mth.lerp(t, from & 0xFF, to & 0xFF);
		return (r << 16) | (gr << 8) | b;
	}

	private static int brighten(int rgb) {
		return lerpColor(rgb, 0xFFFFFF, 0.45f);
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
