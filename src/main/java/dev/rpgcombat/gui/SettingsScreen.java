package dev.rpgcombat.gui;

import dev.combatfx.CombatConfig;
import dev.rpgcombat.config.ModConfig;
import dev.rpgcombat.extras.ExtrasConfig;
import dev.rpgcombat.theme.PackTheme;
import dev.rpghud.HudConfig;
import dev.rpghud.HudDraw;
import dev.rpghud.HudRenderer;
import dev.rpghud.HudTheme;
import dev.rpghud.McCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Menu pengaturan master (tombol H).
 *
 * Kiri  : sidebar tab (Overview, RPG HUD, HUD Elements, CombatFX, Color Themes)
 * Kanan : panel konten untuk tab yang aktif
 *
 * Widget yang dipakai hanya Button dan AbstractSliderButton (API yang sudah terbukti
 * jalan di 26.2). Toggle switch dan swatch warna digambar di atas Button transparan.
 */
public class SettingsScreen extends Screen {
	private enum Tab {
		OVERVIEW("rpgcombat.tab.overview"),
		HUD("rpgcombat.tab.hud"),
		ELEMENTS("rpgcombat.tab.elements"),
		COMBAT("rpgcombat.tab.combat"),
		EXTRAS("rpgcombat.tab.extras"),
		OPACITY("rpgcombat.tab.opacity"),
		COLORS("rpgcombat.tab.colors");

		final String key;

		Tab(String key) {
			this.key = key;
		}
	}

	// tab terakhir diingat selama game berjalan
	private static Tab currentTab = Tab.OVERVIEW;

	private static final int ROW = 24;
	private static final int GAP = 6;
	private static final int SWITCH_W = 32;
	private static final int SWITCH_H = 14;
	private static final int SIDE_W = 112;

	/** Palet warna untuk angka damage CombatFX. */
	private static final int[] PALETTE = {
			0xFF3B3B, 0xFF8A1F, 0xFFC400, 0xFFE066, 0x55FF55,
			0x55FFFF, 0x6AA8FF, 0xB26BFF, 0xFF6AD5, 0xFFFFFF
	};

	// ---- data gambar yang dikumpulkan saat init() ----
	private record Cell(int x, int y, int w, int h) {}
	private record Label(int x, int y, String text, int color) {}
	private record SwitchRef(int x, int y, BooleanSupplier state) {}
	private record Swatch(int x, int y, int w, int h, int argb) {}

	private final List<Cell> cells = new ArrayList<>();
	private final List<Cell> outlines = new ArrayList<>();
	private final List<Label> labels = new ArrayList<>();
	private final List<SwitchRef> switches = new ArrayList<>();
	private final List<Swatch> swatches = new ArrayList<>();
	private final List<ValueSlider> sliders = new ArrayList<>();
	private Cell activeTab;

	// ---- geometri ----
	private int px, py, pw, ph;       // seluruh panel
	private int cx, cy, cw;           // area konten (kanan)
	private int yBase;                // y baris pertama pada tab aktif

	public SettingsScreen() {
		super(Component.translatable("rpgcombat.title"));
	}

