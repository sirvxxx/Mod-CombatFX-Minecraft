# COMBATFX RPG — GUI Redesign Guide (Card Deck + Live Preview)

Target: Fabric, Minecraft **26.2** (workspace). Reference studied: *Kangetsu Visuals 1.7.3+26.3*.

---

## 0. Ground rules (please read first)

**License of the reference.** `fabric.mod.json` of Kangetsu says `All-Rights-Reserved`, and `LICENSE_kangetsu-visuals`
says "Copyright (c) 2026 Kangetsu. All rights reserved." Therefore:

- I only looked at **public structure**: package names, class names, mixin target names, asset folder names, and your screenshots.
- I did **not** decompile method bodies and I do **not** reuse its textures, shaders, sounds or code.
- Everything below is an **original design** that reproduces the *kind* of UX (card deck, preview canvas, search, favorites),
  not Kangetsu's implementation. Visuals, names, colors, sounds and layouts must be your own.
- Only exception worth noting: Kangetsu bundles **Inter** and **Poppins** (SIL OFL 1.1). Those fonts are free to use in your
  mod, but download them from their official sources (rsms/inter, Indian Type Foundry) and ship the OFL text, not from the jar.

**Version honesty (26.2 vs 26.3).** I found no official GUI porting notes between 26.2 and 26.3. What is documented:
Fabric's 26.3 announcement says the technical changes "are focused primarily on rendering", needs Loom 1.17 / Gradle 9.6,
and community ports mention a `LevelExtractor` rewrite on the **world** rendering side. The GUI stack
(`GuiGraphicsExtractor`, `extractRenderState`, `Matrix3x2fStack` pose) is the same family in 26.1.2 and 26.2.
So: this guide only uses (a) APIs already **compiled and running on your 26.2 build**, or (b) APIs found by
**reflection by signature**, isolated in one `compat/` package. If 26.3 changes something, only `compat/` changes.

Legend used below: ✅ proven on your 26.2 build · 🔎 discovered by reflection (fails soft) · ⚠️ unverified, needs a 10-minute spike.

---

## 1. What the reference teaches (structure only)

From class names alone:

| Reference package (names only) | Role we infer | Our equivalent |
|---|---|---|
| `gui` : `OrbitScreen`, `KangetsuScreen`, `Theme`, `Draw`, `ShapeTextures`, `PosedSkin`, `Intro`, `MenuHooks` | one screen for the radial hub, one for category pages; shared theme/drawing helpers; a posed player renderer | `CardDeckScreen`, `CategoryScreen`, `ui/Theme`, `ui/Draw`, `preview/PlayerStage` |
| `gui/widget` : `Row` + `ToggleRow`, `SliderRow`, `ChoiceRow`, `ColorRow`, `TileGridRow`, `FeatureGridRow`, `PlayerPreviewRow`, `AuraPreviewRow`, `TexturePreviewRow`… | pages are **lists of typed rows**, not hand-placed widgets | `ui/row/*` (one `Row` interface, many small row classes) |
| `combat` : `Trails`, `KillAnims`, `KillPreview`, `HitEffects`, `TrailStyle` | effects have a *style* object and a *preview* path | shared `FxStyle` used by both world + preview |
| `tools` : `ChestPreview` | cached container preview | `hud/ChestHud` |
| `config` : single `KangetsuConfig` | one config | you already have `ModConfig` |

Key idea to copy (as a *pattern*, not code): **a page is data** (`List<Row>`), the screen just lays rows out and routes input.
That is what makes 30 sub-menus cheap to build.

---

## 2. Target architecture

