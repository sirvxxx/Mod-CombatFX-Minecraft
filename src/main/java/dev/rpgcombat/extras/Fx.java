package dev.rpgcombat.extras;

/** Helper animasi/warna ringan (tanpa alokasi). */
final class Fx {
	private Fx() {}

	static float clamp01(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}

	static float lerp(float t, float a, float b) {
		return a + (b - a) * t;
	}

	/** Faktor smoothing bebas-framerate: 1 - e^(-dt*speed). */
	static float damp(float dt, float speed) {
		return 1f - (float) Math.exp(-dt * speed);
	}

	static int lerpColor(int from, int to, float t) {
		int r = (int) lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
		int g = (int) lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
		int b = (int) lerp(t, from & 0xFF, to & 0xFF);
		return (r << 16) | (g << 8) | b;
	}

	/** alpha 0..1 + rgb -> ARGB (alpha minimal 5 karena vanilla membuang teks alpha <= 3). */
	static int argb(float alpha, int rgb) {
		int a = Math.max(5, Math.min(255, (int) (alpha * 255f)));
		return (a << 24) | (rgb & 0xFFFFFF);
	}

	static String roman(int n) {
		return switch (n) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			case 6 -> "VI";
			case 7 -> "VII";
			case 8 -> "VIII";
			case 9 -> "IX";
			case 10 -> "X";
			default -> Integer.toString(n);
		};
	}
}
