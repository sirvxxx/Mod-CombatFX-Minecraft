package dev.rpgcombat.ui.row;

import dev.rpgcombat.ui.Draw;
import dev.rpgcombat.ui.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Judul seksi / catatan (bukan setting). */
public final class LabelRow extends LabeledRow {
	private final boolean heading;

	public LabelRow(String key, boolean heading) {
		super(key);
		this.heading = heading;
	}

	@Override
	public int height() {
		return heading ? 22 : 18;
	}

	@Override
	public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, float alpha, int mx, int my) {
		if (heading) {
			Draw.text(g, font, label().toUpperCase(java.util.Locale.ROOT), x + 8, y + 10, Theme.GOLD, alpha);
			g.fill(x + 8, y + 20, x + w - 8, y + 21, Draw.argb(0.25f * alpha, Theme.GOLD));
		} else {
			Draw.text(g, font, label(), x + 8, y + 5, Theme.TEXT_DIM, alpha);
		}
	}

	@Override
	public boolean interactive() {
		return false;
	}
}
