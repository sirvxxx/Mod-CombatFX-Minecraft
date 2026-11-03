package dev.rpgcombat.menu;

import dev.rpgcombat.ui.Icon;

public enum Category {
	COMBAT("rpgcombat.cat.combat", "rpgcombat.cat.combat.sub", Icon.SWORD),
	COSMETICS("rpgcombat.cat.cosmetics", "rpgcombat.cat.cosmetics.sub", Icon.STAR),
	HUD("rpgcombat.cat.hud", "rpgcombat.cat.hud.sub", Icon.HUD),
	QOL("rpgcombat.cat.qol", "rpgcombat.cat.qol.sub", Icon.GEAR);

	public final String titleKey;
	public final String subKey;
	public final Icon icon;

	Category(String titleKey, String subKey, Icon icon) {
		this.titleKey = titleKey;
		this.subKey = subKey;
		this.icon = icon;
	}
}
