package dev.rpgcombat.ui.row;

import dev.rpgcombat.ui.Anim;
import dev.rpgcombat.ui.Draw;
import dev.rpgcombat.ui.Theme;
import dev.rpgcombat.ui.UiSave;
import dev.rpgcombat.ui.UiSounds;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ToggleRow extends LabeledRow {
	public static final int H = 26;
	private final BooleanSupplier get;
	private final Consumer<Boolean> set;
	private float knob = -1f; // 0..1, diperhalus per frame

	public ToggleRow(String labelKey, BooleanSupplier get, Consumer<Boolean> set) {
		super(labelKey);
		this.get = get;
		this.set = set;
	}

	@Override
	public int height() {
		return H;
	}

	@Override
	public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, float alpha, int mx, int my) {
		super.render(g, font, x, y, w, alpha, mx, my);
		boolean on = get.getAsBoolean();
		if (knob < 0f) knob = on ? 1f : 0f;
		knob += ((on ? 1f : 0f) - knob) * 0.28f;

		Draw.text(g, font, label(), x + 8, y + (H - 8) / 2, Theme.TEXT, alpha);

		int pw = 30, ph = 14;
		int px = x + w - pw - 8, py = y + (H - ph) / 2;
		int track = lerpRgb(0x3A3350, Theme.ACCENT_DIM, knob);
		Draw.roundRect(g, px, py, pw, ph, 7, Draw.argb(alpha, track));
		Draw.roundBorder(g, px, py, pw, ph, 7, 1, Draw.argb(0.45f * alpha, on ? Theme.ACCENT : 0x6E6788));
		int kx = px + 2 + Math.round((pw - ph) * knob);
		Draw.roundRect(g, kx, py + 2, ph - 4, ph - 4, (ph - 4) / 2, Draw.argb(alpha, on ? 0xFFFFFF : 0xB8B2CC));
	}

	@Override
	public boolean press(int x, int y, int w, int mx, int my) {
		if (!inside(x, y, w, H, mx, my)) return false;
		set.accept(!get.getAsBoolean());
		UiSounds.click();
		UiSave.touch();
		return true;
	}

	static int lerpRgb(int a, int b, float t) {
		int r = (int) Anim.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
		int g = (int) Anim.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
		int bl = (int) Anim.lerp(t, a & 0xFF, b & 0xFF);
		return (r << 16) | (g << 8) | bl;
	}
}
