package dev.rpgcombat.vfx;

import dev.rpgcombat.RpgCombatClient;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;

// Nama class/method bisa sedikit beda di 26.x. Cek dengan IDE/mapping yang kamu pakai.
public final class ModParticles {
    public static final String MOD_ID = RpgCombatClient.MOD_ID;

    public static final SimpleParticleType SLASH = FabricParticleTypes.simple();
    public static final SimpleParticleType SPARK = FabricParticleTypes.simple();
    public static final SimpleParticleType DEBRIS = FabricParticleTypes.simple();
    public static final SimpleParticleType EXPLOSION = FabricParticleTypes.simple();

    // Dipanggil dari RpgCombatClient (mod ini client-only, jadi registrasi dilakukan di sisi client)
    public static void register() {
        reg("slash", SLASH);
        reg("spark", SPARK);
        reg("debris", DEBRIS);
        reg("explosion", EXPLOSION);
    }

    // Dipanggil di sisi client (ClientModInitializer)
    public static void registerClient() {
        var factories = ParticleFactoryRegistry.getInstance();
        factories.register(SLASH, sprites -> new VfxParticle.Factory(sprites, 6));
        factories.register(SPARK, sprites -> new VfxParticle.Factory(sprites, 8));
        factories.register(DEBRIS, sprites -> new VfxParticle.Factory(sprites, 18));
        factories.register(EXPLOSION, sprites -> new VfxParticle.Factory(sprites, 10));
    }

    private static void reg(String path, SimpleParticleType type) {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                Identifier.fromNamespaceAndPath(MOD_ID, path), type);
    }

    private ModParticles() {}
}
