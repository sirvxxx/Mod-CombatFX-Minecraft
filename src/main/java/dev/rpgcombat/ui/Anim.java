package dev.rpgcombat.ui;

/** Animasi ringan berbasis delta time. Tidak ada alokasi per frame. */
public final class Anim {
	private Anim() {}

	/** Pegas teredam kritis: bergerak halus ke target dan bisa dibatalkan di tengah jalan tanpa patah. */
	public static final class Spring {
		public float value;
		public float velocity;
		public float target;
		private final float stiffness;
		private final float damping;

		public Spring(float initial, float stiffness) {
			this.value = initial;
			this.target = initial;
			this.stiffness = stiffness;
			this.damping = 2f * (float) Math.sqrt(stiffness);
		}

		public void update(float dt) {
			dt = Math.min(dt, 1f / 30f); // stabil saat game tersendat
			float accel = stiffness * (target - value) - damping * velocity;
			velocity += accel * dt;
			value += velocity * dt;
		}

		public void snap(float v) {
			value = v;
			target = v;
			velocity = 0f;
		}

		public boolean settled() {
			return Math.abs(target - value) < 0.002f && Math.abs(velocity) < 0.02f;
		}
	}

	public static float lerp(float t, float a, float b) {
		return a + (b - a) * t;
	}

	public static float clamp01(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}

	public static float easeOutCubic(float t) {
		float i = 1f - clamp01(t);
		return 1f - i * i * i;
	}
}
