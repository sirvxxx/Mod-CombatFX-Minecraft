package dev.rpgcombat.vfx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Busur tebasan dibentuk dari rangkaian partikel slash yang muncul berurutan
 * di sepanjang lengkung. Cara ini hanya memakai API partikel (tanpa render pipeline),
 * jadi tidak bergantung pada nama class render yang sering berubah antar versi.
 */
public final class SlashArcRenderer {
	private SlashArcRenderer() {}

	private static final List<Sweep> ACTIVE = new ArrayList<>();

	/**
	 * @param dir    arah tebasan (tegak lurus normal)
	 * @param normal arah keluar permukaan (menghadap pemain)
	 */
	public static void spawn(ClientLevel level, Vec3 origin, Vec3 dir, Vec3 normal,
			float length, int steps) {
		ACTIVE.add(new Sweep(level, origin, dir.normalize(), normal.normalize(), length, steps));
	}

	/** Dipanggil tiap client tick. */
	public static void tick() {
		Iterator<Sweep> it = ACTIVE.iterator();
		while (it.hasNext()) {
			Sweep s = it.next();
			s.emit();
			if (s.done()) it.remove();
		}
	}

	private static final class Sweep {
		final ClientLevel level;
		final Vec3 origin, dir, normal;
		final float length;
		final int steps;
		int step = 0;

		Sweep(ClientLevel level, Vec3 origin, Vec3 dir, Vec3 normal, float length, int steps) {
			this.level = level;
			this.origin = origin;
			this.dir = dir;
			this.normal = normal;
			this.length = length;
			this.steps = Math.max(1, steps);
		}

		boolean done() {
			return step >= steps;
		}

		void emit() {
			if (level == null) {
				step = steps;
				return;
			}
			int perTick = Math.max(1, 14 / steps);
			for (int i = 0; i < perTick && !done(); i++, step++) {
				float u = (float) step / (steps - 1 <= 0 ? 1 : steps - 1);
				Vec3 p = point(u);
				level.addParticle(ModParticles.SLASH, p.x, p.y, p.z, 0, 0, 0);
			}
		}

		Vec3 point(float u) {
			float ang = (u - 0.5f) * (float) Math.PI * 0.9f;
			float along = (float) Math.sin(ang) * length * 0.5f;
			float bow = (float) (Math.cos(ang) - 1.0) * length * 0.15f;
			return origin.add(dir.scale(along)).add(normal.scale(bow));
		}
	}
}
