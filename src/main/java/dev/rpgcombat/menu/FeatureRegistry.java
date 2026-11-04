package dev.rpgcombat.menu;

import dev.rpgcombat.ui.row.Row;
import dev.rpgcombat.ui.UiConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.network.chat.Component;

public final class FeatureRegistry {
	private FeatureRegistry() {}

	public static final List<Feature> ALL = new ArrayList<>();

	public static void register(Feature f) {
		ALL.add(f);
	}

	/**
	 * Saring fitur.
	 *  - query kosong : fitur milik kategori (atau semua kalau cat == null)
	 *  - query terisi : cari di SEMUA kategori (judul, subjudul, dan teks baris di dalam halaman)
	 * Favorit selalu di urutan teratas.
	 */
	public static List<Feature> query(String query, Category cat, boolean favOnly) {
		String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		Set<String> fav = UiConfig.get().favorites;
		List<Feature> out = new ArrayList<>();
		for (Feature f : ALL) {
			if (favOnly && !isFav(f, fav)) continue;
			if (q.isEmpty()) {
				if (cat != null && !favOnly && f.cat != cat) continue;
			} else if (!matches(f, q)) {
				continue;
			}
			out.add(f);
		}
		// urutan stabil: favorit dulu, sisanya sesuai urutan registrasi
		List<Feature> sorted = new ArrayList<>(out.size());
		for (Feature f : out) if (isFav(f, fav)) sorted.add(f);
		for (Feature f : out) if (!isFav(f, fav)) sorted.add(f);
		return sorted;
	}

	private static boolean isFav(Feature f, Set<String> fav) {
		return fav.contains(f.id) || f.hasFavoriteStyle(fav);
	}

	private static boolean matches(Feature f, String q) {
		if (tr(f.titleKey).toLowerCase(Locale.ROOT).contains(q)) return true;
		if (tr(f.subKey).toLowerCase(Locale.ROOT).contains(q)) return true;
		for (Row r : f.rows()) {
			if (r.searchText().toLowerCase(Locale.ROOT).contains(q)) return true;
		}
		return false;
	}

	public static String tr(String key) {
		return Component.translatable(key).getString();
	}
}
