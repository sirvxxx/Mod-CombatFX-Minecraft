package dev.rpghud;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class McCompat {
    private McCompat() {
    }

    public static void openScreen(Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        for (String name : new String[]{"setScreenAndShow", "setScreen"}) {
            try {
                Method m;
                try {
                    m = Minecraft.class.getMethod(name, Screen.class);
                }
                catch (NoSuchMethodException e) {
                    m = Minecraft.class.getDeclaredMethod(name, Screen.class);
                    m.setAccessible(true);
                }
                m.invoke((Object)mc, screen);
                return;
            }
            catch (NoSuchMethodException m) {
            }
            catch (Exception e) {
                System.err.println("[RPG HUD] Gagal membuka layar: " + String.valueOf(e));
                return;
            }
        }
        System.err.println("[RPG HUD] Method setScreen tidak ditemukan di versi ini.");
    }
}