	// =====================================================================
	// Layout
	// =====================================================================
	@Override
	protected void init() {
		cells.clear();
		outlines.clear();
		labels.clear();
		switches.clear();
		swatches.clear();
		sliders.clear();
		activeTab = null;

		pw = Math.min(this.width - 12, 450);
		ph = Math.min(this.height - 12, 254);
		px = (this.width - pw) / 2;
		py = (this.height - ph) / 2;
		cx = px + SIDE_W + 14;
		cy = py + 30;
		cw = pw - SIDE_W - 24;
		yBase = cy;

		// ---- sidebar: tombol tab ----
		Tab[] tabs = Tab.values();
		int step = Math.max(21, Math.min(24, (ph - 78) / tabs.length));
		for (int i = 0; i < tabs.length; i++) {
			final Tab tab = tabs[i];
			int bx = px + 9;
			int by = py + 30 + i * step;
			int bw = SIDE_W - 18;
			this.addRenderableWidget(Button.builder(tr(tab.key), b -> {
				if (currentTab != tab) {
					currentTab = tab;
					this.rebuildWidgets();
				}
			}).bounds(bx, by, bw, 20).build());
			if (tab == currentTab) activeTab = new Cell(bx, by, bw, 20);
		}
		this.addRenderableWidget(Button.builder(tr("rpgcombat.done"), b -> this.onClose())
				.bounds(px + 9, py + ph - 29, SIDE_W - 18, 20).build());

		// ---- konten ----
		switch (currentTab) {
			case OVERVIEW -> buildOverview();
			case HUD -> buildHud();
			case ELEMENTS -> buildElements();
			case COMBAT -> buildCombat();
			case EXTRAS -> buildExtras();
			case OPACITY -> buildOpacity();
			case COLORS -> buildColors();
		}
	}

	private int colW() {
		return (cw - GAP) / 2;
	}

	private int colX(int col) {
		return cx + col * (colW() + GAP);
	}

	private int rowY(int row) {
		return yBase + row * ROW;
	}

	private static Component tr(String key) {
		return Component.translatable(key);
	}

	private int accent() {
		return HudTheme.byId(HudConfig.get().theme).text;
	}

	// =====================================================================
	// Tab builders
	// =====================================================================
	private void buildOverview() {
		toggle(0, 0, "rpgcombat.opt.hud_master",
				() -> HudConfig.get().enabled, v -> HudConfig.get().enabled = v);
		toggle(1, 0, "rpgcombat.opt.combat_master",
				() -> CombatConfig.get().enabled, v -> CombatConfig.get().enabled = v);

		button(0, 1, 1, tr("rpgcombat.btn.edit_mode"), b -> {
			ModConfig.save();
			McCompat.openScreen(new HudCustomizerScreen());
		});
		button(1, 1, 1, tr("rpgcombat.btn.reset_hud_pos"), b -> {
			HudConfig c = HudConfig.get();
			c.x = 8;
			c.y = 8;
			afterChange();
			ModConfig.save();
		});
		button(0, 2, 1, tr("rpgcombat.btn.reset_hud"), b -> {
			HudConfig.reset();
			this.rebuildWidgets();
		});
		button(1, 2, 1, tr("rpgcombat.btn.reset_combat"), b -> {
			CombatConfig.reset();
			this.rebuildWidgets();
		});

		// reset semuanya: klik dua kali
		final boolean[] armed = {false};
		button(0, 3, 2, tr("rpgcombat.btn.reset_all"), b -> {
			if (!armed[0]) {
				armed[0] = true;
				b.setMessage(Component.literal(tr("rpgcombat.btn.confirm").getString())
						.withStyle(ChatFormatting.YELLOW));
			} else {
				ModConfig.resetAll();
				this.rebuildWidgets();
			}
		});

		int iy = rowY(4) + 6;
		labels.add(new Label(cx, iy, tr("rpgcombat.info.keys").getString(), 0xFF9AA5B8));
		labels.add(new Label(cx, iy + 12, tr("rpgcombat.info.config").getString(), 0xFF9AA5B8));
		labels.add(new Label(cx, iy + 24, tr("rpgcombat.info.commands").getString(), 0xFF9AA5B8));
	}

