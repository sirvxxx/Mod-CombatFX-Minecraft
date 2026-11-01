package dev.rpgcombat.extras;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Notifikasi nama item saat berganti item di tangan/hotbar.
 * Hanya NAMA (tanpa enchant/durability) supaya hotbar tetap bersih.
 *
 * Terdeteksi dari perubahan item di tangan utama (bukan dari nomor slot), jadi
 * tidak bergantung pada API Inventory yang berubah antar versi.
 * Tooltip vanilla disembunyikan oleh RpgCombatClient supaya tidak dobel.
 */
public final class HotbarName {
	private HotbarName() {}

	private static final long LIFE_MS = 1700L;
	private static final long FADE_MS = 450L;

	/** false kalau tooltip vanilla tidak bisa disembunyikan -> popup ini mati otomatis (tidak dobel). */
	public static boolean available = true;

	private static ItemStack last = ItemStack.EMPTY;
	private static LocalPlayer lastPlayer;
	private static int quietTicks;

	private static String text = "";
	private static int textW;
	private static int rgb = 0xFFFFFF;
	private static long showMs = -100000L;

	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) {
			lastPlayer = null;
			last = ItemStack.EMPTY;
			return;
		}
		if (p != lastPlayer) {
			lastPlayer = p;
			quietTicks = 30;
			last = p.getMainHandItem().copy();
			return;
		}
		ItemStack cur = p.getMainHandItem();
		boolean changed = cur.isEmpty() != last.isEmpty() || (!cur.isEmpty() && !ItemKinds.same(cur, last));
		if (!changed) return;

		last = cur.copy();
		if (quietTicks > 0) {
			quietTicks--;
			return;
		}
		ExtrasConfig cfg = ExtrasConfig.get();
		if (!available || !cfg.hotbarNameEnabled || cur.isEmpty()) {
			showMs = -100000L;
			return;
		}
		text = cur.getHoverName().getString();
		textW = mc.font.width(text);
		boolean enchanted = cur.get(DataComponents.ENCHANTMENTS) != null && !cur.get(DataComponents.ENCHANTMENTS).isEmpty();
		rgb = enchanted ? 0x8FEFFF : 0xFFFFFF;
		showMs = System.currentTimeMillis();
	}

	/** Posisi default: di atas hotbar, di tengah layar. */
	public static int defaultY(int screenH) {
		return screenH - 62;
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc, ExtrasConfig cfg) {
		if (!available || !cfg.hotbarNameEnabled) return;
		long age = System.currentTimeMillis() - showMs;
		if (age < 0 || age >= LIFE_MS) return;

		// pop: 0.70 -> 1.06 -> 1.0 dalam 200 ms, lalu memudar di 450 ms terakhir
		float s;
		if (age < 120L) {
			s = Fx.lerp(age / 120f, 0.70f, 1.06f);
		} else if (age < 220L) {
			s = Fx.lerp((age - 120L) / 100f, 1.06f, 1f);
		} else {
			s = 1f;
		}
		float alpha = age < 120L ? Fx.clamp01(age / 120f) : 1f;
		long left = LIFE_MS - age;
		if (left < FADE_MS) alpha = Math.min(alpha, left / (float) FADE_MS);
		if (alpha <= 0.02f) return;

		var window = mc.getWindow();
		float x = window.getGuiScaledWidth() / 2f + cfg.hotbarNameOffsetX;
		float y = defaultY(window.getGuiScaledHeight()) + cfg.hotbarNameOffsetY;
		float sc = cfg.hotbarNameScale * s;
		Font font = mc.font;

		g.pose().pushMatrix();
		g.pose().translate(x, y + 4f);
		g.pose().scale(sc, sc);
		g.text(font, text, -textW / 2, -4, Fx.argb(alpha, rgb), true);
		g.pose().popMatrix();
	}
}
