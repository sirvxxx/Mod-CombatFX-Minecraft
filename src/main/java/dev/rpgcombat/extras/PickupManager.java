package dev.rpgcombat.extras;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Antrean notifikasi loot.
 *
 *  - PENDING : menunggu giliran (mis. 20 item sekaligus, atau layar penuh)
 *  - VISIBLE : sedang tampil, ditumpuk vertikal tanpa tumpang tindih
 *  - item sejenis yang masuk berurutan digabung ("+1" jadi "+5") dan memicu denyut kecil
 *
 * Animasi (semua lerp dari umur/delta time, tanpa objek baru per frame):
 *  masuk  : pop (0.55 -> 1.12 -> 1.0) + geser dari kiri + fade in
 *  diam   : tetap
 *  keluar : meluncur ke kanan (ease-in) + fade out
 *  posisi : Y diredam per frame, jadi tumpukan naik halus saat yang di atas hilang
 */
public final class PickupManager {
	private PickupManager() {}

	private static final List<PickupNotification> VISIBLE = new ArrayList<>();
	private static final ArrayDeque<PickupNotification> PENDING = new ArrayDeque<>();

	private static final long ENTER_MS = 240L;
	private static final long EXIT_MS = 450L;
	private static final float GAP = 4f;

	public static void clear() {
		VISIBLE.clear();
		PENDING.clear();
	}

	// ---------------------------------------------------------------- add
	public static void add(Minecraft mc, ItemStack sample, int delta) {
		ExtrasConfig cfg = ExtrasConfig.get();
		long now = System.currentTimeMillis();

		if (cfg.pickupMergeSame) {
			for (PickupNotification n : VISIBLE) {
				// jangan hidupkan lagi yang sedang meluncur keluar (alpha-nya sudah turun)
				if (now < n.expireMs - EXIT_MS && ItemKinds.same(n.sample, sample)) {
					n.count += delta;
					n.refreshCount(mc.font);
					n.expireMs = now + cfg.pickupDurationMs;
					n.bumpMs = now;
					return;
				}
			}
			for (PickupNotification n : PENDING) {
				if (ItemKinds.same(n.sample, sample)) {
					n.count += delta;
					n.refreshCount(mc.font);
					return;
				}
			}
		}
		PENDING.addLast(new PickupNotification(mc.font, sample, delta, cfg));
	}

	// ------------------------------------------------------------- update
	private static void update(ExtrasConfig cfg, long now, int screenH) {
		for (int i = VISIBLE.size() - 1; i >= 0; i--) {
			if (now >= VISIBLE.get(i).expireMs) VISIBLE.remove(i);
		}

		float budget = screenH * 0.65f;
		float used = 0f;
		for (PickupNotification n : VISIBLE) used += (n.height + GAP) * cfg.pickupScale;

		while (!PENDING.isEmpty() && VISIBLE.size() < cfg.pickupMaxVisible) {
			PickupNotification n = PENDING.peekFirst();
			float h = (n.height + GAP) * cfg.pickupScale;
			if (!VISIBLE.isEmpty() && used + h > budget) break; // tunggu ada ruang
			PENDING.pollFirst();
			n.start(now, cfg.pickupDurationMs);
			VISIBLE.add(n);
			used += h;
		}
	}

	// ------------------------------------------------------------- render
	public static void render(GuiGraphicsExtractor g, Minecraft mc, ExtrasConfig cfg, float dt) {
		var window = mc.getWindow();
		int w = window.getGuiScaledWidth();
		int h = window.getGuiScaledHeight();
		long now = System.currentTimeMillis();

		update(cfg, now, h);
		if (VISIBLE.isEmpty()) return;

		float sc = cfg.pickupScale;
		int right = w - cfg.pickupMarginRight;
		float ty = h * 0.5f + cfg.pickupOffsetY - 30f;
		float follow = Fx.damp(dt, 14f);

		for (int i = 0, n = VISIBLE.size(); i < n; i++) {
			PickupNotification p = VISIBLE.get(i);

			if (!p.placed) {
				p.y = ty;
				p.placed = true;
			} else {
				p.y += (ty - p.y) * follow;
			}
			ty += (p.height + GAP) * sc;

			// ---- animasi masuk / keluar ----
			long age = now - p.popMs;
			long untilExpire = p.expireMs - now;
			float s = 1f;
			float xo = 0f;
			float al = 1f;

			if (age < ENTER_MS) {
				float t = age / (float) ENTER_MS;
				s = t < 0.6f ? Fx.lerp(t / 0.6f, 0.55f, 1.12f) : Fx.lerp((t - 0.6f) / 0.4f, 1.12f, 1f);
				float e = 1f - (1f - t) * (1f - t);
				xo = Fx.lerp(e, -18f, 0f);
				al = Math.min(1f, t * 3f);
			}
			if (untilExpire < EXIT_MS) {
				float t = 1f - Math.max(0f, untilExpire) / (float) EXIT_MS;
				xo += Fx.lerp(t * t, 0f, 90f);   // meluncur ke kanan
				al = Math.min(al, 1f - t);
				s *= Fx.lerp(t, 1f, 0.94f);
			}
			float bump = Math.max(0f, 1f - (now - p.bumpMs) / 200f);
			s *= 1f + 0.12f * bump * bump;

			if (al <= 0.02f) continue;

			g.pose().pushMatrix();
			g.pose().translate(right + xo * sc, p.y + p.height * sc * 0.5f);
			g.pose().scale(sc * s, sc * s);
			g.pose().translate(0f, -p.height * 0.5f);
			p.draw(g, mc.font, al);
			g.pose().popMatrix();
		}
	}
}