	private void buildHud() {
		toggle(0, 0, "rpgcombat.opt.hide_vanilla",
				() -> HudConfig.get().hideVanilla, v -> HudConfig.get().hideVanilla = v);
		button(1, 0, 1, layoutText(), b -> {
			HudConfig c = HudConfig.get();
			c.layoutType = c.layoutType == 1 ? 0 : 1;
			ModConfig.save();
			b.setMessage(layoutText());
			afterChange();
		});

		slider(0, 1, 1, "rpgcombat.opt.x", 0, () -> maxX(), 1, "%.0f",
				() -> HudConfig.get().x, v -> HudConfig.get().x = (int) Math.round(v));
		slider(1, 1, 1, "rpgcombat.opt.y", 0, () -> maxY(), 1, "%.0f",
				() -> HudConfig.get().y, v -> HudConfig.get().y = (int) Math.round(v));
		slider(0, 2, 1, "rpgcombat.opt.scale", 0.5, () -> 3.0, 0.05, "%.2f",
				() -> HudConfig.get().scale, v -> {
					HudConfig.get().scale = (float) v;
					clampPos();
				});

		button(1, 2, 1, tr("rpgcombat.btn.edit_mode"), b -> {
			ModConfig.save();
			McCompat.openScreen(new HudCustomizerScreen());
		});
		button(0, 3, 1, tr("rpgcombat.btn.center"), b -> {
			HudConfig c = HudConfig.get();
			c.x = Math.max(0, (this.width - hudW()) / 2);
			c.y = Math.max(0, (this.height - hudH()) / 2);
			syncSlidersExcept(null);
			ModConfig.save();
		});
		button(1, 3, 1, tr("rpgcombat.btn.reset_hud_pos"), b -> {
			HudConfig c = HudConfig.get();
			c.x = 8;
			c.y = 8;
			afterChange();
			ModConfig.save();
		});
	}

	private void buildElements() {
		toggle(0, 0, "rpgcombat.opt.face", () -> HudConfig.get().showFace, v -> { HudConfig.get().showFace = v; afterChange(); });
		toggle(1, 0, "rpgcombat.opt.level", () -> HudConfig.get().showLevel, v -> { HudConfig.get().showLevel = v; afterChange(); });
		toggle(0, 1, "rpgcombat.opt.name", () -> HudConfig.get().showName, v -> { HudConfig.get().showName = v; afterChange(); });
		toggle(1, 1, "rpgcombat.opt.health", () -> HudConfig.get().showHealth, v -> { HudConfig.get().showHealth = v; afterChange(); });
		toggle(0, 2, "rpgcombat.opt.hunger", () -> HudConfig.get().showHunger, v -> { HudConfig.get().showHunger = v; afterChange(); });
		toggle(1, 2, "rpgcombat.opt.armor", () -> HudConfig.get().showArmor, v -> { HudConfig.get().showArmor = v; afterChange(); });
		toggle(0, 3, "rpgcombat.opt.coords", () -> HudConfig.get().showCoords, v -> { HudConfig.get().showCoords = v; afterChange(); });
		toggle(1, 3, "rpgcombat.opt.fps", () -> HudConfig.get().showFps, v -> { HudConfig.get().showFps = v; afterChange(); });
	}

