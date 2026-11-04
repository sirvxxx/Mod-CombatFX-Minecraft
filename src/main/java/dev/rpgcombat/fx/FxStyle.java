package dev.rpgcombat.fx;

/**
 * SATU deskripsi efek yang dipakai dua renderer:
 *  - dunia  : partikel vanilla (particle / particle2) dengan kecepatan turunan speed/spread
 *  - preview: partikel 2D di menu dengan warna colorA -> colorB, bentuk, ukuran, umur
 * Jadi preview selalu mewakili efek yang benar-benar muncul di game.
 */
public record FxStyle(
		String id, String kind,
		int colorA, int colorB, int shape,
		String particle, String particle2,
		float rate,      // partikel per detik (trail)
		float speed,     // kecepatan dasar (px/detik di preview)
		float spread,    // sebaran
		float gravity,   // px/detik^2 di preview; negatif = naik
		float life,      // detik
		float size) {    // px

	public static final int SQUARE = 0;
	public static final int PLUS = 1;
	public static final int HEART = 2;
	public static final int FLAME = 3;

	public String nameKey() {
		return "rpgcombat.fx." + kind + "." + id;
	}

	/** id yang disimpan di daftar favorit, mis. "trail:flame". */
	public String favId() {
		return kind + ":" + id;
	}
}
