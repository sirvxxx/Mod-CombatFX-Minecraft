package dev.rpgcombat.vfx;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/** Pintu masuk efek Real Wounds: dipanggil dari FxWorld saat gaya "realwounds" dipilih. */
public final class VfxBridge {
	private VfxBridge() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	private static final int SLASH_COLOR = 0xC8F0FF;

	/**
	 * @param hit    titik kena di tubuh target
	 * @param normal arah keluar permukaan (menghadap pemain)
	 * @param crit   apakah hit kritikal
	 */
	public static void onHit(ClientLevel level, Vec3 hit, Vec3 normal, boolean crit) {
		ThreadLocalRandom r = ThreadLocalRandom.current();

		// arah tebasan: mendatar tegak lurus normal, dimiringkan ke bawah, arah kiri/kanan acak
		Vec3 side = normal.cross(UP);
		if (side.lengthSqr() < 1e-6) side = new Vec3(1, 0, 0);
		side = side.normalize().scale(r.nextBoolean() ? 1 : -1);
		Vec3 slashDir = side.add(UP.scale(-0.6)).normalize();

		SlashArcRenderer.spawn(hit, slashDir, normal, crit ? 1.6f : 1.1f, crit ? 10 : 7, SLASH_COLOR);

		int sparks = crit ? 10 : 5;
		for (int i = 0; i < sparks; i++) {
			Vec3 v = slashDir.scale(0.12).add(normal.scale(0.06 + r.nextDouble() * 0.06));
			level.addParticle(ModParticles.SPARK, hit.x, hit.y, hit.z, v.x, v.y, v.z);
		}

		int debris = crit ? 8 : 3;
		for (int i = 0; i < debris; i++) {
			double ox = (r.nextDouble() - 0.5) * 0.3;
			double oz = (r.nextDouble() - 0.5) * 0.3;
			level.addParticle(ModParticles.DEBRIS, hit.x + ox, hit.y, hit.z + oz,
					slashDir.x * 0.05, 0.05, slashDir.z * 0.05);
		}

		if (crit) {
			level.addParticle(ModParticles.EXPLOSION, hit.x, hit.y, hit.z, 0, 0, 0);
		}
	}
}
