package dev.rpgcombat.extras;

import java.util.Objects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * "Jenis" item untuk perbandingan inventory: item + enchantment.
 * Durability sengaja diabaikan, kalau tidak setiap pukulan pakai tool akan terbaca sebagai item baru.
 */
final class ItemKinds {
	private ItemKinds() {}

	static boolean same(ItemStack a, ItemStack b) {
		return a.getItem() == b.getItem()
				&& Objects.equals(a.get(DataComponents.ENCHANTMENTS), b.get(DataComponents.ENCHANTMENTS))
				&& Objects.equals(a.get(DataComponents.STORED_ENCHANTMENTS), b.get(DataComponents.STORED_ENCHANTMENTS));
	}
}
