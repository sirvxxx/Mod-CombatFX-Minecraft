package dev.rpgcombat.preview;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Partikel 2D untuk menu/preview: array paralel, tanpa alokasi saat spawn/tick/draw. */
public final class ParticlePool {
	private final float[] x;
	private final float[] y;
	private final float[] vx;
	private final float[] vy;
	private final float[] age;
	private final float[] life;
	private final float[] size;
	private final int[] rgb;
	private int n;

	public ParticlePool(int capacity) {
		x = new float[capacity];
		y = new float[capacity];
		vx = new float[capacity];
		vy = new float[capacity];
		age = new float[capacity];
		life = new float[capacity];
		size = new float[capacity];
		rgb = new int[capacity];
	}

	public int count() {
		return n;
	}

	public boolean spawn(float px, float py, float pvx, float pvy, float lifeSec, float sz, int color) {
		if (n >= x.length) return false;
		x[n] = px;
		y[n] = py;
		vx[n] = pvx;
		vy[n] = pvy;
		age[n] = 0f;
		life[n] = lifeSec;
		size[n] = sz;
		rgb[n] = color;
		n++;
		return true;
	}

	public void tick(float dt, float gravity) {
		for (int i = n - 1; i >= 0; i--) {
			age[i] += dt;
			if (age[i] >= life[i]) {
				remove(i);
				continue;
			}
			vy[i] += gravity * dt;
			x[i] += vx[i] * dt;
			y[i] += vy[i] * dt;
		}
	}

	private void remove(int i) {
		int last = n - 1;
		if (i != last) {
			x[i] = x[last];
			y[i] = y[last];
			vx[i] = vx[last];
			vy[i] = vy[last];
			age[i] = age[last];
			life[i] = life[last];
			size[i] = size[last];
			rgb[i] = rgb[last];
		}
		n--;
	}

	public void clear() {
		n = 0;
	}

	/** Alpha mengikuti umur: muncul cepat, memudar pelan. */
	public void draw(GuiGraphicsExtractor g, float alpha) {
		for (int i = 0; i < n; i++) {
			float k = age[i] / life[i];
			float a = (k < 0.15f ? k / 0.15f : 1f - (k - 0.15f) / 0.85f) * alpha;
			int al = Math.max(3, Math.min(255, Math.round(a * 255f)));
			int s = Math.max(1, Math.round(size[i] * (1f - 0.4f * k)));
			int px = Math.round(x[i]);
			int py = Math.round(y[i]);
			g.fill(px, py, px + s, py + s, (al << 24) | (rgb[i] & 0xFFFFFF));
		}
	}
}
