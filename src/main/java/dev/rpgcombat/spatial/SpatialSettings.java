package dev.rpgcombat.spatial;

/** Pengaturan 2.5D satu elemen HUD. Semua nol = datar (tidak ada biaya render). */
public class SpatialSettings {
	public static final float MAX_ANGLE = 75f;
	public static final float MAX_DEPTH = 100f;

	public float pitch;  // sumbu X: atas menjauh / mendekat (derajat)
	public float yaw;    // sumbu Y: kanan menjauh / mendekat (derajat)
	public float roll;   // sumbu Z: putar di bidang layar (derajat)
	public float depth;  // -100 (jauh/kecil/redup) .. +100 (dekat/besar/berbayang)

	public boolean isIdentity() {
		return pitch == 0f && yaw == 0f && roll == 0f && depth == 0f;
	}

	public void reset() {
		pitch = yaw = roll = depth = 0f;
	}

	public void sanitize() {
		pitch = clamp(pitch, MAX_ANGLE);
		yaw = clamp(yaw, MAX_ANGLE);
		roll = clamp(roll, 180f);
		depth = clamp(depth, MAX_DEPTH);
	}

	private static float clamp(float v, float lim) {
		return v < -lim ? -lim : (v > lim ? lim : v);
	}
}
