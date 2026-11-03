package dev.rpgcombat.ui.row;

import dev.rpgcombat.ui.Draw;
import dev.rpgcombat.ui.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Dasar baris berlabel: terjemahan di-cache sekali, latar hover seragam. */
abstract class LabeledRow implements Row {
	private final String key;
	private String label;

	LabeledRow(String key) {
		this.key = key;
	}

	final String label() {
		if (label == null) label = Component.translatable(key).getString();
		return label;
	}

	static String tr(String key) {
		return Component.translatable(key).getString();
	}

	static boolean inside(int x, int y, int w, int h, int mx, int my) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	final void hoverBg(GuiGraphicsExtractor g, int x, int y, int w, int h, float alpha, int mx, int my) {
		if (inside(x, y, w, h, mx, my)) {
			Draw.roundRect(g, x, y, w, h, 5, Draw.argb(0.10f * alpha, Theme.ACCENT));
		}
	}

	@Override
	public String searchText() {
		return label();
	}

	@Override
	public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, float alpha, int mx, int my) {
		hoverBg(g, x, y, w, height(), alpha, mx, my);
	}
}
