package dev.rpgcombat.ui.row;

import dev.rpgcombat.fx.FxStyle;
import dev.rpgcombat.ui.Draw;
import dev.rpgcombat.ui.Icon;
import dev.rpgcombat.ui.Theme;
import dev.rpgcombat.ui.UiConfig;
import dev.rpgcombat.ui.UiSave;
import dev.rpgcombat.ui.UiSounds;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Grid pemilih style efek. Setiap chip punya swatch gradasi, nama, dan bintang favorit.
 * Favorit ditampilkan paling depan. Memilih style langsung mengubah preview di sebelahnya.
 */
public final class StyleGridRow extends LabeledRow {
	private static final int COLS = 3;
	private static final int CH = 24;
	private static final int GAP = 6;
	private static final int PAD = 8;
	private static final int TOP = 18;

	private final List<FxStyle> styles;
	private final Supplier<String> get;
	private final Consumer<String> set;
	private final int[] order;
	private final String[] names;
	private int favSig = Integer.MIN_VALUE;

	public StyleGridRow(String labelKey, List<FxStyle> styles, Supplier<String> get, Consumer<String> set) {
		super(labelKey);
		this.styles = styles;
		this.get = get;
		this.set = set;
		this.order = new int[styles.size()];
		this.names = new String[styles.size()];
	}

	@Override
	public int height() {
		int rows = (styles.size() + COLS - 1) / COLS;
		return TOP + rows * (CH + GAP) + 2;
	}

	private void refreshOrder() {
		Set<String> fav = UiConfig.get().favorites;
		int sig = fav.hashCode();
		if (sig == favSig) return;
		favSig = sig;
		int n = 0;
		for (int i = 0; i < styles.size(); i++) if (fav.contains(styles.get(i).favId())) order[n++] = i;
		for (int i = 0; i < styles.size(); i++) if (!fav.contains(styles.get(i).favId())) order[n++] = i;
	}

	private String name(int i) {
		if (names[i] == null) names[i] = Component.translatable(styles.get(i).nameKey()).getString();
		return names[i];
	}

	private int chipW(int w) {
		return (w - 2 * PAD - (COLS - 1) * GAP) / COLS;
	}

	@Override
	public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, float alpha, int mx, int my) {
		refreshOrder();
		Draw.text(g, font, label(), x + 8, y + 4, Theme.TEXT, alpha);
		String cur = get.get();
		Set<String> fav = UiConfig.get().favorites;
		int cw = chipW(w);
		for (int d = 0; d < order.length; d++) {
			FxStyle s = styles.get(order[d]);
			int cx = x + PAD + (d % COLS) * (cw + GAP);
			int cy = y + TOP + (d / COLS) * (CH + GAP);
			boolean sel = s.id().equals(cur);
			boolean hot = inside(cx, cy, cw, CH, mx, my);
			Draw.roundRect(g, cx, cy, cw, CH, 6, Draw.argb(alpha, sel ? Theme.PANEL_HOT : (hot ? 0x1F1A33 : 0x15102A)));
			Draw.roundBorder(g, cx, cy, cw, CH, 6, 1, Draw.argb((sel ? 0.95f : (hot ? 0.6f : 0.3f)) * alpha, sel ? Theme.ACCENT : Theme.ACCENT_DIM));
			// swatch gradasi dua kotak
			Draw.roundRect(g, cx + 5, cy + 7, 5, 10, 2, Draw.argb(alpha, s.colorA()));
			Draw.roundRect(g, cx + 10, cy + 7, 5, 10, 2, Draw.argb(alpha, s.colorB()));
			Draw.text(g, font, fit(font, name(order[d]), cw - 38), cx + 20, cy + (CH - 8) / 2, sel ? 0xFFFFFF : Theme.TEXT, alpha);
			boolean isFav = fav.contains(s.favId());
			boolean hs = inside(cx + cw - 15, cy + 3, 13, CH - 6, mx, my);
			Draw.icon(g, Icon.STAR, cx + cw - 9, cy + CH / 2, 3, isFav ? Theme.GOLD : (hs ? 0xFFFFFF : 0x6F6888), alpha);
		}
	}

	@Override
	public boolean press(int x, int y, int w, int mx, int my) {
		refreshOrder();
		int cw = chipW(w);
		for (int d = 0; d < order.length; d++) {
			int cx = x + PAD + (d % COLS) * (cw + GAP);
			int cy = y + TOP + (d / COLS) * (CH + GAP);
			if (!inside(cx, cy, cw, CH, mx, my)) continue;
			FxStyle s = styles.get(order[d]);
			if (inside(cx + cw - 15, cy + 3, 13, CH - 6, mx, my)) {
				Set<String> fav = UiConfig.get().favorites;
				if (!fav.remove(s.favId())) fav.add(s.favId());
				favSig = Integer.MIN_VALUE;
			} else {
				set.accept(s.id());
			}
			UiSounds.click();
			UiSave.touch();
			return true;
		}
		return false;
	}

	@Override
	public String searchText() {
		StringBuilder sb = new StringBuilder(label());
		for (int i = 0; i < styles.size(); i++) sb.append(' ').append(name(i));
		return sb.toString();
	}

	private static String fit(Font font, String s, int maxW) {
		if (font.width(s) <= maxW) return s;
		String t = s;
		while (t.length() > 1 && font.width(t + "..") > maxW) t = t.substring(0, t.length() - 1);
		return t + "..";
	}
}
