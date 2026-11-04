package dev.rpgcombat.vfx;

import dev.rpgcombat.RpgCombatClient;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Tipe partikel Real Wounds. Mod ini client-only, jadi semua registrasi dilakukan dari RpgCombatClient. */
public final class ModParticles {
	public static final String MOD_ID = RpgCombatClient.MOD_ID;

	public static final SimpleParticleType SLASH = FabricParticleTypes.simple();
	public static final SimpleParticleType SPARK = FabricParticleTypes.simple();
	public static final SimpleParticleType DEBRIS = FabricParticleTypes.simple();
	public static final SimpleParticleType EXPLOSION = FabricParticleTypes.simple();

	/** Dipanggil saat init client, sebelum registri dibekukan. */
	public static void register() {
		reg("slash", SLASH);
		reg("spark", SPARK);
		reg("debris", DEBRIS);
		reg("explosion", EXPLOSION);
	}

	/** Dipanggil setelah register(). */
	public static void registerClient() {
		var providers = ParticleProviderRegistry.getInstance();
		providers.register(SLASH, sprites -> new VfxParticle.Factory(sprites, 6, 0.55f));
		providers.register(SPARK, sprites -> new VfxParticle.Factory(sprites, 8, 0.12f));
		providers.register(DEBRIS, sprites -> new VfxParticle.Factory(sprites, 18, 0.10f));
		providers.register(EXPLOSION, sprites -> new VfxParticle.Factory(sprites, 10, 0.9f));
	}

	private static void reg(String path, SimpleParticleType type) {
		Registry.register(BuiltInRegistries.PARTICLE_TYPE,
				Identifier.fromNamespaceAndPath(MOD_ID, path), type);
	}

	private ModParticles() {}
}