```
dev.rpgcombat
├─ ui/                       shared look & feel
│  ├─ Theme.java             colors, radii, timings (tokens)
│  ├─ Draw.java              roundRect, glass, gradient, shadow, icon-circle
│  ├─ Anim.java              Spring / ease helpers (delta-time, allocation-free)
│  └─ UiSounds.java          local click / whoosh sounds
├─ ui/row/                   Row (interface) + ToggleRow, SliderRow, ChoiceRow, ColorRow, TileGridRow, PreviewRow
├─ menu/
│  ├─ Feature.java           id, category, title/subtitle keys, icon, hasToggle, pages
│  ├─ FeatureRegistry.java   all features + search index + favorites
│  ├─ Category.java          COMBAT, COSMETICS, HUD, QOL
│  ├─ CardDeckScreen.java    main hub (carousel of cards around the player)
│  └─ CategoryScreen.java    side nav + sub-menu grid + feature page + search bar
├─ preview/
│  ├─ PreviewCanvas.java     rect + scissor + owns a PreviewScene
│  ├─ PreviewScene.java      tick(dt) / render(g, rect, mouse)
│  ├─ PlayerStage.java       3D entity (🔎) or paper-doll (✅) behind one interface
│  ├─ ParticlePool.java      struct-of-arrays 2D particles (no per-frame allocation)
│  ├─ FxStyle.java           ONE description of an effect, used by world AND preview
│  └─ scenes/TrailScene, KillScene, HitScene, JumpScene, ...
├─ hud/ChestHud.java         glass chest popup + cache of last-seen containers
└─ compat/                   everything unverified lives here (reflection, 26.3 differences)
```

Rules that keep it modular:
1. `ui/*` knows nothing about features. `menu/*` knows nothing about particles. `preview/*` knows nothing about screens.
2. Every unverified Minecraft call is behind `compat/` (one class per concern: `EntityRender`, `SoundCompat`, `ScreenCompat`).
3. Input always goes through the GLFW poller you already have (`MouseInput`) — the vanilla `isLeftPressed()` is dead inside screens.

---

## 3. Phased plan (what to build in which order)

| Phase | Deliverable | Risk |
|---|---|---|
| 1 | `Theme`, `Draw`, `Anim`, `CardDeckScreen` (deck + transition), `CategoryScreen` (nav + grid), `UiSounds`, search + favorites | low (only ✅ APIs) |
| 2 | `PreviewCanvas`, paper-doll stage, `ParticlePool`, Trails/Kills/Hits scenes | low–medium |
| 3 | 3D entity stage via reflection, real in-world trail/kill effects that share `FxStyle` | medium (spikes in §9) |
| 4 | `ChestHud` glass popup with active-pack slot texture + container cache | medium |

Do Phase 1 first. It is shippable alone and replaces the radial menu completely.

---

## 4. Shared UI toolkit (Phase 1)

### 4.1 `Anim` — delta-time springs (no allocation, framerate independent)

```java
public final class Anim {
    /** Critically-damped-ish spring. Update once per frame with real dt (seconds). */
    public static final class Spring {
        public float value, velocity, target;
        private final float stiffness, damping;
        public Spring(float initial, float stiffness) {
            this.value = this.target = initial;
            this.stiffness = stiffness;
            this.damping = 2f * (float) Math.sqrt(stiffness);   // critical damping
        }
        public void update(float dt) {
            dt = Math.min(dt, 1f / 30f);                        // stability when the game hitches
            float accel = stiffness * (target - value) - damping * velocity;
            velocity += accel * dt;
            value += velocity * dt;
        }
        public boolean settled() { return Math.abs(target - value) < 0.001f && Math.abs(velocity) < 0.01f; }
    }

    public static float lerp(float t, float a, float b) { return a + (b - a) * t; }
    public static float clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
    public static float easeOutCubic(float t) { float i = 1 - t; return 1 - i * i * i; }
}
```

### 4.2 `Draw` — rounded rectangles and glass using only `fill` (✅)

`GuiGraphicsExtractor.fill` is axis-aligned, so rounded corners are built from per-row insets. Cost is `O(radius)` fills.

