package dev.rpgcombat.menu;

import dev.rpgcombat.ui.Icon;
import dev.rpgcombat.ui.row.Row;
import java.util.List;
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

	public Feature(String id, Category cat, String titleKey, String subKey, Icon icon,
			BooleanSupplier enabled, Consumer<Boolean> setEnabled, Supplier<List<Row>> page) {
		this.id = id;
		this.cat = cat;
		this.titleKey = titleKey;
		this.subKey = subKey;
		this.icon = icon;
		this.enabled = enabled;
		this.setEnabled = setEnabled;
		this.pageSupplier = page;
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
