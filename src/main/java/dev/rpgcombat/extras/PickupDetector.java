package dev.rpgcombat.extras;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Mendeteksi item yang masuk inventory TANPA mixin: tiap tick bandingkan isi inventory
 * dengan snapshot sebelumnya, lalu hitung selisih total per "jenis" item.
 *
 *  + memindah/menukar slot  -> selisih 0, tidak muncul
 *  + durability berkurang   -> diabaikan (lihat ItemKinds)
 *  + layar inventory/chest terbuka -> snapshot disinkronkan diam-diam (tidak ada notifikasi)
 *  + 1,5 detik pertama setelah join/respawn -> diam (server mengirim isi inventory)
 *
 * Biaya normal: loop 41 slot membandingkan item+jumlah; baru kalau ada yang berubah
 * dilakukan perhitungan selisih (jarang terjadi).
 */
public final class PickupDetector {
	private PickupDetector() {}

	private static ItemStack[] prev = new ItemStack[0];
	private static LocalPlayer lastPlayer;
	private static int quietTicks;

	private static final class Kind {
		final ItemStack sample;
		int delta;

		Kind(ItemStack sample) {
			this.sample = sample;
		}
	}

	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || mc.level == null) {
			lastPlayer = null;
			PickupManager.clear();
			return;
		}
		ExtrasConfig cfg = ExtrasConfig.get();
		Inventory inv = p.getInventory();
		int n = inv.getContainerSize();

		if (p != lastPlayer || prev.length != n) {
			lastPlayer = p;
			quietTicks = 30;
			PickupManager.clear();
			prev = new ItemStack[n];
		}

		boolean quiet = quietTicks > 0 || ScreenUtil.isOpen(mc) || !cfg.pickupEnabled;
		if (quietTicks > 0) quietTicks--;

		if (quiet) {
			snapshot(inv, n); // tetap sinkron supaya tidak ada notifikasi palsu saat kembali
			return;
		}

		boolean changed = false;
		for (int i = 0; i < n; i++) {
			ItemStack cur = inv.getItem(i);
			ItemStack old = prev[i];
			if (old == null || cur.getItem() != old.getItem() || cur.getCount() != old.getCount()) {
				changed = true;
				break;
			}
		}
		if (!changed) return;

		List<Kind> kinds = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			ItemStack old = prev[i];
			if (old != null && !old.isEmpty()) kindFor(kinds, old).delta -= old.getCount();
		}
		for (int i = 0; i < n; i++) {
			ItemStack cur = inv.getItem(i);
			if (!cur.isEmpty()) kindFor(kinds, cur).delta += cur.getCount();
		}
		for (Kind k : kinds) {
			if (k.delta > 0) PickupManager.add(mc, k.sample, k.delta);
		}
		snapshot(inv, n);
	}

	private static Kind kindFor(List<Kind> kinds, ItemStack s) {
		for (Kind k : kinds) {
			if (ItemKinds.same(k.sample, s)) return k;
		}
		Kind k = new Kind(s.copy());
		kinds.add(k);
		return k;
	}

	private static void snapshot(Inventory inv, int n) {
		for (int i = 0; i < n; i++) {
			prev[i] = inv.getItem(i).copy();
		}
	}
}