```java
public final class Draw {
    public static void roundRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int argb) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        g.fill(x, y + r, x + w, y + h - r, argb);                       // middle band
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, argb);               // top
            g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, argb);       // bottom
        }
    }

    /** Glassmorphism: translucent body + brighter top sheen + 1px border. Real blur is not available in the GUI pass. */
    public static void glass(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, float alpha, int tint) {
        roundRect(g, x - 1, y - 1, w + 2, h + 2, r + 1, a(0.35f * alpha, 0xFFFFFF));          // border
        roundRect(g, x, y, w, h, r, a(0.62f * alpha, tint));                                   // body
        int sheen = Math.max(2, h / 3);
        for (int i = 0; i < sheen; i++) {                                                      // vertical gradient sheen
            float k = 1f - i / (float) sheen;
            g.fill(x + r, y + 1 + i, x + w - r, y + 2 + i, a(0.10f * k * alpha, 0xFFFFFF));
        }
    }

    public static void shadow(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, float strength) {
        for (int i = 4; i >= 1; i--) {
            roundRect(g, x - i, y - i + 3, w + 2 * i, h + 2 * i, r + i, a(0.07f * strength, 0x000000));
        }
    }

    static int a(float alpha, int rgb) {
        int al = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }
}
```

### 4.3 `Theme` — design tokens, one place to restyle

```java
public final class Theme {
    public static final int ACCENT = 0x23E6B4;       // your own accent; do not copy another mod's palette
    public static final int PANEL = 0x0B1218;
    public static final int TEXT = 0xE8EEF6;
    public static final int TEXT_DIM = 0x8C98A8;
    public static final int RADIUS = 10;
    public static final float MOVE_STIFFNESS = 170f;  // card movement
    public static final float FADE_STIFFNESS = 260f;
}
```

If you want a clean font, bundle Inter via a font definition JSON (`assets/rpgcombat/font/ui.json`, `"type": "ttf"`).
Applying a custom font to drawn text goes through `Style`/font-description APIs that changed in 1.21.9+ → ⚠️ spike it (§9) and keep the vanilla font as fallback.

---

## 5. Card Deck hub (`CardDeckScreen`)

### 5.1 Layout math — "floating asymmetrical deck"

Cards sit on an arc *in front of / around* the player. For card `i` of `n`:

```java
record CardPose(float x, float y, float scale, float roll, float alpha, int z) {}

CardPose deckPose(int i, int n, float scroll, int W, int H) {
    float t = (i - (n - 1) / 2f) + scroll;          // scroll = fractional carousel offset (spring)
    float ang = t * 0.46f;                           // spread in radians
    float rx = W * 0.34f, ry = H * 0.10f;
    float x = W / 2f + (float) Math.sin(ang) * rx;
    float y = H * 0.62f + (1f - (float) Math.cos(ang)) * ry * 3f + ASYM[i % ASYM.length];  // asymmetric nudges
    float depth = (float) Math.cos(ang);             // 1 in front, <1 to the sides
    return new CardPose(x, y, 0.72f + 0.28f * depth, ang * 0.30f, 0.55f + 0.45f * depth,
            Math.round(depth * 100));
}
private static final float[] ASYM = {0f, -16f, 9f, -5f, 12f};
```

Draw order = sort by `z` ascending (cheap: insertion sort on ≤8 elements, no allocation if you reuse an `int[] order`).
Each card is drawn with the pose stack (✅ `translate / rotate / scale` on `Matrix3x2fStack`):

```java
g.pose().pushMatrix();
g.pose().translate(p.x(), p.y());
g.pose().rotate(p.roll());                           // JOML Matrix3x2f#rotate(angleRadians)
g.pose().scale(p.scale(), p.scale());
Draw.shadow(g, -CW/2, -CH/2, CW, CH, Theme.RADIUS, p.alpha());
Draw.glass(g, -CW/2, -CH/2, CW, CH, Theme.RADIUS, p.alpha(), hovered ? 0x123A33 : Theme.PANEL);
// icon circle + title (text uses the same transformed pose)
g.pose().popMatrix();
```

Hover/hit-test on a rotated card = inverse-map the cursor into card space (same math as the `Spatial.unmap` I wrote earlier):
subtract the card center, rotate by `-roll`, divide by `scale`, test against `±CW/2, ±CH/2`.

### 5.2 The player in the middle (idle / ambient)

`PlayerStage` (see §7.2) renders either the real 3D player or a paper-doll. "Ambient" = three cheap layers:
1. idle sway: `yaw = sin(time*0.6)*8°`, breathing bob `y += sin(time*1.4)*1px`,
2. a ground ring made of 3 concentric flattened circles (use the `Draw.ring` helper = per-angle `fill` points like the mining bar), slowly rotating,
3. ~30 floating motes from `ParticlePool` (drift upward, wrap around).

