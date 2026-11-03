package dev.rpgcombat.ui.row;

import dev.rpgcombat.ui.Draw;
import dev.rpgcombat.ui.Theme;
import dev.rpgcombat.ui.UiSave;
import dev.rpgcombat.ui.UiSounds;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Slider seret; nilai di-snap ke step. Input lewat press/drag/release dari layar (GLFW). */
public final class SliderRow extends LabeledRow {
	public static final int H = 32;
	private final double min;
	private final double max;
	private final double step;
	private final String fmt;
	private final DoubleSupplier get;
	private final DoubleConsumer set;
	private boolean dragging;

	public SliderRow(String labelKey, double min, double max, double step, String fmt, DoubleSupplier get, DoubleConsumer set) {
		super(labelKey);
		this.min = min;
		this.max = max;
		this.step = step;
		this.fmt = fmt;
		this.get = get;
		this.set = set;
	}

	@Override
	public int height() {
		return H;
	}

	private int trackX(int x) {
		return x + 8;
	}

	private int trackW(int w) {
		return w - 16;
	}

	@Override
	public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, float alpha, int mx, int my) {
		super.render(g, font, x, y, w, alpha, mx, my);
		double v = Math.max(min, Math.min(max, get.getAsDouble()));
		float k = (float) ((v - min) / (max - min));

		Draw.text(g, font, label(), x + 8, y + 4, Theme.TEXT, alpha);
		String val = String.format(Locale.ROOT, fmt, v);
		Draw.text(g, font, val, x + w - 8 - font.width(val), y + 4, Theme.ACCENT, alpha);

		int tx = trackX(x), tw = trackW(w), ty = y + 20;
		Draw.roundRect(g, tx, ty, tw, 4, 2, Draw.argb(alpha, 0x2B2540));
		int fw = Math.round(tw * k);
		if (fw > 0) Draw.roundRect(g, tx, ty, Math.max(fw, 3), 4, 2, Draw.argb(alpha, Theme.ACCENT));
		int kx = tx + fw - 4;
		Draw.roundRect(g, kx, ty - 3, 8, 10, 4, Draw.argb(alpha, dragging ? 0xFFFFFF : 0xE6DCFF));
	}

	@Override
	public boolean press(int x, int y, int w, int mx, int my) {
		if (!inside(x, y + 12, w, H - 12, mx, my)) return false;
		dragging = true;
		apply(x, w, mx);
		UiSounds.click();
		return true;
	}

	@Override
	public void drag(int x, int y, int w, int mx, int my) {
		if (dragging) apply(x, w, mx);
	}

	@Override
	public void release() {
		dragging = false;
	}

	private void apply(int x, int w, int mx) {
		double k = Math.max(0.0, Math.min(1.0, (mx - trackX(x)) / (double) trackW(w)));
		double v = min + k * (max - min);
		if (step > 0) v = Math.round(v / step) * step;
		set.accept(Math.max(min, Math.min(max, v)));
		UiSave.touch();
	}
}