	private void buildCombat() {
		toggle(0, 0, "rpgcombat.opt.combat_enable", () -> CombatConfig.get().enabled, v -> CombatConfig.get().enabled = v);
		toggle(1, 0, "rpgcombat.opt.numbers", () -> CombatConfig.get().showDamageNumbers, v -> CombatConfig.get().showDamageNumbers = v);
		toggle(0, 1, "rpgcombat.opt.combo", () -> CombatConfig.get().showCombo, v -> CombatConfig.get().showCombo = v);
		toggle(1, 1, "rpgcombat.opt.combo_bar", () -> CombatConfig.get().showComboBar, v -> CombatConfig.get().showComboBar = v);
		toggle(0, 2, "rpgcombat.opt.outline", () -> CombatConfig.get().numberOutline, v -> CombatConfig.get().numberOutline = v);
		toggle(1, 2, "rpgcombat.opt.pulse", () -> CombatConfig.get().comboPulse, v -> CombatConfig.get().comboPulse = v);

		slider(0, 3, 1, "rpgcombat.opt.num_size", 0.5, () -> 2.0, 0.05, "%.2f",
				() -> CombatConfig.get().numberScale, v -> CombatConfig.get().numberScale = (float) v);
		slider(1, 3, 1, "rpgcombat.opt.combo_size", 0.5, () -> 2.0, 0.05, "%.2f",
				() -> CombatConfig.get().comboScale, v -> CombatConfig.get().comboScale = (float) v);
		slider(0, 4, 1, "rpgcombat.opt.timeout", 500, () -> 5000.0, 100, "%.0f ms",
				() -> CombatConfig.get().comboTimeoutMs, v -> CombatConfig.get().comboTimeoutMs = (int) Math.round(v));
		slider(1, 4, 1, "rpgcombat.opt.heavy", 2, () -> 40.0, 1, "%.0f HP",
				() -> CombatConfig.get().heavyDamage, v -> CombatConfig.get().heavyDamage = (float) v);
		slider(0, 5, 1, "rpgcombat.opt.combo_x", -200, () -> 200.0, 2, "%.0f",
				() -> CombatConfig.get().comboOffsetX, v -> CombatConfig.get().comboOffsetX = (int) Math.round(v));
		slider(1, 5, 1, "rpgcombat.opt.combo_y", -150, () -> 150.0, 2, "%.0f",
				() -> CombatConfig.get().comboOffsetY, v -> CombatConfig.get().comboOffsetY = (int) Math.round(v));

		button(0, 6, 2, tr("rpgcombat.btn.reset_combat"), b -> {
			CombatConfig.reset();
			this.rebuildWidgets();
		});
	}

	private void buildExtras() {
		toggle(0, 0, "rpgcombat.opt.break_enable", () -> ExtrasConfig.get().breakEnabled, v -> ExtrasConfig.get().breakEnabled = v);
		toggle(1, 0, "rpgcombat.opt.pickup_enable", () -> ExtrasConfig.get().pickupEnabled, v -> ExtrasConfig.get().pickupEnabled = v);

		button(0, 1, 1, breakStyleText(), b -> {
			ExtrasConfig e = ExtrasConfig.get();
			e.breakStyle = e.breakStyle == 1 ? 0 : 1;
			ModConfig.save();
			b.setMessage(breakStyleText());
		});
		toggle(1, 1, "rpgcombat.opt.break_percent", () -> ExtrasConfig.get().breakShowPercent, v -> ExtrasConfig.get().breakShowPercent = v);

		toggle(0, 2, "rpgcombat.opt.hotbar_name", () -> ExtrasConfig.get().hotbarNameEnabled, v -> ExtrasConfig.get().hotbarNameEnabled = v);
		slider(1, 2, 1, "rpgcombat.opt.break_size", 0.5, () -> 2.0, 0.05, "%.2f",
				() -> ExtrasConfig.get().breakScale, v -> ExtrasConfig.get().breakScale = (float) v);

		toggle(0, 3, "rpgcombat.opt.pickup_enchants", () -> ExtrasConfig.get().pickupEnchants, v -> ExtrasConfig.get().pickupEnchants = v);
		toggle(1, 3, "rpgcombat.opt.pickup_durability", () -> ExtrasConfig.get().pickupDurability, v -> ExtrasConfig.get().pickupDurability = v);

		slider(0, 4, 1, "rpgcombat.opt.pickup_time", 1000, () -> 8000.0, 100, "%.0f ms",
				() -> ExtrasConfig.get().pickupDurationMs, v -> ExtrasConfig.get().pickupDurationMs = (int) Math.round(v));
		slider(1, 4, 1, "rpgcombat.opt.pickup_max", 1, () -> 10.0, 1, "%.0f",
				() -> ExtrasConfig.get().pickupMaxVisible, v -> ExtrasConfig.get().pickupMaxVisible = (int) Math.round(v));
		slider(0, 5, 1, "rpgcombat.opt.pickup_scale", 0.6, () -> 1.6, 0.05, "%.2f",
				() -> ExtrasConfig.get().pickupScale, v -> ExtrasConfig.get().pickupScale = (float) v);
		slider(1, 5, 1, "rpgcombat.opt.pickup_x", 0, () -> 120.0, 2, "%.0f",
				() -> ExtrasConfig.get().pickupMarginRight, v -> ExtrasConfig.get().pickupMarginRight = (int) Math.round(v));
		slider(0, 6, 1, "rpgcombat.opt.pickup_y", -150, () -> 150.0, 2, "%.0f",
				() -> ExtrasConfig.get().pickupOffsetY, v -> ExtrasConfig.get().pickupOffsetY = (int) Math.round(v));
		toggle(1, 6, "rpgcombat.opt.pickup_merge", () -> ExtrasConfig.get().pickupMergeSame, v -> ExtrasConfig.get().pickupMergeSame = v);
	}

