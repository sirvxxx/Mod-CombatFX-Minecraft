package dev.rpgcombat.fx;

import java.util.List;

/** Semua style efek. Menambah style baru = menambah satu baris + satu kunci terjemahan. */
public final class FxStyles {
	private FxStyles() {}

	//                 id        kind     colorA    colorB    shape             particle             particle2          rate  speed spread grav   life  size
	public static final List<FxStyle> TRAILS = List.of(
			new FxStyle("sparkle", "trail", 0xFFF4B0, 0xFFFFFF, FxStyle.PLUS,   "END_ROD",           null,              18f, 46f, 14f, -4f,  0.9f, 1f),
			new FxStyle("flame",   "trail", 0xFFB02E, 0xFF3B1F, FxStyle.FLAME,  "FLAME",             null,              22f, 40f, 12f, -26f, 0.7f, 2f),
			new FxStyle("soul",    "trail", 0x6FE8FF, 0x2B5BFF, FxStyle.FLAME,  "SOUL_FIRE_FLAME",   null,              20f, 38f, 12f, -22f, 0.8f, 2f),
			new FxStyle("hearts",  "trail", 0xFF6AD5, 0xFF3B6B, FxStyle.HEART,  "HEART",             null,              9f,  34f, 16f, -14f, 1.1f, 2f),
			new FxStyle("magic",   "trail", 0xB26BFF, 0x6B2BFF, FxStyle.SQUARE, "WITCH",             null,              20f, 42f, 16f, -6f,  0.9f, 2f),
			new FxStyle("frost",   "trail", 0xDDF6FF, 0x9AD8FF, FxStyle.PLUS,   "SNOWFLAKE",         null,              18f, 36f, 14f, 12f,  1.1f, 1f),
			new FxStyle("static",  "trail", 0xBFEFFF, 0x55FFFF, FxStyle.PLUS,   "ELECTRIC_SPARK",    null,              16f, 60f, 20f, 0f,   0.5f, 1f),
			new FxStyle("cloud",   "trail", 0xFFFFFF, 0xB0B0C0, FxStyle.SQUARE, "CLOUD",             null,              12f, 30f, 10f, -8f,  1.2f, 3f));

	public static final List<FxStyle> HITS = List.of(
			new FxStyle("spark",   "hit",   0xFFE066, 0xFF8A1F, FxStyle.PLUS,   "CRIT",              null,              0f,  90f, 40f, 60f,  0.45f, 1f),
			new FxStyle("arcane",  "hit",   0xB26BFF, 0x6B2BFF, FxStyle.PLUS,   "ENCHANTED_HIT",     null,              0f,  80f, 40f, 30f,  0.5f, 1f),
			new FxStyle("ember",   "hit",   0xFF8A1F, 0xFF3B1F, FxStyle.FLAME,  "FLAME",             null,              0f,  70f, 36f, -30f, 0.55f, 2f),
			new FxStyle("frost",   "hit",   0xDDF6FF, 0x9AD8FF, FxStyle.PLUS,   "SNOWFLAKE",         null,              0f,  70f, 36f, 40f,  0.6f, 1f),
			new FxStyle("static",  "hit",   0xBFEFFF, 0x55FFFF, FxStyle.PLUS,   "ELECTRIC_SPARK",    null,              0f,  100f, 44f, 0f,  0.35f, 1f),
			new FxStyle("realwounds", "hit", 0xC0141F, 0x5C0A0A, FxStyle.PLUS,   "CRIT",              null,              0f,  90f,  40f, 20f, 0.5f, 1f));

	public static final List<FxStyle> KILLS = List.of(
			new FxStyle("burst",   "kill",  0xFFE066, 0xFF3B1F, FxStyle.SQUARE, "CRIT",              "FLAME",           0f,  120f, 50f, 50f, 0.8f, 2f),
			new FxStyle("soul",    "kill",  0x6FE8FF, 0x2B5BFF, FxStyle.FLAME,  "SOUL",              "SOUL_FIRE_FLAME", 0f,  26f,  24f, -40f, 1.4f, 2f),
			new FxStyle("static",  "kill",  0xBFEFFF, 0x55FFFF, FxStyle.PLUS,   "ELECTRIC_SPARK",    "END_ROD",         0f,  110f, 46f, 0f,  0.6f, 1f),
			new FxStyle("hearts",  "kill",  0xFF6AD5, 0xFF3B6B, FxStyle.HEART,  "HEART",             "HAPPY_VILLAGER",  0f,  30f,  26f, -34f, 1.3f, 2f));

	public static FxStyle find(List<FxStyle> list, String id) {
		for (FxStyle s : list) {
			if (s.id().equals(id)) return s;
		}
		return list.get(0);
	}
}