### 5.3 Transition state machine (deck → category)

```
DECK ──click card──▶ TO_CATEGORY ──(spring settles)──▶ CATEGORY
CATEGORY ──back/ESC──▶ TO_DECK ──▶ DECK
```

One `Spring open` (0 → 1). Every card has a `deckPose` and a `navPose`:

- the clicked card: `navPose` = header slot of the left navigation panel (x=16, y=16, scale 0.8, roll 0),
- other cards: `navPose` = small icon buttons stacked below it (the "rail"),
- player stage: slides to the bottom-left corner and shrinks (this is where the ambient ring stays visible as in a mini-hub).

```java
CardPose pose = lerp(open.value, deck, nav);        // lerp each field; roll → 0, alpha → 1
float content = Anim.clamp01((open.value - 0.45f) / 0.55f);   // right-hand grid fades in AFTER the card arrives
for (int k = 0; k < tiles.size(); k++) {
    float a = Anim.clamp01(content * 3f - k * 0.25f);          // stagger per tile
    ...
}
```

`open.target = 1` on click, `0` on back. Because it is a spring, interrupting mid-transition (clicking back quickly)
is smooth for free — that is the main reason to prefer springs over fixed-time tweens.

### 5.4 Input (GLFW poller you already have)

Reuse `MouseInput.window / left / cursor`. Pattern: `press edge → hit-test → set target`, with the
"ignore a click that was already held when the screen opened" guard. Keyboard: use `keyPressed` only for ESC/arrow
if you verify the signature in the spike; otherwise ESC already works via the base `Screen`.
Scroll-to-rotate-deck needs `mouseScrolled` → ⚠️ spike; fallback = `◀ ▶` buttons (plain vanilla `Button`, ✅).

---

## 6. Category screen: nav + sub-menu grid + search + favorites

### 6.1 Pages are data

```java
public interface Row {
    int height();                                   // layout asks, row answers
    void render(GuiGraphicsExtractor g, int x, int y, int w, float alpha, int mx, int my);
    default boolean click(int x, int y, int w, int mx, int my) { return false; }
    default String searchText() { return ""; }      // used by the search bar
}
```
Rows: `ToggleRow`, `SliderRow`, `ChoiceRow`, `ColorRow`, `TileGridRow`, `PreviewRow(PreviewCanvas)`.
A feature page is just `List<Row> rows()`. Adding "Kills → Style" = 5 lines, no layout code.

### 6.2 Feature registry, favorites, search

```java
public record Feature(String id, Category cat, String titleKey, String subKey, Supplier<List<Row>> page) {}

public final class FeatureRegistry {
    public static final List<Feature> ALL = new ArrayList<>();

    public static List<Feature> query(String q, Category cat, Set<String> favorites) {
        String s = q.trim().toLowerCase(Locale.ROOT);
        List<Feature> out = new ArrayList<>();
        for (Feature f : ALL) {
            if (cat != null && f.cat() != cat && s.isEmpty()) continue;           // global search ignores category
            if (s.isEmpty() || matches(f, s)) out.add(f);
        }
        out.sort(Comparator.comparing((Feature f) -> !favorites.contains(f.id()))   // favorites first
                           .thenComparing(f -> I18n.get(f.titleKey())));
        return out;
    }
    private static boolean matches(Feature f, String s) {
        if (I18n.get(f.titleKey()).toLowerCase(Locale.ROOT).contains(s)) return true;
        for (Row r : f.page().get()) if (r.searchText().toLowerCase(Locale.ROOT).contains(s)) return true;  // "sparkle" finds Trails
        return false;
    }
}
```
(`I18n` = `net.minecraft.client.resources.language.I18n`, ⚠️ confirm path or use `Component.translatable(key).getString()` ✅.)

Favorites persist in config: add `Set<String> favorites` to `ModConfig` (Gson handles `Set<String>`). A star button on each tile toggles it.

### 6.3 Search bar

Use the vanilla `EditBox` (✅ widget): draw your own glass background behind it and call `setBordered(false)`;
`setHint(Component)` for the placeholder and `setResponder(text -> grid.refilter(text))`.
Filtering happens on text change only (not per frame).

