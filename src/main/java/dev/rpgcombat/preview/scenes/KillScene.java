package dev.rpgcombat.preview.scenes;

import dev.rpgcombat.fx.FxConfig;
import dev.rpgcombat.fx.FxStyle;
import dev.rpgcombat.fx.FxStyles;
import dev.rpgcombat.preview.DollStage;
import dev.rpgcombat.preview.Dummy;
import dev.rpgcombat.preview.ParticlePool;
import dev.rpgcombat.preview.PreviewCanvas;
import dev.rpgcombat.preview.PreviewScene;
import dev.rpgcombat.ui.Draw;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Siklus: target muncul -> pemain memukul -> animasi kematian sesuai style -> kosong -> muncul lagi.
 *   burst  : kilatan putih lalu ledakan partikel
 *   soul   : target membiru, naik, dan memudar bersama jiwa yang naik
 *   static : target berkedip dengan petir pendek
 *   hearts : target memudar naik diiringi hati
 */
public final class KillScene implements PreviewScene {
	private static final float PERIOD = 4.0f;
	private static final float HIT_AT = 0.9f;
	private static final float DEATH_LEN = 1.5f;

	private final ParticlePool pool = new ParticlePool(300);
	private float clock;
	private boolean burstDone;
	private float lastPhase;
	private float emitAcc;

	@Override
	public void render(GuiGraphicsExtractor g, Minecraft mc, int x, int y, int w, int h, float dt, float time, float alpha) {
		FxConfig cfg = FxConfig.get();
		FxStyle st = FxStyles.find(FxStyles.KILLS, cfg.killStyle);
		String id = st.id();

		float s = Math.max(1.4f, Math.min(3.6f, Math.min((h - 70) / (float) DollStage.HEIGHT, w * 0.2f / DollStage.WIDTH)));
		float floor = y + h - PreviewCanvas.FLOOR_PAD;
		float attackerX = x + w * 0.28f;
		float dummyX = x + w * 0.70f;

		clock += dt;
		float phase = clock % PERIOD;
		if (phase < lastPhase) burstDone = false;
		lastPhase = phase;

		float attack = phase < HIT_AT ? Math.max(0f, (phase - 0.55f) / 0.35f) : Math.max(0f, 1f - (phase - HIT_AT) / 0.25f);
		float d = phase < HIT_AT ? -1f : (phase - HIT_AT) / DEATH_LEN; // <0 hidup; 0..1 sekarat; >1 sudah hilang
		float appear = phase > PERIOD - 0.6f ? (phase - (PERIOD - 0.6f)) / 0.6f : 1f;
		if (phase < 0.6f) appear = Math.min(1f, 0.3f + phase / 0.6f * 0.7f);

		ThreadLocalRandom r = ThreadLocalRandom.current();
		float cy = floor - 16f * s;

		// ----- ledakan / emisi sesuai style -----
		if (d >= 0f && !burstDone && ("burst".equals(id) || "static".equals(id))) {
			burstDone = true;
			int n = Math.round((id.equals("burst") ? 34 : 24) * cfg.killIntensity);
			for (int i = 0; i < n; i++) {
				double a = r.nextDouble() * Math.PI * 2;
				float k = st.speed() * (0.35f + r.nextFloat() * 0.9f);
				pool.spawn(dummyX, cy, (float) Math.cos(a) * k, (float) Math.sin(a) * k * 0.8f - 12f,
						st.life() * (0.6f + r.nextFloat() * 0.7f), st.size() + r.nextInt(2), st.colorA(), st.colorB(), st.shape());
			}
		}
		if (d >= 0f && d <= 1f && ("soul".equals(id) || "hearts".equals(id))) {
			emitAcc += dt * 34f * cfg.killIntensity;
			while (emitAcc >= 1f) {
				emitAcc -= 1f;
				pool.spawn(dummyX + (r.nextFloat() - 0.5f) * 8f * s, cy + (r.nextFloat() - 0.2f) * 14f * s,
						(r.nextFloat() - 0.5f) * st.spread(), -st.speed() * (0.5f + r.nextFloat()),
						st.life() * (0.7f + r.nextFloat() * 0.5f), st.size(), st.colorA(), st.colorB(), st.shape());
			}
		}
		pool.tick(dt, st.gravity() * 0.2f);

		// ----- gambar -----
		float atkAttack = phase < PERIOD - 0.6f ? attack : 0f;
		DollStage.draw(g, mc, attackerX, floor, s, time, 0f, 0f, alpha, atkAttack);

		if (d < 0f) {
			Dummy.draw(g, dummyX, floor, s, 0f, alpha * appear, 0f, 0x7A6FA0);
		} else if (d <= 1f) {
			switch (id) {
				case "burst" -> {
					if (d < 0.08f) Dummy.draw(g, dummyX, floor, s, 0f, alpha, 1f, 0x7A6FA0);
				}
				case "soul" -> {
					float rise = d * 22f * s;
					Dummy.draw(g, dummyX, floor - rise, s, 0f, alpha * (1f - d), 0.15f + d * 0.3f, 0x5B8BFF);
				}
				case "static" -> {
					if (d < 0.7f && ((int) (d * 22f)) % 2 == 0) Dummy.draw(g, dummyX, floor, s, 0f, alpha, 0.5f, 0x7A6FA0);
					drawArcs(g, dummyX, cy, s, d, alpha, st);
				}
				default -> { // hearts
					float rise = d * 14f * s;
					Dummy.draw(g, dummyX, floor - rise, s, 0f, alpha * (1f - d), 0.2f, 0xFF8FB5);
				}
			}
		}

		pool.draw(g, alpha, x + 4, y + 4, x + w - 4, y + h - 4);

		String name = net.minecraft.network.chat.Component.translatable(st.nameKey()).getString();
		Draw.text(g, mc.font, name, x + 10, y + h - 14, 0x9A93B0, alpha * 0.8f);
	}

	/** Petir pendek bergerigi di sekitar target (posisi acak-semu dari waktu; tanpa alokasi). */
	private void drawArcs(GuiGraphicsExtractor g, float cx, float cy, float s, float d, float alpha, FxStyle st) {
		if (d > 0.8f) return;
		float fade = (1f - d / 0.8f) * alpha;
		int col = Draw.argb(fade, st.colorB());
		for (int k = 0; k < 3; k++) {
			double a0 = clock * 9.0 + k * 2.1;
			int x0 = Math.round(cx);
			int y0 = Math.round(cy - 14f * s);
			int steps = 5;
			for (int i = 0; i < steps; i++) {
				int x1 = Math.round(cx + (float) Math.sin(a0 + i * 1.7) * 9f * s);
				int y1 = Math.round(cy - 14f * s + (i + 1) * (28f * s / steps));
				Draw.line(g, x0, y0, x1, y1, 1, col);
				x0 = x1;
				y0 = y1;
			}
		}
	}
}
