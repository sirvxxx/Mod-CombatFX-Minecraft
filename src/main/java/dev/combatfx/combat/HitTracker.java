package dev.combatfx.combat;

import dev.combatfx.CombatConfig;
import dev.combatfx.Config;
import dev.combatfx.render.IndicatorManager;
import dev.combatfx.render.Tier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Client tidak menerima angka damage dari server, jadi damage dihitung dari
 * selisih (HP + absorption) target beberapa tick setelah kita memukul.
 */
public final class HitTracker {
	private HitTracker() {}

	private static final class Pending {
		final int entityId;
		float healthBefore;
		final boolean crit;
		int ageTicks = 0;

		Pending(int entityId, float healthBefore, boolean crit) {
			this.entityId = entityId;
			this.healthBefore = healthBefore;
			this.crit = crit;
		}
	}

	private static final List<Pending> PENDING = new ArrayList<>();

	public static InteractionResult onAttack(Player player, Level level, InteractionHand hand,
			Entity target, EntityHitResult hit) {
		Minecraft mc = Minecraft.getInstance();
		if (!CombatConfig.get().enabled) return InteractionResult.PASS;
		if (!level.isClientSide() || player != mc.player) return InteractionResult.PASS;
		if (!(target instanceof LivingEntity living) || !living.isAlive()) return InteractionResult.PASS;

		if (PENDING.size() >= Config.MAX_PENDING) PENDING.remove(0);
		PENDING.add(new Pending(
				living.getId(),
				living.getHealth() + living.getAbsorptionAmount(),
				isCrit(player)
		));
		return InteractionResult.PASS;
	}

	public static void onClientTick(Minecraft mc) {
		if (mc.level == null || mc.player == null || !CombatConfig.get().enabled) {
			PENDING.clear();
			IndicatorManager.clear();
			ComboManager.reset();
			return;
		}

		CombatConfig cfg = CombatConfig.get();
		long now = System.currentTimeMillis();
		IndicatorManager.prune(now);

		for (int i = PENDING.size() - 1; i >= 0; i--) {
			Pending p = PENDING.get(i);
			p.ageTicks++;

			if (mc.level.getEntity(p.entityId) instanceof LivingEntity le) {
				float current = le.getHealth() + le.getAbsorptionAmount();
				float damage = p.healthBefore - current;

				if (damage > 0.05f) {
					Tier tier = p.crit ? Tier.CRIT
							: (damage >= cfg.heavyDamage ? Tier.HEAVY : Tier.NORMAL);

					ThreadLocalRandom rnd = ThreadLocalRandom.current();
					IndicatorManager.add(
							le.getX() + rnd.nextDouble(-0.25, 0.25),
							le.getY() + le.getBbHeight() + 0.15,
							le.getZ() + rnd.nextDouble(-0.25, 0.25),
							format(damage), tier, now
					);
					ComboManager.registerHit(now);

					for (Pending other : PENDING) {
						if (other != p && other.entityId == p.entityId) other.healthBefore = current;
					}
					PENDING.remove(i);
					continue;
				}
			}

			if (p.ageTicks >= Config.PENDING_TIMEOUT_TICKS) {
				PENDING.remove(i);
			}
		}
	}

	/** Meniru kondisi critical vanilla (Player#attack) di sisi client. */
	private static boolean isCrit(Player p) {
		return p.getAttackStrengthScale(0.5F) > 0.9F
				&& p.fallDistance > 0.0
				&& !p.onGround()
				&& !p.onClimbable()
				&& !p.isInWater()
				&& !p.hasEffect(MobEffects.BLINDNESS)
				&& !p.isPassenger()
				&& !p.isSprinting();
	}

	private static String format(float d) {
		int rounded = Math.round(d);
		if (Math.abs(d - rounded) < 0.05f) return Integer.toString(rounded);
		return String.format(Locale.ROOT, "%.1f", d);
	}
}
