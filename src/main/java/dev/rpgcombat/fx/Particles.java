package dev.rpgcombat.fx;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

/**
 * Menyalakan partikel vanilla tanpa bergantung pada nama field / signature yang bisa berubah:
 *  - jenis partikel dicari lewat nama field di ParticleTypes (mis. "FLAME")
 *  - Level#addParticle dicari lewat signature (ParticleOptions, 6 double)
 * Kalau tidak ketemu, efek dunia dilewati secara senyap (preview di menu tetap jalan).
 */
final class Particles {
	private Particles() {}

	private static final Object MISSING = new Object();
	private static final Map<String, Object> TYPES = new HashMap<>();
	private static Method add;
	private static boolean searched;
	private static boolean broken;

	static Object type(String name) {
		if (name == null) return null;
		Object o = TYPES.get(name);
		if (o == null) {
			try {
				o = ParticleTypes.class.getField(name).get(null);
			} catch (Throwable t) {
				o = MISSING;
			}
			TYPES.put(name, o);
		}
		return o == MISSING ? null : o;
	}

	static void spawn(Level level, Object options, double x, double y, double z, double vx, double vy, double vz) {
		if (broken || options == null) return;
		try {
			if (!searched) {
				searched = true;
				find(options);
			}
			if (add == null) {
				broken = true;
				return;
			}
			add.invoke(level, options, x, y, z, vx, vy, vz);
		} catch (Throwable t) {
			broken = true;
		}
	}

	private static void find(Object sample) {
		for (Method m : Level.class.getMethods()) {
			if (!m.getName().equals("addParticle") || m.getParameterCount() != 7) continue;
			Class<?>[] p = m.getParameterTypes();
			if (!p[0].isInstance(sample)) continue;
			boolean doubles = true;
			for (int i = 1; i < 7; i++) {
				if (p[i] != double.class) doubles = false;
			}
			if (doubles) {
				add = m;
				return;
			}
		}
	}
}
