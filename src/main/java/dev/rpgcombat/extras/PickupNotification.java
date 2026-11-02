package dev.rpgcombat.extras;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Satu popup item. Semua teks dan lebar dihitung SEKALI saat dibuat,
 * jadi menggambarnya per frame murah (tanpa string/format baru).
 *
 * Titik asal gambar (0,0) = pojok kanan-atas blok; blok digambar ke kiri (x negatif).
 * Susunan: [ikon item] [+N Nama] / durability / daftar enchant (rata kiri di samping ikon).
 */
final class PickupNotification {
	static final int LINE = 10;
	private static final int ICON_GAP = 4;

	final ItemStack sample;
	int count;

	private String countStr;
	private int countW;
	private final String nameStr;
	private final int nameW;
	private final int nameRgb;
	private final EnchantChips chips;

	// durability
	private boolean hasDur;
	private String durStr = "";
	private int durStrW;
	private float durFrac;

	int width;
	int height;

	// state animasi (dikelola PickupManager)
	long popMs;
	long bumpMs;
	long expireMs;
	float y;
	boolean placed;

	PickupNotification(Font font, ItemStack s, int count, ExtrasConfig cfg) {
		this.sample = s.copy();
		this.count = count;
		this.nameStr = s.getHoverName().getString();
		this.nameW = font.width(nameStr);
		this.nameRgb = EnchantChips.has(s) ? 0x8FEFFF : 0xFFFFFF;
		this.chips = cfg.pickupEnchants ? new EnchantChips(font, s, 2) : null;

		if (cfg.pickupDurability && s.isDamageableItem() && s.getMaxDamage() > 0) {
			int max = s.getMaxDamage();
			int left = Math.max(0, max - s.getDamageValue());
			hasDur = true;
			durStr = left + " / " + max;
			durStrW = font.width(durStr);
			durFrac = Fx.clamp01(left / (float) max);
		}
		refreshCount(font);
	}

	/** Dipanggil saat dibuat dan saat jumlah bertambah (merge). */
	void refreshCount(Font font) {
		countStr = "+" + count;
		countW = font.width(countStr);

		int w = countW + 4 + nameW;
		if (hasDur) w = Math.max(w, Math.max(durStrW, 70));
		if (chips != null) w = Math.max(w, chips.width());
		this.width = ItemIcon.SIZE + ICON_GAP + w;

		int h = LINE;
		if (hasDur) h += 13;
		if (chips != null) h += chips.height();
		this.height = Math.max(ItemIcon.SIZE, h);
	}

	void start(long now, int durationMs) {
		this.popMs = now;
		this.bumpMs = -100000L;
		this.expireMs = now + durationMs;
	}

	/**
	 * @param anim alpha animasi (masuk/keluar), dipakai juga untuk ikon
	 * @param op   opacity dari pengaturan, hanya untuk teks dan panel
	 */
	void draw(GuiGraphicsExtractor g, Font font, float anim, float op) {
		float alpha = anim * op;
		int left = -width;
		if (op >= 0.3f) ItemIcon.draw(g, sample, left, 0, anim);

		int cx = left + ItemIcon.SIZE + ICON_GAP;
		int y0 = 0;

		g.text(font, countStr, cx, y0, Fx.argb(alpha, 0x55FF55), true);
		g.text(font, nameStr, cx + countW + 4, y0, Fx.argb(alpha, nameRgb), true);
		y0 += LINE;

		if (hasDur) {
			g.text(font, durStr, cx, y0, Fx.argb(alpha * 0.85f, 0xC8D0DC), true);
			y0 += 10;
			int bw = Math.max(durStrW, 70);
			g.fill(cx, y0, cx + bw, y0 + 2, Fx.argb(alpha * 0.6f, 0x20242C));
			int filled = Math.round(bw * durFrac);
			if (filled > 0) {
				g.fill(cx, y0, cx + filled, y0 + 2, Fx.argb(alpha, Fx.lerpColor(0xFF4040, 0x55FF55, durFrac)));
			}
			y0 += 3;
		}

		if (chips != null && !chips.isEmpty()) {
			chips.draw(g, font, cx, y0, alpha, false);
		}
	}
}