### 6.4 Grid tile (right side, as in your screenshot 3)

Tile = glass card with: icon circle, title, subtitle ("3 settings" / live status), optional toggle, chevron, star.
Hover = spring-driven scale 1.0→1.03 and border brightening. Layout = `cols = max(1, (areaW + gap) / (tileW + gap))`.

---

## 7. Live preview system (Phase 2–3)

### 7.1 One effect description, two renderers

The preview must look like the real thing, so **do not write effects twice**:

```java
public record FxStyle(
    String id, int[] gradient, float rate, float life, float speed, float spread,
    float gravity, float size, Shape shape /* DOT, STAR, FLAME, RING */) {}
```

- **World renderer** (real trail): each tick spawn vanilla particles at the player's feet. ✅ the call you will use is
  `level.addParticle(...)` on the client; map `FxStyle` to `ParticleTypes` + velocities. (⚠️ confirm the 26.2 particle option signature in the spike.)
- **Preview renderer** (GUI): 2D `ParticlePool` spawning from the doll's feet with the *same* numbers.

### 7.2 `PlayerStage` — one interface, two implementations

```java
public interface PlayerStage {
    void tick(float dt, boolean walking);
    void render(GuiGraphicsExtractor g, int x, int y, int w, int h, float mouseX, float mouseY);
    /** where trail particles should be born, in canvas coordinates */
    float feetX(); float feetY();
}
```

**A. `DollStage` (✅ no risky API).** A flat front-view "paper doll" built from the skin texture you already load (`SkinHelper`/`HudState.skin`).
Skin 64×64 front regions: head (8,8,8,8), body (20,20,8,12), right arm (44,20,4,12), left arm (36,52,4,12), right leg (4,20,4,12), left leg (20,52,4,12) (+ overlay layers at the 2nd-layer offsets).
Walking = rotate limbs around hip/shoulder pivots with the 3×2 pose:

```java
void limb(GuiGraphicsExtractor g, Identifier skin, int u, int v, int w, int h, float px, float py, float angle) {
    g.pose().pushMatrix();
    g.pose().translate(px, py);                       // pivot (shoulder / hip)
    g.pose().rotate(angle);
    g.blit(RenderPipelines.GUI_TEXTURED, skin, -w * S / 2, 0, (float) u, (float) v, w * S, h * S, w, h, 64, 64);
    g.pose().popMatrix();
}
// walk cycle: legs ±sin(t*8)*0.55 rad, arms ∓sin(t*8)*0.45 rad, body bob |sin(t*8)|*1px
```
It is not "real 3D", but it is deterministic, cheap, and always works. Use it for Trails/Hits scenes if Phase 3 stalls.

**B. `EntityStage` (🔎 real 3D).** Vanilla draws the inventory player with a static method on `InventoryScreen`
(`drawEntity(...)` in 1.21.5 per the mappings; renamed to an `extract…` variant in 26.x). Find it by **signature**, not by name:

```java
// compat/EntityRender.java
static Method find() {
    for (Method m : InventoryScreen.class.getDeclaredMethods()) {
        Class<?>[] p = m.getParameterTypes();
        if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) && p.length == 10
            && p[0] == GuiGraphicsExtractor.class
            && p[1] == int.class && p[2] == int.class && p[3] == int.class && p[4] == int.class   // x0 y0 x1 y1
            && p[5] == int.class                                                                   // scale
            && p[6] == float.class && p[7] == float.class && p[8] == float.class                  // yOffset mouseX mouseY
            && LivingEntity.class.isAssignableFrom(p[9])) { m.setAccessible(true); return m; }
    }
    return null;                       // → fall back to DollStage
}
```
Idle rotation = pass a synthetic `mouseX/mouseY` that orbits slowly. Walking in 3D is the hard part: the pose comes from the entity's
`walkAnimation`. Options, in order of safety: (1) keep the real player idle in 3D and show walking only in `DollStage`;
(2) ⚠️ clone the local player into a client-only dummy and drive its `walkAnimation` each frame (needs a spike);
(3) accept a static 3D pose and spawn the trail behind it.
Decision rule: ship (1) first; upgrade to (2) only if the spike compiles.

