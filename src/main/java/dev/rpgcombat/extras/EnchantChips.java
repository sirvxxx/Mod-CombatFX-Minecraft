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
 * Daftar enchantment sebagai "chip": nama + badge level berwarna (contoh: Sharpness [V]  Mending).
 * Semua teks dan lebar dihitung sekali saat dibuat; draw() tidak membuat objek baru.
 *
 * Sumber data: DataComponents.ENCHANTMENTS, atau STORED_ENCHANTMENTS untuk enchanted book.
 */
final class EnchantChips {
	static final int LINE = 10;
	static final int ROW_H = LINE + 1;
	private static final int MAX_CHIPS = 8;
	private static final int GAP = 10;

	private final int cols;
	private int n;
	private final String[] name = new String[MAX_CHIPS + 1];
	private final int[] nameW = new int[MAX_CHIPS + 1];
	private final String[] lvl = new String[MAX_CHIPS + 1];
	private final int[] lvlW = new int[MAX_CHIPS + 1];
	private final int[] badge = new int[MAX_CHIPS + 1];
	private final boolean[] hasBadge = new boolean[MAX_CHIPS + 1];
	private final int[] row = new int[MAX_CHIPS + 1];
	private final int[] x = new int[MAX_CHIPS + 1];
	private final int[] rowW = new int[MAX_CHIPS + 1];
	private int rows;

	static ItemEnchantments of(ItemStack s) {
		ItemEnchantments e = s.get(DataComponents.ENCHANTMENTS);
		if (e == null || e.isEmpty()) e = s.get(DataComponents.STORED_ENCHANTMENTS);
		return e != null && !e.isEmpty() ? e : null;
	}

	static boolean has(ItemStack s) {
		return of(s) != null;
	}

	EnchantChips(Font font, ItemStack s, int cols) {
		this.cols = Math.max(1, cols);
		ItemEnchantments ench = of(s);
		if (ench == null) return;

		List<Object2IntMap.Entry<Holder<Enchantment>>> list = new ArrayList<>(ench.entrySet());
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

			name[i] = en.description().getString();
			nameW[i] = font.width(name[i]);
			hasBadge[i] = max > 1 || level > 1;
			if (hasBadge[i]) {
				lvl[i] = Fx.roman(level);
				lvlW[i] = font.width(lvl[i]);
				// level maksimal = merah, tinggi = biru, sisanya = hijau
				badge[i] = level >= max ? 0xD04040 : (level * 10 >= max * 6 ? 0x3D7BE0 : 0x3FA06B);
			}
		}
		n = shown;
		if (list.size() > shown) { // chip ringkasan "+N"
			name[n] = "+" + (list.size() - shown);
			nameW[n] = font.width(name[n]);
			n++;
		}

		int r = 0;
		int cx = 0;
		for (int i = 0; i < n; i++) {
			int w = nameW[i] + (hasBadge[i] ? 3 + lvlW[i] + 4 : 0);
			if (i > 0 && i % this.cols == 0) {
				rowW[r] = cx - GAP;
				r++;
				cx = 0;
			}
			row[i] = r;
			x[i] = cx;
			cx += w + GAP;
		}
		rowW[r] = cx - GAP;
		rows = r + 1;
	}

	boolean isEmpty() {
		return n == 0;
	}

	int rows() {
		return n == 0 ? 0 : rows;
	}

	int height() {
		return rows() * ROW_H;
	}

	int width() {
		int w = 0;
		for (int r = 0; r < rows(); r++) w = Math.max(w, rowW[r]);
		return w;
	}

	/**
	 * @param ox kiri blok (centered=false) atau titik tengah (centered=true)
	 * @param oy atas blok
	 */
	void draw(GuiGraphicsExtractor g, Font font, int ox, int oy, float alpha, boolean centered) {
		for (int i = 0; i < n; i++) {
			int r = row[i];
			int cx = centered ? ox - rowW[r] / 2 + x[i] : ox + x[i];
			int cy = oy + r * ROW_H;
			g.text(font, name[i], cx, cy, Fx.argb(alpha, 0xCFD8E6), true);
			if (hasBadge[i]) {
				int bx = cx + nameW[i] + 3;
				g.fill(bx, cy - 1, bx + lvlW[i] + 4, cy + 9, Fx.argb(alpha * 0.9f, badge[i]));
				g.text(font, lvl[i], bx + 2, cy, Fx.argb(alpha, 0xFFFFFF), false);
			}
		}
	}
}
