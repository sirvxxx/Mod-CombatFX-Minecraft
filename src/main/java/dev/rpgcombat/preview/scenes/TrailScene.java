package dev.rpgcombat.preview.scenes;

import dev.rpgcombat.fx.FxConfig;
import dev.rpgcombat.fx.FxStyle;
import dev.rpgcombat.fx.FxStyles;
import dev.rpgcombat.preview.DollStage;
import dev.rpgcombat.preview.ParticlePool;
import dev.rpgcombat.preview.PreviewCanvas;
import dev.rpgcombat.preview.PreviewScene;
import dev.rpgcombat.ui.Draw;
import dev.rpgcombat.ui.Theme;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Karakter berjalan di tempat; trail mengalir ke belakang dengan style yang sedang dipilih. */
public final class TrailScene implements PreviewScene {
	private final ParticlePool pool = new ParticlePool(360);
	private float acc;
	private float phase;

	@Override
	public void render(GuiGraphicsExtractor g, Minecraft mc, int x, int y, int w, int h, float dt, float time, float alpha) {
		FxConfig cfg = FxConfig.get();
		FxStyle st = FxStyles.find(FxStyles.TRAILS, cfg.trailStyle);
		float pace = cfg.trailSprintOnly ? 1.4f : 1.0f; // mode sprint = langkah lebih cepat
		float s = Math.max(1.4f, Math.min(4.2f, Math.min((h - 70) / (float) DollStage.HEIGHT, w * 0.32f / DollStage.WIDTH)));
		float floor = y + h - PreviewCanvas.FLOOR_PAD;
		float dollX = x + w * 0.60f;
		phase += dt * 7.5f * pace;

		// garis lantai bergulir ke kiri = ilusi berlari
		float off = (time * 80f * pace) % 46f;
		for (int k = -1; k < w / 46 + 2; k++) {
			int gx = Math.round(x + k * 46 - off);
			int x0 = Math.max(gx, x + 6);
			int x1 = Math.min(gx + 14, x + w - 6);
			if (x1 > x0) g.fill(x0, Math.round(floor) + 4, x1, Math.round(floor) + 5, Draw.argb(0.22f * alpha, Theme.ACCENT));
		}

		// spawn di telapak kaki, mengalir ke kiri
		ThreadLocalRandom r = ThreadLocalRandom.current();
		acc += dt * st.rate() * cfg.trailDensity;
		while (acc >= 1f) {
			acc -= 1f;
			float px = dollX + (r.nextFloat() - 0.5f) * 5f * s;
			float py = floor - 1f - r.nextFloat() * 2f;
			float vx = -st.speed() * (0.7f + r.nextFloat() * 0.6f) * pace;
			float vy = (r.nextFloat() - 0.5f) * st.spread() - (st.gravity() < 0 ? 8f : 0f);
			pool.spawn(px, py, vx, vy, st.life() * (0.7f + r.nextFloat() * 0.6f), st.size(), st.colorA(), st.colorB(), st.shape());
		}
		pool.tick(dt, st.gravity());
		pool.draw(g, alpha, x + 4, y + 4, x + w - 4, y + h - 4);

		DollStage.draw(g, mc, dollX, floor, s, time, 0.6f * pace, phase, alpha);
	}
}
