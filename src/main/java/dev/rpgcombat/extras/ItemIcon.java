package dev.rpgcombat.extras;

import java.lang.reflect.Method;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Menggambar ikon item 16x16 (model 2D/3D sesuai item, termasuk resource pack).
 *
 * Nama method di GuiGraphicsExtractor berubah-ubah (renderItem / item / fakeItem ...),
 * jadi dicari sekali lewat reflection: method publik (ItemStack, int, int) -> void.
 * Kalau tidak ketemu, digambar kotak kecil sebagai pengganti sehingga layout tidak rusak.
 */
final class ItemIcon {
	private ItemIcon() {}

	static final int SIZE = 16;

	private static boolean searched;
	private static Method method;

	/** @param alpha 0..1; render item tidak mendukung alpha, jadi ikon mengecil + hilang saat memudar */
	static void draw(GuiGraphicsExtractor g, ItemStack stack, int x, int y, float alpha) {
		if (alpha <= 0.25f) return;
		if (!searched) {
			searched = true;
			find();
		}

		float s = alpha >= 0.98f ? 1f : 0.55f + 0.45f * ((alpha - 0.25f) / 0.73f);
		boolean scaled = s < 0.999f;
		if (scaled) {
			g.pose().pushMatrix();
			g.pose().translate(x + SIZE / 2f, y + SIZE / 2f);
			g.pose().scale(s, s);
			g.pose().translate(-SIZE / 2f, -SIZE / 2f);
		}
		int dx = scaled ? 0 : x;
		int dy = scaled ? 0 : y;

		boolean ok = false;
		if (method != null) {
			try {
				method.invoke(g, stack, dx, dy);
				ok = true;
			} catch (Throwable t) {
				method = null;
			}
		}
		if (!ok) {
			g.fill(dx + 3, dy + 3, dx + 13, dy + 13, 0xFF8A93A6);
			g.fill(dx + 4, dy + 4, dx + 12, dy + 12, 0xFFC9D1E0);
		}
		if (scaled) g.pose().popMatrix();
	}

	private static void find() {
		String[] preferred = {"item", "renderItem", "drawItem", "fakeItem", "renderFakeItem"};
		for (String name : preferred) {
			for (Method m : GuiGraphicsExtractor.class.getMethods()) {
				if (!m.getName().equals(name)) continue;
				Class<?>[] p = m.getParameterTypes();
				if (p.length == 3 && p[0] == ItemStack.class && p[1] == int.class && p[2] == int.class
						&& m.getReturnType() == void.class) {
					method = m;
					return;
				}
			}
		}
		// cadangan: method (ItemStack,int,int) apa pun yang bukan dekorasi/tooltip
		for (Method m : GuiGraphicsExtractor.class.getMethods()) {
			Class<?>[] p = m.getParameterTypes();
			String n = m.getName().toLowerCase();
			if (p.length == 3 && p[0] == ItemStack.class && p[1] == int.class && p[2] == int.class
					&& m.getReturnType() == void.class && !n.contains("decor") && !n.contains("tooltip")) {
				method = m;
				return;
			}
		}
	}
}