	private static Component breakStyleText() {
		boolean circular = ExtrasConfig.get().breakStyle == 1;
		return Component.translatable("rpgcombat.opt.break_style",
				Component.translatable(circular ? "rpgcombat.style.circular" : "rpgcombat.style.linear"));
	}

	private void buildOpacity() {
		opacity(0, 0, "rpgcombat.op.face", () -> HudConfig.get().faceOpacity, v -> HudConfig.get().faceOpacity = (float) v);
		opacity(1, 0, "rpgcombat.op.bars", () -> HudConfig.get().barsOpacity, v -> HudConfig.get().barsOpacity = (float) v);
		opacity(0, 1, "rpgcombat.op.info", () -> HudConfig.get().infoOpacity, v -> HudConfig.get().infoOpacity = (float) v);
		opacity(1, 1, "rpgcombat.op.combo", () -> CombatConfig.get().comboOpacity, v -> CombatConfig.get().comboOpacity = (float) v);
		opacity(0, 2, "rpgcombat.op.numbers", () -> CombatConfig.get().numberOpacity, v -> CombatConfig.get().numberOpacity = (float) v);
		opacity(1, 2, "rpgcombat.op.mining", () -> ExtrasConfig.get().breakOpacity, v -> ExtrasConfig.get().breakOpacity = (float) v);
		opacity(0, 3, "rpgcombat.op.pickup", () -> ExtrasConfig.get().pickupOpacity, v -> ExtrasConfig.get().pickupOpacity = (float) v);
		opacity(1, 3, "rpgcombat.op.hotbar", () -> ExtrasConfig.get().hotbarNameOpacity, v -> ExtrasConfig.get().hotbarNameOpacity = (float) v);

		button(0, 4, 2, tr("rpgcombat.btn.open_editor"), b -> {
			ModConfig.save();
			McCompat.openScreen(new HudCustomizerScreen());
		});
		labels.add(new Label(cx, rowY(5) + 4, tr("rpgcombat.info.opacity").getString(), 0xFF9AA5B8));
	}

	/** Slider opacity 10% - 100% (nilai disimpan 0.1 - 1.0). */
	private void opacity(int col, int row, String labelKey, DoubleSupplier get, DoubleConsumer set) {
		slider(col, row, 1, labelKey, 10, () -> 100.0, 5, "%.0f%%",
				() -> get.getAsDouble() * 100.0, v -> set.accept(v / 100.0));
	}

