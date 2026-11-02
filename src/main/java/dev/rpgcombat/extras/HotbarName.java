package dev.rpgcombat.extras;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Popup saat berganti item di tangan/hotbar: NAMA item + daftar enchantment di bawahnya
 * (contoh: Netherite Sword / Sharpness [V]  Mending). Item tanpa enchant hanya menampilkan nama.
 *
 * Terdeteksi dari perubahan item di tangan utama (bukan dari nomor slot), jadi tidak bergantung
 * pada API Inventory yang berubah antar versi. Tooltip vanilla disembunyikan oleh RpgCombatClient.
 * Blok ditambatkan di tepi bawah (dekat hotbar) dan tumbuh ke atas kalau enchant-nya banyak.
 */
public final class HotbarName {
	private HotbarName() {}

	private static final long LIFE_MS = 2000L;
	private static final long FADE_MS = 500L;
	public static final int NAME_H = 10;

	/** false kalau tooltip vanilla tidak bisa disembunyikan -> popup ini mati otomatis (tidak dobel). */
	public static boolean available = true;

	private static ItemStack last = ItemStack.EMPTY;
	private static LocalPlayer lastPlayer;
	private static int quietTicks;

	private static String text = "";
	private static int textW;
	private static int rgb = 0xFFFFFF;
	private static EnchantChips chips;
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
		Font font = mc.font;
		text = cur.getHoverName().getString();
		textW = font.width(text);
		rgb = EnchantChips.has(cur) ? 0x8FEFFF : 0xFFFFFF;
		chips = new EnchantChips(font, cur, 2);
		showMs = System.currentTimeMillis();
	}

	/** Posisi default tepi atas baris nama (di atas hotbar). */
	public static int defaultY(int screenH) {
		return screenH - 62;
	}

	/** Tinggi blok untuk jumlah baris enchant tertentu. */
	public static int blockHeight(int enchantRows) {
		return NAME_H + (enchantRows > 0 ? 2 + enchantRows * EnchantChips.ROW_H : 0);
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc, ExtrasConfig cfg) {
		if (!available || !cfg.hotbarNameEnabled) return;
		long age = System.currentTimeMillis() - showMs;
		if (age < 0 || age >= LIFE_MS) return;

		// pop: 0.70 -> 1.06 -> 1.0 dalam 220 ms, lalu memudar di 500 ms terakhir
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
		alpha *= cfg.hotbarNameOpacity;
		if (alpha <= 0.02f) return;

		var window = mc.getWindow();
		int rows = chips == null ? 0 : chips.rows();
		int h = blockHeight(rows);
		float sc = cfg.hotbarNameScale * s;
		float x = window.getGuiScaledWidth() / 2f + cfg.hotbarNameOffsetX;
		float bottom = defaultY(window.getGuiScaledHeight()) + NAME_H + cfg.hotbarNameOffsetY;
		Font font = mc.font;

		// pivot di tengah-bawah blok, supaya "pop" tumbuh dari dekat hotbar
		g.pose().pushMatrix();
		g.pose().translate(x, bottom);
		g.pose().scale(sc, sc);
		g.pose().translate(0f, (float) -h);
		g.text(font, text, -textW / 2, 0, Fx.argb(alpha, rgb), true);
		if (rows > 0) {
			chips.draw(g, font, 0, NAME_H + 2, alpha, true);
		}
		g.pose().popMatrix();
	}
}
