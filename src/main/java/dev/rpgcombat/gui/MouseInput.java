package dev.rpgcombat.gui;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Membaca status tombol mouse dan posisi kursor langsung dari GLFW.
 *
 * KENAPA BUKAN mouseHandler.isLeftPressed(): di Minecraft field itu HANYA diperbarui saat tidak ada
 * layar yang terbuka. Di dalam layar apa pun nilainya tidak pernah berubah, jadi drag tidak pernah
 * mulai. GLFW selalu memberi status tombol yang sebenarnya, tidak tergantung versi Minecraft.
 *
 * Posisi kursor dikonversi ke koordinat GUI (pecahan) dengan rumus yang sama seperti vanilla:
 *   guiX = cursorX * guiScaledWidth / windowWidth
 */
public final class MouseInput {
	private MouseInput() {}

	private static final double[] CX = new double[1];
	private static final double[] CY = new double[1];
	private static final int[] WW = new int[1];
	private static final int[] WH = new int[1];

	private static boolean searched;
	private static Method handleMethod;

	/** Handle jendela GLFW, atau 0 kalau tidak ketemu. */
	public static long window(Minecraft mc) {
		long h = GLFW.glfwGetCurrentContext();
		if (h != 0L) return h;
		if (!searched) {
			searched = true;
			try {
				Object win = mc.getWindow();
				for (String n : new String[] {"handle", "getWindow", "window", "getHandle"}) {
					try {
						Method m = win.getClass().getMethod(n);
						if (m.getReturnType() == long.class) {
							handleMethod = m;
							break;
						}
					} catch (Throwable ignored) {
						// coba nama berikutnya
					}
				}
			} catch (Throwable ignored) {
				handleMethod = null;
			}
		}
		if (handleMethod != null) {
			try {
				return (Long) handleMethod.invoke(mc.getWindow());
			} catch (Throwable t) {
				handleMethod = null;
			}
		}
		return 0L;
	}

	public static boolean left(long win) {
		return GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
	}

	public static boolean right(long win) {
		return GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
	}

	/** Isi out[0], out[1] dengan posisi kursor dalam koordinat GUI. */
	public static void cursor(Minecraft mc, long win, double[] out) {
		GLFW.glfwGetCursorPos(win, CX, CY);
		GLFW.glfwGetWindowSize(win, WW, WH);
		var window = mc.getWindow();
		double w = Math.max(1, WW[0]);
		double h = Math.max(1, WH[0]);
		out[0] = CX[0] * window.getGuiScaledWidth() / w;
		out[1] = CY[0] * window.getGuiScaledHeight() / h;
	}
}
