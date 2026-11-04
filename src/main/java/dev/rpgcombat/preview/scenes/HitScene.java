package dev.rpgcombat.preview.scenes;

import dev.combatfx.CombatConfig;
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
 * Pemain memukul target latihan terus-menerus. Tiap pukulan memunculkan partikel hit (style terpilih)
 * dan angka damage melayang memakai WARNA yang sedang diatur di fitur Damage Numbers.
 * Setiap pukulan ketiga adalah critical.
 */
public final class HitScene implements PreviewScene {
	private static final float PERIOD = 1.3f;
	private static final String[] NORMAL = {"6", "7", "5", "8"};

	private final ParticlePool pool = new ParticlePool(240);
	private float clock;
	private float lastPhase;
	private boolean fired;
	private int hits;
	private float shake;

	// angka melayang (ring buffer kecil)
	private final String[] numText = new String[4];
	private final boolean[] numCrit = new boolean[4];
	private final float[] numAge = new float[4];
	private final float[] numX = new float[4];
	private int numHead;

	@Override
	public void render(GuiGraphicsExtractor g, Minecraft mc, int x, int y, int w, int h, float dt, float time, float alpha) {
		FxConfig cfg = FxConfig.get();
		CombatConfig cc = CombatConfig.get();
		FxStyle st = FxStyles.find(FxStyles.HITS, cfg.hitStyle);

		float s = Math.max(1.4f, Math.min(3.6f, Math.min((h - 70) / (float) DollStage.HEIGHT, w * 0.2f / DollStage.WIDTH)));
		float floor = y + h - PreviewCanvas.FLOOR_PAD;
		float attackerX = x + w * 0.30f;
		float dummyX = x + w * 0.70f;

		clock += dt;
		float phase = clock % PERIOD;
		if (phase < lastPhase) fired = false;
		lastPhase = phase;
		float attack = phase < 0.32f ? phase / 0.32f : Math.max(0f, 1f - (phase - 0.32f) / 0.28f);
		if (!fired && phase >= 0.30f) {
			fired = true;
			hit(cfg, st, dummyX, floor - 18f * s);
		}
		shake = Math.max(0f, shake - dt * 4.5f);
		for (int i = 0; i < numAge.length; i++) {
			if (numText[i] != null) numAge[i] += dt;
		}

		pool.tick(dt, st.gravity());

		DollStage.draw(g, mc, attackerX, floor, s, time, 0f, 0f, alpha, attack);
		float rot = (float) Math.sin(clock * 55f) * 0.14f * shake;
		float flash = Math.max(0f, shake - 0.55f) * 2f;
		Dummy.draw(g, dummyX + shake * 3f, floor, s, rot, alpha, flash, 0x7A6FA0);

		pool.draw(g, alpha, x + 4, y + 4, x + w - 4, y + h - 4);

		for (int i = 0; i < numAge.length; i++) {
			if (numText[i] == null) continue;
			float age = numAge[i];
			float life = numCrit[i] ? 1.1f : 0.9f;
			if (age >= life) {
				numText[i] = null;
				continue;
			}
			float k = age / life;
			float pop = age < 0.07f ? 0.4f + age / 0.07f * (numCrit[i] ? 1.1f : 0.7f) : (age < 0.2f ? (numCrit[i] ? 1.5f : 1.1f) - (age - 0.07f) * 0.5f : 1f);
			float sc = (numCrit[i] ? 1.5f : 1.0f) * Math.max(0.5f, pop);
			float ny = floor - 30f * s - (1f - (1f - k) * (1f - k) * (1f - k)) * 26f;
			float a = (k < 0.6f ? 1f : 1f - (k - 0.6f) / 0.4f) * alpha;
			int rgb = numCrit[i] ? cc.colorCrit : cc.colorNormal;
			float tw = mc.font.width(numText[i]) * sc;
			Draw.textScaled(g, mc.font, numText[i], numX[i] - tw / 2f, ny, sc, rgb, a);
		}
	}

	private void hit(FxConfig cfg, FxStyle st, float cx, float cy) {
		hits++;
		boolean crit = hits % 3 == 0;
		int slot = numHead++ % numText.length;
		numText[slot] = crit ? "14" : NORMAL[hits % NORMAL.length];
		numCrit[slot] = crit;
		numAge[slot] = 0f;
		numX[slot] = cx + (hits % 2 == 0 ? 6f : -6f);
		shake = 1f;

		ThreadLocalRandom r = ThreadLocalRandom.current();
		int n = Math.round((crit ? 18 : 9) * cfg.hitIntensity);
		for (int i = 0; i < n; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			float k = st.speed() * (0.4f + r.nextFloat() * 0.8f);
			pool.spawn(cx, cy, (float) Math.cos(a) * k, (float) Math.sin(a) * k - 10f,
					st.life() * (0.7f + r.nextFloat() * 0.6f), st.size() + (crit ? 1 : 0), st.colorA(), st.colorB(), st.shape());
		}
	}
}
