package dev.rpgcombat.spatial;

import dev.rpgcombat.config.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2f;

/**
 * Transformasi 2.5D (pseudo-3D) untuk elemen HUD.
 *
 * Pose GUI di 26.x adalah Matrix3x2fStack, yaitu matriks AFIN 2D (translate / rotate / scale / shear),
 * bukan matriks 4x4 perspektif. Karena itu elemen diproyeksikan secara ORTOGRAFIS:
 *
 *     R = Rz(roll) * Rx(pitch) * Ry(yaw)   ->   ambil kolom x,y dan baris x,y (buang z)
 *
 * Hasilnya jajaran genjang yang sah untuk pose 3x2: sisi yang miring memendek (foreshortening) dan
 * bidang tampak condong. Kedalaman (depth) menskalakan elemen seperti kamera mendekat/menjauh
 * dan menambah bayangan jatuh (dekat) atau meredupkan elemen (jauh).
 * Titik hilang (trapesium perspektif sejati) tidak bisa dibuat dengan pose 3x2, jadi tidak dipalsukan.
 *
 * Matriks yang sama dipakai untuk menggambar DAN untuk hit-test mouse (lihat linear() dan unmap()),
 * sehingga klik/drag tetap akurat pada elemen yang miring atau dimajumundurkan.
 */
public final class Spatial {
	private Spatial() {}

	public static final String FACE = "face";
	public static final String BARS = "bars";
	public static final String INFO = "info";
	public static final String CIRCLE = "rpg_hud";
	public static final String COMBO = "combo";
	public static final String PICKUP = "pickup";
	public static final String MINING = "mining";
	public static final String HOTBAR = "hotbar";

	private static final float FOCAL = 420f;
	private static final Matrix3x2f TMP = new Matrix3x2f();
	private static final float[] LIN = new float[4];
	private static final SpatialSettings FLAT = new SpatialSettings();

	/** Pengaturan elemen (dibuat otomatis kalau belum ada). */
	public static SpatialSettings get(String id) {
		return ModConfig.get().spatial.computeIfAbsent(id, k -> new SpatialSettings());
	}

	/** Pengaturan datar yang tidak pernah disimpan (untuk pemanggil tanpa elemen). */
	public static SpatialSettings flat() {
		return FLAT;
	}

	// ------------------------------------------------------------------ math
	/**
	 * Bagian linear 2x2 (sudah termasuk skala kedalaman): x' = o0*x + o1*y ; y' = o2*x + o3*y.
	 * Koordinat relatif terhadap titik pivot elemen, sumbu y ke BAWAH (seperti layar).
	 */
	public static void linear(SpatialSettings s, float[] o) {
		float th = (float) Math.toRadians(s.yaw);
		float ph = (float) Math.toRadians(s.pitch);
		float ps = (float) Math.toRadians(s.roll);
		float cth = (float) Math.cos(th), sth = (float) Math.sin(th);
		float cph = (float) Math.cos(ph), sph = (float) Math.sin(ph);
		float cps = (float) Math.cos(ps), sps = (float) Math.sin(ps);

		// A = (Rx * Ry) bagian 2x2 = [[cth, 0], [sph*sth, cph]]
		// M = Rz * A
		float m00 = cps * cth - sps * sph * sth;
		float m01 = -sps * cph;
		float m10 = sps * cth + cps * sph * sth;
		float m11 = cps * cph;

		float k = scaleOf(s);
		o[0] = m00 * k;
		o[1] = m01 * k;
		o[2] = m10 * k;
		o[3] = m11 * k;
	}

	/** Skala dari kedalaman: dekat = besar, jauh = kecil. */
	public static float scaleOf(SpatialSettings s) {
		return 1f / (1f - s.depth / FOCAL);
	}

	/** Pengali opacity: elemen yang jauh (depth negatif) meredup sampai 70%. */
	public static float fade(SpatialSettings s) {
		return s.depth >= 0f ? 1f : 1f + 0.3f * (s.depth / SpatialSettings.MAX_DEPTH);
	}

	/** Invers dari linear(): titik layar (relatif pivot) -> titik lokal elemen. null kalau tidak bisa dibalik. */
	public static boolean unmap(float[] lin, float dx, float dy, float[] out) {
		float det = lin[0] * lin[3] - lin[1] * lin[2];
		if (Math.abs(det) < 1e-3f) return false;
		out[0] = (lin[3] * dx - lin[1] * dy) / det;
		out[1] = (-lin[2] * dx + lin[0] * dy) / det;
		return true;
	}

	// ---------------------------------------------------------------- render
	/**
	 * Mulai transformasi. Wajib dipasangkan dengan end(g, hasil).
	 * Pivot (cx, cy) dan ukuran (w, h) dalam koordinat yang sedang aktif di pose.
	 *
	 * @param panel   true = elemen berlatar panel, gambar bayangan jatuh bergradasi
	 * @param opacity untuk intensitas bayangan
	 * @return true kalau transformasi dipasang (dan harus di-end)
	 */
	public static boolean begin(GuiGraphicsExtractor g, SpatialSettings s, float cx, float cy,
			float w, float h, boolean panel, float opacity) {
		if (s == null || s.isIdentity()) return false;
		linear(s, LIN);

		if (panel) {
			float tilt = Math.min(1f, (Math.abs(s.pitch) + Math.abs(s.yaw)) / 90f);
			float lift = Math.max(0f, s.depth) / SpatialSettings.MAX_DEPTH;
			float strength = Math.min(1f, lift + tilt * 0.6f);
			if (strength > 0.02f) drawShadow(g, cx, cy, w, h, strength, opacity);
		}

		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		TMP.set(LIN[0], LIN[2], LIN[1], LIN[3], 0f, 0f); // JOML: set(m00, m01, m10, m11, m20, m21)
		g.pose().mul(TMP);
		g.pose().translate(-cx, -cy);
		return true;
	}

	public static void end(GuiGraphicsExtractor g, boolean applied) {
		if (applied) g.pose().popMatrix();
	}

	/** Bayangan jatuh 4 lapis (tepi lembut), mengikuti kemiringan bidang tapi bergeser di ruang layar. */
	private static void drawShadow(GuiGraphicsExtractor g, float cx, float cy, float w, float h,
			float strength, float opacity) {
		float off = 2f + 7f * strength;
		g.pose().pushMatrix();
		g.pose().translate(cx + off * 0.7f, cy + off);
		g.pose().mul(TMP.set(LIN[0], LIN[2], LIN[1], LIN[3], 0f, 0f));
		int x0 = Math.round(-w / 2f);
		int y0 = Math.round(-h / 2f);
		int x1 = Math.round(w / 2f);
		int y1 = Math.round(h / 2f);
		float base = 0.34f * strength * opacity;
		for (int i = 3; i >= 0; i--) {
			int a = Math.max(0, Math.min(255, Math.round(base * (0.35f + 0.22f * (3 - i)) * 255f)));
			if (a < 3) continue;
			g.fill(x0 - i, y0 - i, x1 + i, y1 + i, a << 24);
		}
		g.pose().popMatrix();
	}
}