	private void buildColors() {
		int acc = accent();

		// --- HUD theme (6 tema) ---
		labels.add(new Label(cx, cy + 1, tr("rpgcombat.section.hud_theme").getString(), acc));
		yBase = cy + 14;
		HudConfig hc = HudConfig.get();
		for (int i = 0; i < HudTheme.ALL.length; i++) {
			final HudTheme th = HudTheme.ALL[i];
			int col = i % 2;
			int row = i / 2;
			int x = colX(col);
			int y = rowY(row);
			int w = colW();
			String name = tr(th.translationKey()).getString();
			this.addRenderableWidget(Button.builder(Component.literal(name), b -> {
				HudConfig.get().theme = th.id;
				ModConfig.save();
				this.rebuildWidgets();
			}).bounds(x, y, w, 20).build());
			swatches.add(new Swatch(x + 6, y + 6, 8, 8, th.hp));
			swatches.add(new Swatch(x + 16, y + 6, 8, 8, th.food));
			swatches.add(new Swatch(x + 26, y + 6, 8, 8, th.armor));
			if (th.id.equals(hc.theme)) outlines.add(new Cell(x, y, w, 20));
		}
		// tema "auto" mengambil gaya dari resource pack aktif; tombol ini membaca ulang pack
		button(1, 3, 1, tr("rpgcombat.btn.refresh_theme"), b -> {
			PackTheme.refresh(Minecraft.getInstance());
			ModConfig.save();
		});
		swatches.add(new Swatch(colX(0) + 38, rowY(3) + 6, 8, 8, 0xFF000000 | PackTheme.baseRgb()));

		// --- warna angka damage CombatFX ---
		int headerY = cy + 14 + 4 * ROW + 4;
		labels.add(new Label(cx, headerY, tr("rpgcombat.section.combat_colors").getString(), acc));
		yBase = headerY + 13;
		colorButton(0, 0, "rpgcombat.color.normal", () -> CombatConfig.get().colorNormal, v -> CombatConfig.get().colorNormal = v);
		colorButton(1, 0, "rpgcombat.color.heavy", () -> CombatConfig.get().colorHeavy, v -> CombatConfig.get().colorHeavy = v);
		colorButton(0, 1, "rpgcombat.color.crit", () -> CombatConfig.get().colorCrit, v -> CombatConfig.get().colorCrit = v);
		button(1, 1, 1, tr("rpgcombat.btn.reset_colors"), b -> {
			CombatConfig.get().resetColors();
			ModConfig.save();
			this.rebuildWidgets();
		});
	}

	// =====================================================================
	// Widget helpers
	// =====================================================================
	private Button button(int col, int row, int span, Component msg, Button.OnPress press) {
		int x = colX(col);
		int y = rowY(row);
		int w = span == 2 ? cw : colW();
		return this.addRenderableWidget(Button.builder(msg, press).bounds(x, y, w, 20).build());
	}

	/** Baris berlabel dengan toggle switch di kanan. */
	private void toggle(int col, int row, String labelKey, BooleanSupplier get, Consumer<Boolean> set) {
		int x = colX(col);
		int y = rowY(row);
		int w = colW();
		cells.add(new Cell(x, y, w, ROW - 4));
		labels.add(new Label(x + 8, y + 6, tr(labelKey).getString(), 0xFFDDE3EE));

		int sx = x + w - SWITCH_W - 6;
		int sy = y + (ROW - 4 - SWITCH_H) / 2;
		switches.add(new SwitchRef(sx, sy, get));
		// Button tak terlihat (ditutup gambar switch) hanya untuk menangkap klik
		this.addRenderableWidget(Button.builder(Component.empty(), b -> {
			set.accept(!get.getAsBoolean());
			ModConfig.save();
		}).bounds(sx, sy, SWITCH_W, SWITCH_H).build());
	}

	private void slider(int col, int row, int span, String labelKey, double min, DoubleSupplier max,
			double step, String fmt, DoubleSupplier get, DoubleConsumer set) {
		int x = colX(col);
		int y = rowY(row);
		int w = span == 2 ? cw : colW();
		ValueSlider s = new ValueSlider(this, x, y, w, 20, tr(labelKey).getString(), min, max, step, fmt, get, set);
		sliders.add(s);
		this.addRenderableWidget(s);
	}

