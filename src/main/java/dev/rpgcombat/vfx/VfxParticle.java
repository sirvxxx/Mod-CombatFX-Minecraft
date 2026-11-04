package dev.rpgcombat.vfx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** Partikel quad generik: sprite dari assets/rpgcombat/particles/*.json, pudar di akhir. */
public class VfxParticle extends SingleQuadParticle {
	private final SpriteSet sprites;

	VfxParticle(ClientLevel level, double x, double y, double z,
			double vx, double vy, double vz, SpriteSet sprites, int maxAge, float size) {
		super(level, x, y, z, vx, vy, vz);
		this.sprites = sprites;
		this.lifetime = maxAge;
		this.quadSize = size;
		this.gravity = 0.04f;
		this.hasPhysics = false;
		this.xd = vx;
		this.yd = vy;
		this.zd = vz;
		this.setSpriteFromAge(sprites);
	}

	@Override
	public void tick() {
		super.tick();
		this.setSpriteFromAge(sprites);
	}

	@Override
	protected SingleQuadParticle.Layer getLayer() {
		return SingleQuadParticle.Layer.TRANSLUCENT;
	}

	public static class Factory implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;
		private final int maxAge;
		private final float size;

		public Factory(SpriteSet sprites, int maxAge, float size) {
			this.sprites = sprites;
			this.maxAge = maxAge;
			this.size = size;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level,
				double x, double y, double z, double vx, double vy, double vz, RandomSource random) {
			return new VfxParticle(level, x, y, z, vx, vy, vz, sprites, maxAge, size);
		}
	}
}