### 7.3 `PreviewCanvas` — clipping, framing, input

```java
public final class PreviewCanvas {
    private final PreviewScene scene;
    public void render(GuiGraphicsExtractor g, int x, int y, int w, int h, float dt, int mx, int my) {
        Draw.glass(g, x, y, w, h, Theme.RADIUS, 1f, 0x070C11);
        g.enableScissor(x, y, x + w, y + h);          // everything the scene draws stays inside the rounded frame
        scene.tick(dt);
        scene.render(g, x, y, w, h, mx, my);
        g.disableScissor();
    }
}
```
(`enableScissor/disableScissor` are documented on `GuiGraphicsExtractor`; scissor is rectangular, so draw a rounded frame on top to hide the square corners.)

### 7.4 Particle pool (zero allocation)

```java
public final class ParticlePool {
    final float[] x, y, vx, vy, age, life, size; final int[] color; int n;
    public ParticlePool(int cap) { x = new float[cap]; /* ... */ }
    public void spawn(float px, float py, FxStyle s, RandomSource rnd) { if (n == x.length) return; /* init slot n++ */ }
    public void tick(float dt, float gravity) {
        for (int i = n - 1; i >= 0; i--) {
            age[i] += dt; if (age[i] >= life[i]) { remove(i); continue; }
            vy[i] += gravity * dt; x[i] += vx[i] * dt; y[i] += vy[i] * dt;
        }
    }
    public void draw(GuiGraphicsExtractor g) {
        for (int i = 0; i < n; i++) {
            float k = age[i] / life[i];
            int a = (int) ((1f - k) * 255); int s = Math.max(1, Math.round(size[i] * (1f - 0.5f * k)));
            g.fill((int) x[i], (int) y[i], (int) x[i] + s, (int) y[i] + s, (a << 24) | gradient(k));
        }
    }
    private void remove(int i) { /* swap with last, n-- */ }
}
```
`remove` swaps with the last element so the loop stays O(n) and allocation-free. Cap 256 per scene.

### 7.5 Scenes

| Scene | What loops | Notes |
|---|---|---|
| `TrailScene` | doll walks in place, trail spawns at feet and streams backward (world moves left: add `-speed` to `vx`) | preview follows the selected `FxStyle` live (Sparkle/Flame/...) |
| `KillScene` | dummy target (box built from fills) stands; every 2.5 s an "attacker flash" → kill animation (burst + fade) → respawn | uses the same `FxStyle` for the death burst |
| `HitScene` | dummy takes a hit every 1 s: damage number floats (reuse `FloatingText` math), spark particles | uses your existing Tier colors |
| `JumpScene` / `MaceScene` | doll jumps on a sine, ring shockwave on landing | pure math |

Selecting a different effect only swaps the `FxStyle` reference (spring crossfade of the particle tint), no scene rebuild.

---

## 8. Chest HUD (Phase 4)

Your screenshot 5 shows: crosshair on a chest → glass popup with name, "N stacks · seen just now", and a slot grid.

1. **Cache.** The client does not know chest contents until you open it. When a container screen closes (hook the screen's
   `removed()` via Fabric `ScreenEvents.REMOVE`, ⚠️ confirm event name in 26.2, or read it from `mc.player.containerMenu` just before it resets),
   store `ItemStack` copies by `(dimension, BlockPos)` + `System.currentTimeMillis()`. Large chests are double-chests: key both halves to the same entry.
2. **Trigger.** Each tick, `mc.hitResult instanceof BlockHitResult` → look up the cache for that pos. Draw only when found.
3. **Look.** `Draw.glass(...)` for the frame; slots use the **active resource pack's** slot art:
   draw the plain texture `textures/gui/sprites/container/slot.png` with `g.blit(RenderPipelines.GUI_TEXTURED, id, ...)`
   — loading a texture by file path through `blit` is exactly what your HUD theme textures already do (✅), and it follows the active pack.
   Fallback if the path is missing: `PackTheme.baseRgb()` darkened, with a 1px lighter border.
