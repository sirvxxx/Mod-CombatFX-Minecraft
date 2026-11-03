package dev.rpgcombat.ui.row;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Satu baris pengaturan. Halaman fitur = List&lt;Row&gt;; layar hanya menata dan meneruskan input.
 * (x, y, w) adalah kotak yang dialokasikan layar untuk baris ini.
 */
public interface Row {
	int height();

	void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, float alpha, int mx, int my);

	/** Tombol kiri baru ditekan di dalam kotak baris. true = input dipakai. */
	default boolean press(int x, int y, int w, int mx, int my) {
		return false;
	}

	/** Dipanggil tiap frame selama tombol ditahan setelah press() yang dipakai baris ini. */
	default void drag(int x, int y, int w, int mx, int my) {
	}

	default void release() {
	}

	/** Teks yang dicari oleh search bar. */
	default String searchText() {
		return "";
	}

	/** false = hanya label/judul (tidak dihitung sebagai "setting"). */
	default boolean interactive() {
		return true;
	}
}
