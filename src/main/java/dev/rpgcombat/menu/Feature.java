package dev.rpgcombat.menu;

import dev.rpgcombat.preview.PreviewScene;
import dev.rpgcombat.ui.Icon;
import dev.rpgcombat.ui.row.Row;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Satu fitur = satu kartu di grid + satu halaman berisi baris pengaturan. */
public final class Feature {
	public final String id;
	public final Category cat;
	public final String titleKey;
	public final String subKey;
	public final Icon icon;
	/** null = tidak ada toggle di kartu. */
	public final BooleanSupplier enabled;
	public final Consumer<Boolean> setEnabled;
	private final Supplier<List<Row>> pageSupplier;
	private List<Row> rows;
	private final Supplier<PreviewScene> previewSupplier;
	private PreviewScene scene;

	public Feature(String id, Category cat, String titleKey, String subKey, Icon icon,
			BooleanSupplier enabled, Consumer<Boolean> setEnabled, Supplier<List<Row>> page) {
		this(id, cat, titleKey, subKey, icon, enabled, setEnabled, page, null);
	}

	public Feature(String id, Category cat, String titleKey, String subKey, Icon icon,
			BooleanSupplier enabled, Consumer<Boolean> setEnabled, Supplier<List<Row>> page,
			Supplier<PreviewScene> preview) {
		this.previewSupplier = preview;
		this.id = id;
		this.cat = cat;
		this.titleKey = titleKey;
		this.subKey = subKey;
		this.icon = icon;
		this.enabled = enabled;
		this.setEnabled = setEnabled;
		this.pageSupplier = page;
	}

	/** Adegan preview (dibuat sekali). null = halaman ini tidak punya preview efek. */
	public PreviewScene scene() {
		if (previewSupplier == null) return null;
		if (scene == null) scene = previewSupplier.get();
		return scene;
	}

	/** true kalau salah satu style di fitur ini ditandai favorit (id favorit berawalan "kind:"). */
	public boolean hasFavoriteStyle(Set<String> favorites) {
		String prefix = id + ":";
		String prefix2 = kindPrefix();
		for (String f : favorites) {
			if (f.startsWith(prefix) || (prefix2 != null && f.startsWith(prefix2))) return true;
		}
		return false;
	}

	/** trails -> "trail:", hits -> "hit:", kills -> "kill:" (sesuai FxStyle#favId). */
	private String kindPrefix() {
		return switch (id) {
			case "trails" -> "trail:";
			case "hits" -> "hit:";
			case "kills" -> "kill:";
			default -> null;
		};
	}

	public boolean hasToggle() {
		return enabled != null;
	}

	/** Dibuat sekali lalu dipakai ulang (baris hanya memegang supplier ke config). */
	public List<Row> rows() {
		if (rows == null) rows = pageSupplier.get();
		return rows;
	}

	public int settingCount() {
		int n = 0;
		for (Row r : rows()) {
			if (r.interactive()) n++;
		}
		return n;
	}
}