4. **Items.** `ItemIcon.draw(...)` (✅ already working in your pickup popup) + count text drawn small in the slot corner.
5. **Perf.** Pre-compute the grid layout when the cache entry changes; per frame it is `fill`/`blit`/icon only, max 54 slots.

---

## 9. Local UI sounds (does not touch vanilla sounds)

1. Ship your own CC0/own-made `.ogg` (mono, 44.1 kHz): `assets/rpgcombat/sounds/ui/click.ogg`, `whoosh.ogg`, `hover.ogg`.
2. `assets/rpgcombat/sounds.json`:
```json
{
  "ui.click":  { "sounds": [ "rpgcombat:ui/click" ],  "subtitle": "" },
  "ui.whoosh": { "sounds": [ "rpgcombat:ui/whoosh" ], "subtitle": "" }
}
```
3. Play without registering anything in the global registry (so nothing leaks into vanilla):
```java
// compat/SoundCompat.java  ⚠️ confirm names in the spike
SoundEvent ev = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("rpgcombat", "ui.click"));
Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, volume));
```
Call sites: card hover (pitch 1.2, vol 0.25), click (1.0 / 0.5), deck slide (whoosh, pitch tied to spring velocity, rate-limited to once per 80 ms).
Add a "UI sounds" toggle + volume slider in the QoL category.

---

## 10. Spike checklist (do these BEFORE writing Phase 3/4 — each is ~10 minutes)

Create a throwaway `SpikeScreen` that tries each item and prints `OK/FAIL` on screen; paste me the results.

| # | Question | If it fails |
|---|---|---|
| 1 | `Matrix3x2fStack#rotate(float)` exists (card roll, doll limbs) | use `mul(Matrix3x2f)` with a manual rotation matrix (I already did this in `Spatial`) ✅ |
| 2 | `enableScissor/disableScissor` signature `(int,int,int,int)` | clip manually by clamping particle coords |
| 3 | `InventoryScreen` static entity method found by signature (§7.2) | `DollStage` only |
| 4 | Can a dummy client player be created and its `walkAnimation` driven | keep 3D idle, walking only in doll |
| 5 | `SoundEvent.createVariableRangeEvent` + `SimpleSoundInstance.forUI` | `SimpleSoundInstance` ctor with `Identifier` |
| 6 | `keyPressed` / `mouseScrolled` signatures on `Screen` (for arrows / wheel) | buttons ◀ ▶, ESC only |
| 7 | Custom font via `Style`/font description | vanilla font (default) |
| 8 | Particle spawn: `level.addParticle` with your chosen `ParticleOptions` | use only simple `ParticleTypes` |
| 9 | Screen-close hook for the chest cache | read `containerMenu` on the tick before the screen disappears |

---

## 11. How this maps onto your existing code

- **Keep:** `ModConfig`, `HudAlpha`, `ItemIcon`, `EnchantChips`, `PackTheme`, `MouseInput`, `Spatial` (optional: card tilt can reuse it), GitHub workflow.
- **Replace:** `SettingsScreen` (tabs) → `CardDeckScreen` + `CategoryScreen`. Existing option lists become `Row` pages:
  *Combat* = damage numbers, combo, colors; *HUD* = bar layout, sections, opacity, themes; *QoL* = pickup popup, mining bar, hotbar name, chest HUD, UI sounds.
- **Keep the drag editor** (`HudCustomizerScreen`) as a page action: "Edit HUD layout".
- **Not yet existing features:** Trails, Kill animations, Hit effects, Jumps, Mace & wind, Deaths, Cosmetics (capes/wings/auras) are
  *features*, not UI. The GUI shell only exposes them. Build the shell + previews first, then add each effect as a `FxStyle` + a `Row` page.

## 12. Suggested first commit (Phase 1, ~12 files)

`ui/{Theme,Draw,Anim,UiSounds}` · `menu/{Category,Feature,FeatureRegistry,CardDeckScreen,CategoryScreen}` · `ui/row/{Row,ToggleRow,SliderRow,ChoiceRow,TileGridRow}` · lang keys · `H` opens `CardDeckScreen`.
Ask me for Phase 1 and I will write it as compilable project files using only ✅ APIs, with the old settings screen kept behind a "Classic menu" button until the new one is verified.
