package dev.rpgcombat.vfx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

// Partikel generik: pakai sprite dari particles/*.json, gerak dengan gravitasi kecil, pudar di akhir.
public class VfxParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    VfxParticle(ClientLevel level, double x, double y, double z,
                double vx, double vy, double vz, SpriteSet sprites, int maxAge) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.lifetime = maxAge;
        this.quadSize = 0.18f;
        this.gravity = 0.04f;
        this.hasPhysics = false;
        this.xd = vx; this.yd = vy; this.zd = vz;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        // Translucent + brightness penuh supaya terlihat menyala
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTicks) {
        return 0xF000F0; // full bright
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final int maxAge;

        public Factory(SpriteSet sprites, int maxAge) {
            this.sprites = sprites;
            this.maxAge = maxAge;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new VfxParticle(level, x, y, z, vx, vy, vz, sprites, maxAge);
        }
    }
}