	/** Tombol yang memutar warna dari PALETTE; swatch digambar di sisi kanan. */
	private void colorButton(int col, int row, String labelKey, IntSupplier get, IntConsumer set) {
		int x = colX(col);
		int y = rowY(row);
		int w = colW();
		this.addRenderableWidget(Button.builder(tr(labelKey), b -> {
			int cur = get.getAsInt() & 0xFFFFFF;
			int idx = -1;
			for (int i = 0; i < PALETTE.length; i++) {
				if (PALETTE[i] == cur) {
					idx = i;
					break;
				}
			}
			set.accept(PALETTE[(idx + 1) % PALETTE.length]);
			ModConfig.save();
			this.rebuildWidgets();
		}).bounds(x, y, w, 20).build());
		swatches.add(new Swatch(x + w - 20, y + 5, 12, 10, 0xFF000000 | (get.getAsInt() & 0xFFFFFF)));
	}

	private static Component layoutText() {
		boolean bar = HudConfig.get().layoutType == 1;
		return Component.translatable("custom_rpg_hud.config.layout",
				Component.translatable(bar ? "custom_rpg_hud.layout.bar" : "custom_rpg_hud.layout.circle"));
	}

	// =====================================================================
	// Posisi HUD (sama seperti layar lama)
	// =====================================================================
	private int hudW() {
		return Math.max(1, Math.round((float) HudRenderer.width(HudConfig.get()) * HudConfig.get().scale));
	}

	private int hudH() {
		return Math.max(1, Math.round((float) HudRenderer.height(HudConfig.get()) * HudConfig.get().scale));
	}

	private double maxX() {
		return Math.max(1, this.width - hudW());
	}

	private double maxY() {
		return Math.max(1, this.height - hudH());
	}

	private void clampPos() {
		HudConfig c = HudConfig.get();
		c.x = Math.max(0, Math.min(c.x, Math.max(0, this.width - hudW())));
		c.y = Math.max(0, Math.min(c.y, Math.max(0, this.height - hudH())));
	}

	/** Dipanggil setelah perubahan yang bisa mengubah ukuran/posisi HUD. */
	private void afterChange() {
		clampPos();
		syncSlidersExcept(null);
	}

	private void syncSlidersExcept(ValueSlider except) {
		for (ValueSlider s : sliders) {
			if (s != except) s.sync();
		}
	}

	// =====================================================================
	// Render
	// =====================================================================
	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		HudTheme theme = HudTheme.byId(HudConfig.get().theme);
		int accent = theme.text;

		// ---- 1) latar panel + latar baris (di bawah widget) ----
		int fillSide = theme.auto ? PackTheme.menuFill() : 0xE60A1018;
		int fillMain = theme.auto ? PackTheme.menuFill() : 0xE60D141C;
		g.fill(px, py, px + SIDE_W, py + ph, fillSide);
		g.fill(px + SIDE_W + 4, py, px + pw, py + ph, fillMain);
		for (Cell c : cells) {
			g.fill(c.x(), c.y(), c.x() + c.w(), c.y() + c.h(), 0x66000000);
			g.fill(c.x(), c.y(), c.x() + 2, c.y() + c.h(), accent);
		}

		// ---- 2) widget bawaan (tombol tab, slider, tombol aksi) ----
		super.extractRenderState(g, mouseX, mouseY, delta);

		// ---- 3) bingkai logam bertema (hanya tepi, tidak menutup widget) ----
		HudDraw.panelBorder(g, theme, px, py, SIDE_W, ph);
		HudDraw.panelBorder(g, theme, px + SIDE_W + 4, py, pw - SIDE_W - 4, ph);

		// judul sidebar + judul konten
		String side = tr("rpgcombat.title").getString();
		g.text(this.font, side, px + (SIDE_W - this.font.width(side)) / 2, py + 12, accent, true);
		g.text(this.font, tr(currentTab.key).getString(), cx, py + 12, accent, true);
		g.fill(cx, py + 24, cx + cw, py + 25, (accent & 0xFFFFFF) | 0x66000000);

		// label hijau kecil di sidebar (seperti "Custom graphics" di referensi)
		String prof = tr("rpgcombat.profile").getString();
		g.text(this.font, prof, px + (SIDE_W - this.font.width(prof)) / 2, py + ph - 43, 0xFF55FF55, true);

