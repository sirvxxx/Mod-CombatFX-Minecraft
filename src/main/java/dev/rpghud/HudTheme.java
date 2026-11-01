package dev.rpghud;

import net.minecraft.resources.Identifier;

public final class HudTheme {
    public static final String DEFAULT_ID = "netherite";
    public final String id;
    public final boolean auto;
    public final int text;
    public final int level;
    public final int hp;
    public final int food;
    public final int armor;
    public final int ghost;
    public final int sat;
    public final int[] empty;
    public final int bandBack;
    public final Identifier frame;
    public final Identifier badge;
    public final Identifier ring;
    public final Identifier iconCoords;
    public final Identifier iconFps;
    public final Identifier iconHeart;
    public final Identifier iconFood;
    public final Identifier iconArmor;
    public int index;
    public static final HudTheme[] ALL = new HudTheme[]{new HudTheme("hardcore", -42417, -33941, -3664876, -1540070, -4933956, -1521472, -9824, -15003374, -14017507, -13097433, -16120570, -16382971), new HudTheme("netherite", -11413, -6488198, -2086358, -24804, -3880238, -1319496, -3424, -14805743, -13885158, -12964572, -16054523, -16317173), new HudTheme("forest", -8519776, -4653206, -2608594, -1531350, -5193800, -1646408, -724854, -15458278, -14733787, -13943503, -16315127, -16446713), new HudTheme("celestial", -6502145, -3612417, -2079398, -19641, -2695438, -1513217, -3920, -15525848, -14867398, -14077360, -16250350, -16381936), new HudTheme("cyber", -10685697, -8519712, -51104, -15360, -6442300, -3084043, -96, -15853024, -15325650, -14666431, -16446448, -16578806), new HudTheme("classic", -997256, -4661114, -4180436, -2061782, -4673376, -1319496, -861046, -14280174, -13293796, -12307418, -15726074, -15923195), new HudTheme("auto", "netherite", -11413, -6488198, -2086358, -24804, -3880238, -1319496, -3424, -14805743, -13885158, -12964572, -16054523, -16317173)};

    private HudTheme(String id, int text, int level, int hp, int food, int armor, int ghost, int sat, int e0, int e1, int e2, int e3, int bandBack) {
        this(id, id, text, level, hp, food, armor, ghost, sat, e0, e1, e2, e3, bandBack);
    }

    /** texDir: folder tekstur ikon (tema "auto" meminjam ikon netherite; bingkai digambar dari resource pack). */
    private HudTheme(String id, String texDir, int text, int level, int hp, int food, int armor, int ghost, int sat, int e0, int e1, int e2, int e3, int bandBack) {
        this.id = id;
        this.auto = "auto".equals(id);
        this.text = text;
        this.level = level;
        this.hp = hp;
        this.food = food;
        this.armor = armor;
        this.ghost = ghost;
        this.sat = sat;
        this.empty = new int[]{e0, e1, e2, e3};
        this.bandBack = bandBack;
        this.frame = HudTheme.tex(texDir, "frame");
        this.badge = HudTheme.tex(texDir, "badge");
        this.ring = HudTheme.tex(texDir, "ring");
        this.iconCoords = HudTheme.tex(texDir, "icon_coords");
        this.iconFps = HudTheme.tex(texDir, "icon_fps");
        this.iconHeart = HudTheme.tex(texDir, "icon_heart");
        this.iconFood = HudTheme.tex(texDir, "icon_food");
        this.iconArmor = HudTheme.tex(texDir, "icon_armor");
    }

    private static Identifier tex(String id, String name) {
        return Identifier.fromNamespaceAndPath((String)"custom_rpg_hud", (String)("textures/gui/" + id + "/" + name + ".png"));
    }

    public static HudTheme byId(String id) {
        if (id != null) {
            for (HudTheme t : ALL) {
                if (!t.id.equals(id)) continue;
                return t;
            }
        }
        for (HudTheme t : ALL) {
            if (!t.id.equals(DEFAULT_ID)) continue;
            return t;
        }
        return ALL[0];
    }

    public static HudTheme next(HudTheme current) {
        for (int i = 0; i < ALL.length; ++i) {
            if (ALL[i] != current) continue;
            return ALL[(i + 1) % ALL.length];
        }
        return ALL[0];
    }

    public String translationKey() {
        return "custom_rpg_hud.theme." + this.id;
    }

    static {
        for (int i = 0; i < ALL.length; ++i) {
            HudTheme.ALL[i].index = i;
        }
    }
}

