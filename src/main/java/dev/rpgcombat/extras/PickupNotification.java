package dev.rpgcombat.extras;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Satu popup. Semua teks dan lebar dihitung SEKALI saat dibuat,
 * jadi menggambarnya per frame murah (tanpa string/format baru).
 *
 * Titik asal gambar (0,0) = pojok kanan-atas blok; x negatif = ke kiri (rata kanan).
 */
final class PickupNotification {
	static final int LINE = 10;
	private static final int MAX_CHIPS = 8;
	private static final int COLS = 2;
	private static final int CHIP_GAP = 10;

	final ItemStack sample;
	int count;

	private String countStr;
	private int countW;
	private final String nameStr;
	private final int nameW;
	private final int nameRgb;

	// chip enchantment
	private int chipN;
	private final String[] chipName = new String[MAX_CHIPS + 1];
	private final int[] chipNameW = new int[MAX_CHIPS + 1];
	private final String[] chipLvl = new String[MAX_CHIPS + 1];
	private final int[] chipLvlW = new int[MAX_CHIPS + 1];
	private final int[] chipBadge = new int[MAX_CHIPS + 1];
	private final boolean[] chipHasBadge = new boolean[MAX_CHIPS + 1];
	private final int[] chipRow = new int[MAX_CHIPS + 1];
	private final int[] chipX = new int[MAX_CHIPS + 1];
	private final int[] rowW = new int[MAX_CHIPS + 1];
	private int rowCount;

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

		ItemEnchantments ench = s.get(DataComponents.ENCHANTMENTS);
		if (ench == null || ench.isEmpty()) ench = s.get(DataComponents.STORED_ENCHANTMENTS);
		boolean enchanted = ench != null && !ench.isEmpty();
		this.nameRgb = enchanted ? 0x8FEFFF : 0xFFFFFF;

		if (cfg.pickupEnchants && enchanted) readEnchants(font, ench);

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

	// ---------------------------------------------------------------- data
	private void readEnchants(Font font, ItemEnchantments ench) {
		List<Object2IntMap.Entry<Holder<Enchantment>>> list = new ArrayList<>(ench.entrySet());
		// level tertinggi dulu, lalu nama
		list.sort((a, b) -> {
			int c = Integer.compare(b.getIntValue(), a.getIntValue());
			return c != 0 ? c : a.getKey().value().description().getString()
					.compareTo(b.getKey().value().description().getString());
		});

		int shown = Math.min(list.size(), MAX_CHIPS);
		for (int i = 0; i < shown; i++) {
			Object2IntMap.Entry<Holder<Enchantment>> e = list.get(i);
			Enchantment en = e.getKey().value();
			int level = e.getIntValue();
			int max = en.getMaxLevel();

			chipName[i] = en.description().getString();
			chipNameW[i] = font.width(chipName[i]);
			chipHasBadge[i] = max > 1 || level > 1;
			if (chipHasBadge[i]) {
				chipLvl[i] = Fx.roman(level);
				chipLvlW[i] = font.width(chipLvl[i]);
				// level maksimal = merah, tinggi = biru, sisanya = hijau
				chipBadge[i] = level >= max ? 0xD04040 : (level * 10 >= max * 6 ? 0x3D7BE0 : 0x3FA06B);
			}
		}
		chipN = shown;
		if (list.size() > shown) { // chip ringkasan "+N"
			chipName[chipN] = "+" + (list.size() - shown);
			chipNameW[chipN] = font.width(chipName[chipN]);
			chipHasBadge[chipN] = false;
			chipN++;
		}

		// susun chip ke baris (COLS per baris)
		int rowIdx = 0;
		int x = 0;
		for (int i = 0; i < chipN; i++) {
			int w = chipNameW[i] + (chipHasBadge[i] ? 3 + chipLvlW[i] + 4 : 0);
			if (i % COLS == 0 && i > 0) {
				rowW[rowIdx] = x - CHIP_GAP;
				rowIdx++;
				x = 0;
			}
			chipRow[i] = rowIdx;
			chipX[i] = x;
			x += w + CHIP_GAP;
		}
		rowW[rowIdx] = x - CHIP_GAP;
		rowCount = rowIdx + 1;
	}

	/** Dipanggil saat dibuat dan saat jumlah bertambah (merge). */
	void refreshCount(Font font) {
		countStr = "+" + count;
		countW = font.width(countStr);

		int w = countW + 4 + nameW;
		if (hasDur) w = Math.max(w, Math.max(durStrW, 70));
		for (int r = 0; r < rowCount; r++) w = Math.max(w, rowW[r]);
		this.width = w;

		int h = LINE;
		if (hasDur) h += 13;
		h += rowCount * (LINE + 1);
		this.height = h;
	}

	void start(long now, int durationMs) {
		this.popMs = now;
		this.bumpMs = -100000L;
		this.expireMs = now + durationMs;
	}

	// ---------------------------------------------------------------- draw
	void draw(GuiGraphicsExtractor g, Font font, float alpha) {
		int y0 = 0;

		// "+3 Nama Item"
		int lineW = countW + 4 + nameW;
		g.text(font, countStr, -lineW, y0, Fx.argb(alpha, 0x55FF55), true);
		g.text(font, nameStr, -nameW, y0, Fx.argb(alpha, nameRgb), true);
		y0 += LINE;

		if (hasDur) {
			g.text(font, durStr, -durStrW, y0, Fx.argb(alpha * 0.85f, 0xC8D0DC), true);
			y0 += 10;
			int bw = Math.max(durStrW, 70);
			g.fill(-bw, y0, 0, y0 + 2, Fx.argb(alpha * 0.6f, 0x20242C));
			int filled = Math.round(bw * durFrac);
			if (filled > 0) {
				g.fill(-bw, y0, -bw + filled, y0 + 2,
						Fx.argb(alpha, Fx.lerpColor(0xFF4040, 0x55FF55, durFrac)));
			}
			y0 += 3;
		}

		// enchantment: sub-teks di bawah nama
		for (int i = 0; i < chipN; i++) {
			int r = chipRow[i];
			int cx = -rowW[r] + chipX[i];
			int cy = y0 + r * (LINE + 1);
			g.text(font, chipName[i], cx, cy, Fx.argb(alpha, 0xCFD8E6), true);
			if (chipHasBadge[i]) {
				int bx = cx + chipNameW[i] + 3;
				g.fill(bx, cy - 1, bx + chipLvlW[i] + 4, cy + 9, Fx.argb(alpha * 0.9f, chipBadge[i]));
				g.text(font, chipLvl[i], bx + 2, cy, Fx.argb(alpha, 0xFFFFFF), false);
			}
		}
	}
}
