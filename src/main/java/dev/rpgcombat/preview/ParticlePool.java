package dev.rpgcombat.preview;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Partikel 2D untuk menu/preview: array paralel, tanpa alokasi saat spawn/tick/draw.
 * Warna bergradasi colorA -> colorB sepanjang umur; bentuk: 0 kotak, 1 plus, 2 hati, 3 api.
 */
public final class ParticlePool {
	private final float[] x;
	private final float[] y;
	private final float[] vx;
	private final float[] vy;
	private final float[] age;
	private final float[] life;
	private final float[] size;
	private final int[] c0;
	private final int[] c1;
	private final int[] shape;
	private int n;

	public ParticlePool(int capacity) {
		x = new float[capacity];
		y = new float[capacity];
		vx = new float[capacity];
		vy = new float[capacity];
		age = new float[capacity];
		life = new float[capacity];
		size = new float[capacity];
		c0 = new int[capacity];
		c1 = new int[capacity];
		shape = new int[capacity];
	}

	public int count() {
		return n;
	}

	public boolean spawn(float px, float py, float pvx, float pvy, float lifeSec, float sz, int color) {
		return spawn(px, py, pvx, pvy, lifeSec, sz, color, color, 0);
	}

	public boolean spawn(float px, float py, float pvx, float pvy, float lifeSec, float sz, int colorA, int colorB, int shp) {
		if (n >= x.length) return false;
		x[n] = px;
		y[n] = py;
		vx[n] = pvx;
		vy[n] = pvy;
		age[n] = 0f;
		life[n] = Math.max(0.05f, lifeSec);
		size[n] = sz;
		c0[n] = colorA;
		c1[n] = colorB;
		shape[n] = shp;
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
			c0[i] = c0[last];
			c1[i] = c1[last];
			shape[i] = shape[last];
		}
		n--;
	}

	public void clear() {
		n = 0;
	}

	public void draw(GuiGraphicsExtractor g, float alpha) {
		draw(g, alpha, -100000, -100000, 100000, 100000);
	}

	/** Hanya menggambar partikel yang berada di dalam kotak (pengganti scissor). */
	public void draw(GuiGraphicsExtractor g, float alpha, int minX, int minY, int maxX, int maxY) {
		for (int i = 0; i < n; i++) {
			int px = Math.round(x[i]);
			int py = Math.round(y[i]);
			if (px < minX || py < minY || px >= maxX || py >= maxY) continue;
			float k = age[i] / life[i];
			float a = (k < 0.12f ? k / 0.12f : 1f - (k - 0.12f) / 0.88f) * alpha;
			int al = Math.max(3, Math.min(255, Math.round(a * 255f)));
			int rgb = mix(c0[i], c1[i], k);
			int col = (al << 24) | rgb;
			int s = Math.max(1, Math.round(size[i] * (1f - 0.35f * k)));
			switch (shape[i]) {
				case 1 -> { // plus
					g.fill(px - s, py, px + s + 1, py + 1, col);
					g.fill(px, py - s, px + 1, py + s + 1, col);
				}
				case 2 -> { // hati
					int u = Math.max(1, s / 2 + 1);
					g.fill(px + u, py, px + 2 * u, py + u, col);
					g.fill(px + 3 * u, py, px + 4 * u, py + u, col);
					g.fill(px, py + u, px + 5 * u, py + 2 * u, col);
					g.fill(px + u, py + 2 * u, px + 4 * u, py + 3 * u, col);
					g.fill(px + 2 * u, py + 3 * u, px + 3 * u, py + 4 * u, col);
				}
				case 3 -> g.fill(px, py, px + Math.max(1, s * 2 / 3), py + s * 3 / 2 + 1, col); // api
				default -> g.fill(px, py, px + s, py + s, col);
			}
		}
	}

	private static int mix(int a, int b, float t) {
		int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
		int g = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
		int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
		return (r << 16) | (g << 8) | bl;
	}
}
