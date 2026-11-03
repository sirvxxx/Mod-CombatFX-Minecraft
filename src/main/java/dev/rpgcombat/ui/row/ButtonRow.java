package dev.rpgcombat.ui.row;

import dev.rpgcombat.ui.Draw;
import dev.rpgcombat.ui.Theme;
import dev.rpgcombat.ui.UiSounds;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ButtonRow extends LabeledRow {
	public static final int H = 28;
	private final Runnable action;

	public ButtonRow(String labelKey, Runnable action) {
		super(labelKey);
		this.action = action;
	}

	@Override
	public int height() {
		return H;
	}

	@Override
	public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, float alpha, int mx, int my) {
		boolean hot = inside(x, y, w, H, mx, my);
		Draw.roundRect(g, x + 4, y + 3, w - 8, H - 6, 7, Draw.argb(alpha, hot ? Theme.PANEL_HOT : 0x1A1530));
		Draw.roundBorder(g, x + 4, y + 3, w - 8, H - 6, 7, 1, Draw.argb((hot ? 0.8f : 0.4f) * alpha, Theme.ACCENT));
		Draw.textCentered(g, font, label(), x + w / 2, y + (H - 8) / 2, hot ? 0xFFFFFF : Theme.TEXT, alpha);
	}

	@Override
	public boolean press(int x, int y, int w, int mx, int my) {
		if (!inside(x, y, w, H, mx, my)) return false;
		UiSounds.click();
		action.run();
		return true;
	}
}
