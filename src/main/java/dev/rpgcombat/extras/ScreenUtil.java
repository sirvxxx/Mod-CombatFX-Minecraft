package dev.rpgcombat.extras;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Cek apakah ada layar (inventory, chest, menu) yang sedang terbuka.
 * Lokasinya berbeda antar versi, jadi dicoba berurutan lewat reflection:
 *   1. Minecraft#gui lalu Gui#screen()   (26.2)
 *   2. Minecraft#screen (field)          (versi lama)
 *   3. Minecraft#getScreen()/screen()
 * Kalau semuanya gagal, dianggap tidak ada layar.
 */
final class ScreenUtil {
	private ScreenUtil() {}

	private static boolean searched;
	private static Field guiField;
	private static Method guiScreenMethod;
	private static Field screenField;
	private static Method screenMethod;

	static boolean isOpen(Minecraft mc) {
		if (!searched) {
			searched = true;
			search();
		}
		try {
			if (guiField != null && guiScreenMethod != null) {
				Object gui = guiField.get(mc);
				return gui != null && guiScreenMethod.invoke(gui) != null;
			}
			if (screenField != null) return screenField.get(mc) != null;
			if (screenMethod != null) return screenMethod.invoke(mc) != null;
		} catch (Throwable t) {
			guiField = null;
			guiScreenMethod = null;
			screenField = null;
			screenMethod = null;
		}
		return false;
	}

	private static void search() {
		try {
			Field f = Minecraft.class.getDeclaredField("gui");
			f.setAccessible(true);
			Method m = f.getType().getMethod("screen");
			if (Screen.class.isAssignableFrom(m.getReturnType())) {
				guiField = f;
				guiScreenMethod = m;
				return;
			}
		} catch (Throwable ignored) {
			// coba cara berikutnya
		}
		try {
			Field f = Minecraft.class.getDeclaredField("screen");
			if (Screen.class.isAssignableFrom(f.getType())) {
				f.setAccessible(true);
				screenField = f;
				return;
			}
		} catch (Throwable ignored) {
			// coba method
		}
		for (String name : new String[] {"getScreen", "screen"}) {
			try {
				Method m = Minecraft.class.getMethod(name);
				if (Screen.class.isAssignableFrom(m.getReturnType())) {
					screenMethod = m;
					return;
				}
			} catch (Throwable ignored) {
				// lanjut
			}
		}
	}
}
