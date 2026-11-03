package dev.rpgcombat.ui.row;

import dev.rpgcombat.ui.Draw;
import dev.rpgcombat.ui.Theme;
import dev.rpgcombat.ui.UiSave;
import dev.rpgcombat.ui.UiSounds;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Baris pemilih warna dari palet (kotak kecil). */
public final class ColorRow extends LabeledRow {
	public static final int H = 30;
	public static final int[] PALETTE = {
			0xFF3B3B, 0xFF8A1F, 0xFFC400, 0xFFE066, 0x55FF55,
			0x55FFFF, 0x6AA8FF, 0xB26BFF, 0xFF6AD5, 0xFFFFFF
	};
	private static final int SW = 12;
	private static final int GAP = 4;
	private final IntSupplier get;
	private final IntConsumer set;

	public ColorRow(String labelKey, IntSupplier get, IntConsumer set) {
		super(labelKey);
		this.get = get;
		this.set = set;
	}

	@Override
	public int height() {
		return H;
	}

	private int left(int x, int w) {
		return x + w - 8 - PALETTE.length * (SW + GAP) + GAP;
	}

	@Override
	public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, float alpha, int mx, int my) {
		super.render(g, font, x, y, w, alpha, mx, my);
		Draw.text(g, font, label(), x + 8, y + (H - 8) / 2, Theme.TEXT, alpha);
		int cur = get.getAsInt() & 0xFFFFFF;
		int sx = left(x, w), sy = y + (H - SW) / 2;
		for (int c : PALETTE) {
			boolean sel = c == cur;
			if (sel) Draw.roundBorder(g, sx - 2, sy - 2, SW + 4, SW + 4, 4, 1, Draw.argb(alpha, 0xFFFFFF));
			Draw.roundRect(g, sx, sy, SW, SW, 3, Draw.argb(alpha, c));
			sx += SW + GAP;
		}
	}

	@Override
	public boolean press(int x, int y, int w, int mx, int my) {
		if (!inside(x, y, w, H, mx, my)) return false;
		int sx = left(x, w);
		for (int c : PALETTE) {
			if (mx >= sx - 1 && mx < sx + SW + 1) {
				set.accept(c);
				UiSounds.click();
				UiSave.touch();
				return true;
			}
			sx += SW + GAP;
		}
		return true;
	}
}
