package dev.rpgcombat.fx;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Trail, efek hit, dan efek kill di dunia (client-side, hanya partikel). */
public final class FxWorld {
	private FxWorld() {}

	private static final int MAX_PER_EVENT = 60;

	/** Dipanggil tiap tick klien. */
	public static void tick(Minecraft mc) {
		FxConfig c = FxConfig.get();
		LocalPlayer p = mc.player;
		if (!c.trailEnabled || p == null || mc.level == null) return;
		if (!c.trailFirstPerson && mc.options.getCameraType().isFirstPerson()) return;

		Vec3 mv = p.getDeltaMovement();
		double sp = Math.hypot(mv.x, mv.z);
		if (sp < 0.05) return;
		if (c.trailSprintOnly && !p.isSprinting()) return;

		FxStyle s = FxStyles.find(FxStyles.TRAILS, c.trailStyle);
		ThreadLocalRandom r = ThreadLocalRandom.current();
		// rate (per detik) -> peluang per tick, bisa >1 untuk density tinggi
		float want = s.rate() / 20f * c.trailDensity;
		int n = (int) want;
		if (r.nextFloat() < want - n) n++;
		double vel = s.speed() * 0.0009;
		for (int i = 0; i < n; i++) {
			Object type = Particles.type(r.nextBoolean() || s.particle2() == null ? s.particle() : s.particle2());
			double ox = (r.nextDouble() - 0.5) * 0.35;
			double oz = (r.nextDouble() - 0.5) * 0.35;
			double vx = -mv.x * 0.25 + (r.nextDouble() - 0.5) * vel * 0.8;
			double vz = -mv.z * 0.25 + (r.nextDouble() - 0.5) * vel * 0.8;
			double vy = s.gravity() < 0 ? vel * (0.3 + r.nextDouble()) : (r.nextDouble() - 0.5) * vel * 0.4;
			Particles.spawn(mc.level, type, p.getX() + ox, p.getY() + 0.08, p.getZ() + oz, vx, vy, vz);
		}
	}

	/** Dipanggil saat sebuah serangan terkonfirmasi mengurangi HP target. */
	public static void onHit(Minecraft mc, LivingEntity target, boolean crit) {
		FxConfig c = FxConfig.get();
		if (!c.hitEnabled || mc.level == null || (c.hitCritOnly && !crit)) return;
		FxStyle s = FxStyles.find(FxStyles.HITS, c.hitStyle);
		burst(mc, target, s, Math.round((crit ? 16 : 8) * c.hitIntensity), 0.6);
	}

	/** Dipanggil saat serangan kita menghabiskan HP target. */
	public static void onKill(Minecraft mc, LivingEntity target) {
		FxConfig c = FxConfig.get();
		if (!c.killEnabled || mc.level == null) return;
		FxStyle s = FxStyles.find(FxStyles.KILLS, c.killStyle);
		burst(mc, target, s, Math.round(30 * c.killIntensity), 0.5);
	}

	private static void burst(Minecraft mc, LivingEntity t, FxStyle s, int count, double heightFrac) {
		ThreadLocalRandom r = ThreadLocalRandom.current();
		int n = Math.max(1, Math.min(MAX_PER_EVENT, count));
		double cx = t.getX();
		double cy = t.getY() + t.getBbHeight() * heightFrac;
		double cz = t.getZ();
		double vel = s.speed() * 0.0016;
		boolean rise = s.gravity() < 0;
		for (int i = 0; i < n; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			double b = (r.nextDouble() - 0.5) * Math.PI;
			double k = vel * (0.4 + r.nextDouble() * 0.8);
			double vx = Math.cos(a) * Math.cos(b) * k;
			double vz = Math.sin(a) * Math.cos(b) * k;
			double vy = Math.sin(b) * k + (rise ? vel * 0.5 : 0.0);
			Object type = Particles.type(s.particle2() != null && r.nextBoolean() ? s.particle2() : s.particle());
			Particles.spawn(mc.level, type, cx + (r.nextDouble() - 0.5) * 0.3, cy, cz + (r.nextDouble() - 0.5) * 0.3, vx, vy, vz);
		}
	}
}
