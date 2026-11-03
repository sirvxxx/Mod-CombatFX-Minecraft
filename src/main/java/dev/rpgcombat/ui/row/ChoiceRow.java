package dev.rpgcombat.ui.row;

import dev.rpgcombat.ui.Draw;
import dev.rpgcombat.ui.Theme;
import dev.rpgcombat.ui.UiSave;
import dev.rpgcombat.ui.UiSounds;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Pilihan: segmen untuk <= 3 opsi, panah kiri/kanan untuk lebih banyak. */
public final class ChoiceRow extends LabeledRow {
	public static final int H = 30;
	private final List<String> optionKeys;
	private String[] options;
	private final IntSupplier get;
	private final IntConsumer set;

	public ChoiceRow(String labelKey, List<String> optionKeys, IntSupplier get, IntConsumer set) {
		super(labelKey);
		this.optionKeys = optionKeys;
		this.get = get;
		this.set = set;
	}

	@Override
	public int height() {
		return H;
	}

	private String[] opts() {
		if (options == null) {
			options = new String[optionKeys.size()];
			for (int i = 0; i < options.length; i++) options[i] = tr(optionKeys.get(i));
		}
		return options;
	}

	private boolean segmented() {
		return optionKeys.size() <= 3;
	}

	private int ctrlW(Font font, int w) {
		if (!segmented()) return Math.min(w / 2, 130);
		int total = 0;
		for (String o : opts()) total += font.width(o) + 16;
		return Math.min(total, w - 90);
	}

	@Override
	public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, float alpha, int mx, int my) {
		super.render(g, font, x, y, w, alpha, mx, my);
		Draw.text(g, font, label(), x + 8, y + (H - 8) / 2, Theme.TEXT, alpha);
		int cur = Math.max(0, Math.min(optionKeys.size() - 1, get.getAsInt()));
		String[] o = opts();
		int cw = ctrlW(font, w);
		int cx = x + w - cw - 8, cy = y + 5, ch = H - 10;
		Draw.roundRect(g, cx, cy, cw, ch, 6, Draw.argb(alpha, 0x1A1530));
		Draw.roundBorder(g, cx, cy, cw, ch, 6, 1, Draw.argb(0.4f * alpha, Theme.ACCENT_DIM));

		if (segmented()) {
			int sx = cx;
			for (int i = 0; i < o.length; i++) {
				int sw = font.width(o[i]) + 16;
				if (i == cur) Draw.roundRect(g, sx + 1, cy + 1, sw - 2, ch - 2, 5, Draw.argb(alpha, Theme.ACCENT_DIM));
				Draw.text(g, font, o[i], sx + 8, cy + (ch - 8) / 2, i == cur ? 0xFFFFFF : Theme.TEXT_DIM, alpha);
				sx += sw;
			}
		} else {
			Draw.text(g, font, "<", cx + 6, cy + (ch - 8) / 2, Theme.ACCENT, alpha);
			Draw.text(g, font, ">", cx + cw - 6 - font.width(">"), cy + (ch - 8) / 2, Theme.ACCENT, alpha);
			Draw.textCentered(g, font, o[cur], cx + cw / 2, cy + (ch - 8) / 2, Theme.TEXT, alpha);
		}
	}

	@Override
	public boolean press(int x, int y, int w, int mx, int my) {
		if (!inside(x, y, w, H, mx, my)) return false;
		net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
		int cw = ctrlW(font, w);
		int cx = x + w - cw - 8;
		if (mx < cx || mx >= cx + cw) return false;
		int cur = Math.max(0, Math.min(optionKeys.size() - 1, get.getAsInt()));
		int next = cur;
		if (segmented()) {
			int sx = cx;
			String[] o = opts();
			for (int i = 0; i < o.length; i++) {
				int sw = font.width(o[i]) + 16;
				if (mx >= sx && mx < sx + sw) next = i;
				sx += sw;
			}
		} else {
			int n = optionKeys.size();
			next = mx < cx + cw / 2 ? (cur + n - 1) % n : (cur + 1) % n;
		}
		if (next != cur) {
			set.accept(next);
			UiSounds.click();
			UiSave.touch();
		}
		return true;
	}

	@Override
	public String searchText() {
		StringBuilder sb = new StringBuilder(label());
		for (String o : opts()) sb.append(' ').append(o);
		return sb.toString();
	}
}