		// tab aktif
		if (activeTab != null) {
			g.fill(activeTab.x() - 4, activeTab.y(), activeTab.x() - 2, activeTab.y() + activeTab.h(), accent);
			g.outline(activeTab.x() - 1, activeTab.y() - 1, activeTab.w() + 2, activeTab.h() + 2, accent);
		}

		// label
		for (Label l : labels) {
			g.text(this.font, l.text(), l.x(), l.y(), l.color(), true);
		}

		// toggle switch
		for (SwitchRef s : switches) {
			boolean hover = mouseX >= s.x() && mouseX < s.x() + SWITCH_W && mouseY >= s.y() && mouseY < s.y() + SWITCH_H;
			drawSwitch(g, s, hover);
		}

		// swatch warna
		for (Swatch s : swatches) {
			g.fill(s.x() - 1, s.y() - 1, s.x() + s.w() + 1, s.y() + s.h() + 1, 0xFF05080C);
			g.fill(s.x(), s.y(), s.x() + s.w(), s.y() + s.h(), s.argb());
		}

		// pilihan aktif (tema HUD)
		for (Cell c : outlines) {
			g.outline(c.x() - 1, c.y() - 1, c.w() + 2, c.h() + 2, accent);
		}
	}

	private void drawSwitch(GuiGraphicsExtractor g, SwitchRef s, boolean hover) {
		boolean on = s.state().getAsBoolean();
		int x = s.x();
		int y = s.y();
		int w = SWITCH_W;
		int h = SWITCH_H;
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, hover ? 0xFFE0E4EC : 0xFF05080C);
		g.fill(x, y, x + w, y + h, on ? 0xFF2F9E44 : 0xFF9B2F3C);
		int kw = w / 2 - 1;
		int kx = on ? x + w - kw - 1 : x + 1;
		g.fill(kx, y + 1, kx + kw, y + h - 1, on ? 0xFFEAFBEA : 0xFFD9D9E0);
		String mark = on ? "I" : "O";
		int mx = on ? x + 5 : x + w - 5 - this.font.width(mark);
		g.text(this.font, mark, mx, y + 3, 0xFFFFFFFF, false);
	}

	@Override
	public void removed() {
		ModConfig.save();
		super.removed();
	}

	// =====================================================================
	// Slider generik
	// =====================================================================
	private static final class ValueSlider extends AbstractSliderButton {
		private final SettingsScreen owner;
		private final String label;
		private final double min;
		private final double step;
		private final DoubleSupplier maxSup;
		private final DoubleSupplier get;
		private final DoubleConsumer set;
		private final String fmt;

		ValueSlider(SettingsScreen owner, int x, int y, int w, int h, String label, double min,
				DoubleSupplier maxSup, double step, String fmt, DoubleSupplier get, DoubleConsumer set) {
			super(x, y, w, h, Component.empty(), 0.0);
			this.owner = owner;
			this.label = label;
			this.min = min;
			this.maxSup = maxSup;
			this.step = step;
			this.fmt = fmt;
			this.get = get;
			this.set = set;
			this.sync();
		}

		/** Baca ulang nilai dari config (mis. setelah scale mengubah batas posisi). */
		void sync() {
			double range = Math.max(1e-6, maxSup.getAsDouble() - min);
			this.value = Math.max(0.0, Math.min(1.0, (get.getAsDouble() - min) / range));
			this.updateMessage();
		}

		private double current() {
			double max = maxSup.getAsDouble();
			double v = min + this.value * (max - min);
			if (step > 0) v = Math.round(v / step) * step;
			return Math.max(min, Math.min(max, v));
		}

		@Override
		protected void updateMessage() {
			if (label == null) return; // dipanggil dari konstruktor super sebelum field terisi
			this.setMessage(Component.literal(label + ": " + String.format(Locale.ROOT, fmt, current())));
		}

		@Override
		protected void applyValue() {
			set.accept(current());
			owner.syncSlidersExcept(this);
		}
	}
}
