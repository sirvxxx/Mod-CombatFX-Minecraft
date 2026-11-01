package dev.rpgcombat.extras;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Cek apakah ada layar (inventory, chest, menu) yang sedang terbuka.
 * Nama field/method layar saat ini berubah-ubah antar versi Minecraft,
 * jadi dicari lewat reflection sekali saja. Kalau tidak ketemu, dianggap tidak ada layar.
 */
final class ScreenUtil {
	private ScreenUtil() {}

	private static boolean searched;
	private static Field field;
	private static Method method;

	static boolean isOpen(Minecraft mc) {
		if (!searched) {
			searched = true;
			try {
				Field f = Minecraft.class.getDeclaredField("screen");
				if (Screen.class.isAssignableFrom(f.getType())) {
					f.setAccessible(true);
					field = f;
				}
			} catch (Throwable ignored) {
				// coba method
			}
			if (field == null) {
				for (String name : new String[] {"getScreen", "screen"}) {
					try {
						Method m = Minecraft.class.getMethod(name);
						if (Screen.class.isAssignableFrom(m.getReturnType())) {
							method = m;
							break;
						}
					} catch (Throwable ignored) {
						// lanjut
					}
				}
			}
		}
		try {
			if (field != null) return field.get(mc) != null;
			if (method != null) return method.invoke(mc) != null;
		} catch (Throwable t) {
			field = null;
			method = null;
		}
		return false;
	}
}
