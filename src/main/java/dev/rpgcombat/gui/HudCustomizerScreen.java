package dev.rpgcombat.gui;

import dev.combatfx.CombatConfig;
import dev.rpgcombat.config.ModConfig;
import dev.rpgcombat.extras.ExtrasConfig;
import dev.rpgcombat.extras.HotbarName;
import dev.rpghud.HudConfig;
import dev.rpghud.HudRenderer;
import dev.rpghud.HudTheme;
import dev.rpghud.McCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Mode edit posisi HUD: klik elemen mana pun lalu geser (drag & drop).
 *
 * Elemen: RPG HUD, Combo counter, Popup item pickup, Bar hancur blok, Nama item hotbar.
 * Posisi (X, Y) dan skala setiap elemen disimpan otomatis ke rpgcombat.json setiap kali tombol
 * mouse dilepas, dan sekali lagi saat layar ditutup.
 *
 * Input mouse: alur press -> drag -> release dijalankan dari status tombol kiri yang dibaca tiap frame
 * (sama persis dengan event mouseClicked / mouseDragged / mouseReleased, tetapi tidak bergantung
 * pada signature event yang berbeda antar versi Minecraft). Akibatnya:
 *  - release SELALU terdeteksi (bahkan kalau kursor keluar jendela / dilepas di atas tombol),
 *  - klik yang masih ditahan dari layar sebelumnya diabaikan (tidak ada drag "nyangkut" saat layar dibuka),
 *  - drag hanya bisa dimulai dari kejadian press baru di atas elemen.
 */
public class HudCustomizerScreen extends Screen {
	private interface Elem {
		String key();

		int x();

		int y();

		int w();

		int h();

		void setPos(int x, int y);

		float scale();

		void setScale(float s);

		void reset();

		void preview(GuiGraphicsExtractor g, Minecraft mc);
	}

	private final List<Elem> elems = new ArrayList<>();
	private Elem selected;
	private Elem dragging;
	private int grabX;
	private int grabY;
	private boolean wasDown;
	private boolean synced;

	public HudCustomizerScreen() {
		super(Component.translatable("rpgcombat.edit.title"));
	}

	// =====================================================================
	// Setup
	// =====================================================================
	@Override
	protected void init() {
		HudRenderer.suppressHud = true; // HUD asli digantikan preview supaya tidak tergambar dobel
		elems.clear();
		buildElems();
		if (selected == null || !elems.contains(selected)) {
			selected = elems.get(0);
		} else {
			// pertahankan pilihan setelah resize layar
			for (Elem e : elems) {
				if (e.key().equals(selected.key())) selected = e;
			}
		}
		dragging = null;

		int n = 7;
		int gap = 4;
		int bw = Math.max(44, Math.min(70, (this.width - 16 - (n - 1) * gap) / n));
		int total = n * bw + (n - 1) * gap;
		int x = (this.width - total) / 2;
		int y = this.height - 26;

		this.addRenderableWidget(Button.builder(Component.translatable("rpgcombat.edit.scale_minus"), b -> changeScale(-0.05f))
				.bounds(x, y, bw, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("rpgcombat.edit.scale_plus"), b -> changeScale(0.05f))
				.bounds(x + (bw + gap), y, bw, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("rpgcombat.edit.center"), b -> {
			Elem e = selected;
			e.setPos(Math.max(0, (this.width - e.w()) / 2), e.y());
			ModConfig.save();
		}).bounds(x + 2 * (bw + gap), y, bw, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("rpgcombat.edit.reset"), b -> {
			selected.reset();
			ModConfig.save();
		}).bounds(x + 3 * (bw + gap), y, bw, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("rpgcombat.edit.reset_all"), b -> {
			for (Elem e : elems) e.reset();
			ModConfig.save();
		}).bounds(x + 4 * (bw + gap), y, bw, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("rpgcombat.edit.settings"), b -> {
			ModConfig.save();
			McCompat.openScreen(new SettingsScreen());
		}).bounds(x + 5 * (bw + gap), y, bw, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("rpgcombat.done"), b -> this.onClose())
				.bounds(x + 6 * (bw + gap), y, bw, 20).build());
	}

	private void changeScale(float delta) {
		Elem e = selected;
		e.setScale(e.scale() + delta);
		keepInside(e);
		ModConfig.save();
	}

	private void keepInside(Elem e) {
		int nx = Math.max(0, Math.min(e.x(), Math.max(0, this.width - e.w())));
		int ny = Math.max(0, Math.min(e.y(), Math.max(0, this.height - e.h())));
		if (nx != e.x() || ny != e.y()) e.setPos(nx, ny);
	}

	// =====================================================================
	// Mouse: press -> drag -> release
	// =====================================================================
	private void onPress(int mx, int my) {
		if (my >= this.height - 34) return; // area tombol bawah
		Elem hit = hitTest(mx, my);
		if (hit != null) {
			selected = hit;
			dragging = hit;
			grabX = mx - hit.x();
			grabY = my - hit.y();
		}
	}

	private void onDrag(int mx, int my) {
		Elem e = dragging;
		int nx = Math.max(0, Math.min(mx - grabX, Math.max(0, this.width - e.w())));
		int ny = Math.max(0, Math.min(my - grabY, Math.max(0, this.height - e.h())));
		e.setPos(nx, ny);
	}

	private void onRelease() {
		if (dragging != null) {
			dragging = null;
			ModConfig.save(); // simpan otomatis
		}
	}

	/** Elemen di bawah kursor; kalau bertumpuk, yang paling kecil menang (mudah diambil). */
	private Elem hitTest(int mx, int my) {
		Elem best = null;
		long bestArea = Long.MAX_VALUE;
		for (Elem e : elems) {
			if (mx >= e.x() - 2 && mx < e.x() + e.w() + 2 && my >= e.y() - 2 && my < e.y() + e.h() + 2) {
				long area = (long) e.w() * e.h();
				if (area < bestArea) {
					best = e;
					bestArea = area;
				}
			}
		}
		return best;
	}

	// =====================================================================
	// Render
	// =====================================================================
	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		boolean down = Minecraft.getInstance().mouseHandler.isLeftPressed();
		if (!synced) {
			synced = true;
			wasDown = down; // tombol yang masih ditahan dari layar sebelumnya bukan klik baru
		}
		if (down && !wasDown) {
			onPress(mouseX, mouseY);
		} else if (down && dragging != null) {
			onDrag(mouseX, mouseY);
		}
		if (!down && wasDown) {
			onRelease();
		}
		wasDown = down;

		super.extractRenderState(g, mouseX, mouseY, delta); // latar + tombol

		Minecraft mc = Minecraft.getInstance();
		HudTheme theme = HudTheme.byId(HudConfig.get().theme);
		int accent = theme.text;
		Elem hover = dragging == null ? hitTest(mouseX, mouseY) : null;

		for (Elem e : elems) {
			e.preview(g, mc);
		}
		for (Elem e : elems) {
			int col;
			if (e == dragging) col = 0xFFFFFFFF;
			else if (e == selected) col = accent;
			else if (e == hover) col = 0xCCFFFFFF;
			else col = 0x55FFFFFF;
			g.outline(e.x() - 2, e.y() - 2, e.w() + 4, e.h() + 4, col);
		}

		Elem s = selected;
		String name = Component.translatable(s.key()).getString();
		int ly = s.y() - 12 >= 2 ? s.y() - 12 : s.y() + s.h() + 4;
		g.text(this.font, name, s.x(), ly, accent, true);

		String title = Component.translatable("rpgcombat.edit.title").getString();
		String hint = Component.translatable("rpgcombat.edit.hint").getString();
		g.text(this.font, title, (this.width - this.font.width(title)) / 2, 8, accent, true);
		g.text(this.font, hint, (this.width - this.font.width(hint)) / 2, 20, 0xFFFFFFFF, true);

		String info = name + "   X: " + s.x() + "  Y: " + s.y() + "  "
				+ Component.translatable("rpgcombat.edit.scale").getString() + ": "
				+ String.format(Locale.ROOT, "%.2fx", s.scale());
		g.text(this.font, info, (this.width - this.font.width(info)) / 2, this.height - 26 - 13, 0xFFC8D0DC, true);
	}

	@Override
	public void removed() {
		HudRenderer.suppressHud = false;
		dragging = null;
		ModConfig.save();
		super.removed();
	}

	// =====================================================================
	// Elemen yang bisa digeser
	// =====================================================================
	private void buildElems() {
		final Minecraft mc = Minecraft.getInstance();

		// ---- 1) RPG HUD ----
		elems.add(new Elem() {
			public String key() { return "rpgcombat.edit.rpg_hud"; }
			public int x() { return HudConfig.get().x; }
			public int y() { return HudConfig.get().y; }
			public int w() { return Math.max(1, Math.round(HudRenderer.width(HudConfig.get()) * HudConfig.get().scale)); }
			public int h() { return Math.max(1, Math.round(HudRenderer.height(HudConfig.get()) * HudConfig.get().scale)); }
			public void setPos(int x, int y) { HudConfig.get().x = x; HudConfig.get().y = y; }
			public float scale() { return HudConfig.get().scale; }
			public void setScale(float s) { HudConfig.get().scale = Math.max(0.5f, Math.min(3.0f, s)); }
			public void reset() { HudConfig c = HudConfig.get(); c.x = 8; c.y = 8; c.scale = 1.0f; }
			public void preview(GuiGraphicsExtractor g, Minecraft m) { HudRenderer.preview(g); }
		});

		// ---- 2) Combo counter ----
		elems.add(new Elem() {
			private float base() { return 1.8f * CombatConfig.get().comboScale; }
			public String key() { return "rpgcombat.edit.combo"; }
			public int x() { return HudCustomizerScreen.this.width / 2 + CombatConfig.get().comboOffsetX; }
			public int y() { return HudCustomizerScreen.this.height / 2 + CombatConfig.get().comboOffsetY; }
			public int w() { return Math.round(mc.font.width("x12") * base() + 6 + mc.font.width("COMBO") * 0.75f); }
			public int h() { return Math.round(9 * base() + 8); }
			public void setPos(int x, int y) {
				CombatConfig c = CombatConfig.get();
				c.comboOffsetX = x - HudCustomizerScreen.this.width / 2;
				c.comboOffsetY = y - HudCustomizerScreen.this.height / 2;
			}
			public float scale() { return CombatConfig.get().comboScale; }
			public void setScale(float s) { CombatConfig.get().comboScale = Math.max(0.5f, Math.min(2.0f, s)); }
			public void reset() { CombatConfig c = CombatConfig.get(); c.comboOffsetX = 34; c.comboOffsetY = -8; c.comboScale = 1.0f; }
			public void preview(GuiGraphicsExtractor g, Minecraft m) {
				float b = base();
				int x = x();
				int y = y();
				float lw = m.font.width("x12") * b;
				g.pose().pushMatrix();
				g.pose().translate(x, y);
				g.pose().scale(b, b);
				g.text(m.font, "x12", 0, 0, 0xFFFFA032, true);
				g.pose().popMatrix();
				g.pose().pushMatrix();
				g.pose().translate(x + lw + 3f, y + 2f * b);
				g.pose().scale(0.75f, 0.75f);
				g.text(m.font, "COMBO", 0, 0, 0xFFFFFFFF, true);
				g.pose().popMatrix();
				int by = y + Math.round(9 * b) + 3;
				int bw = Math.max(40, Math.round(lw));
				g.fill(x, by, x + bw, by + 3, 0xAA202020);
				g.fill(x, by, x + Math.round(bw * 0.65f), by + 3, 0xFFFFA032);
			}
		});

		// ---- 3) Popup item pickup ----
		elems.add(new Elem() {
			public String key() { return "rpgcombat.edit.pickup"; }
			public int w() { return Math.round(150 * ExtrasConfig.get().pickupScale); }
			public int h() { return Math.round(30 * ExtrasConfig.get().pickupScale); }
			public int x() { return HudCustomizerScreen.this.width - ExtrasConfig.get().pickupMarginRight - w(); }
			public int y() { return HudCustomizerScreen.this.height / 2 + ExtrasConfig.get().pickupOffsetY - 30; }
			public void setPos(int x, int y) {
				ExtrasConfig c = ExtrasConfig.get();
				c.pickupMarginRight = Math.max(0, HudCustomizerScreen.this.width - (x + w()));
				c.pickupOffsetY = y - HudCustomizerScreen.this.height / 2 + 30;
			}
			public float scale() { return ExtrasConfig.get().pickupScale; }
			public void setScale(float s) { ExtrasConfig.get().pickupScale = Math.max(0.6f, Math.min(1.6f, s)); }
			public void reset() { ExtrasConfig c = ExtrasConfig.get(); c.pickupMarginRight = 10; c.pickupOffsetY = 0; c.pickupScale = 1.0f; }
			public void preview(GuiGraphicsExtractor g, Minecraft m) {
				float s = scale();
				g.pose().pushMatrix();
				g.pose().translate(x(), y());
				g.pose().scale(s, s);
				g.fill(0, 0, 16, 16, 0xFF3C4350);
				g.fill(1, 1, 15, 15, 0xFF8A93A6);
				g.text(m.font, "+1", 20, 0, 0xFF55FF55, true);
				g.text(m.font, "Netherite Sword", 20 + m.font.width("+1") + 4, 0, 0xFF8FEFFF, true);
				g.text(m.font, "Sharpness", 20, 11, 0xFFCFD8E6, true);
				int bx = 20 + m.font.width("Sharpness") + 3;
				g.fill(bx, 10, bx + m.font.width("V") + 4, 20, 0xFFD04040);
				g.text(m.font, "V", bx + 2, 11, 0xFFFFFFFF, false);
				g.pose().popMatrix();
			}
		});

		// ---- 4) Bar hancur blok ----
		elems.add(new Elem() {
			public String key() { return "rpgcombat.edit.mining"; }
			public int w() { return Math.max(20, Math.round(44 * ExtrasConfig.get().breakScale)); }
			public int h() { return Math.round(16 * ExtrasConfig.get().breakScale); }
			public int x() { return HudCustomizerScreen.this.width / 2 + ExtrasConfig.get().breakOffsetX - w() / 2; }
			public int y() { return HudCustomizerScreen.this.height / 2 + ExtrasConfig.get().breakOffsetY; }
			public void setPos(int x, int y) {
				ExtrasConfig c = ExtrasConfig.get();
				c.breakOffsetX = x + w() / 2 - HudCustomizerScreen.this.width / 2;
				c.breakOffsetY = y - HudCustomizerScreen.this.height / 2;
			}
			public float scale() { return ExtrasConfig.get().breakScale; }
			public void setScale(float s) { ExtrasConfig.get().breakScale = Math.max(0.5f, Math.min(2.0f, s)); }
			public void reset() { ExtrasConfig c = ExtrasConfig.get(); c.breakOffsetX = 0; c.breakOffsetY = 14; c.breakScale = 1.0f; }
			public void preview(GuiGraphicsExtractor g, Minecraft m) {
				float s = scale();
				int bw = w();
				int bh = Math.max(2, Math.round(3 * s));
				int x = x();
				int y = y();
				g.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, 0x99000000);
				g.fill(x, y, x + bw, y + bh, 0xD94A4A4A);
				g.fill(x, y, x + Math.round(bw * 0.7f), y + bh, 0xFFFFFFFF);
				g.pose().pushMatrix();
				g.pose().translate(x + bw / 2f, y + bh + 3);
				g.pose().scale(0.8f * s, 0.8f * s);
				g.text(m.font, "70%", -m.font.width("70%") / 2, 0, 0xFFFFFFFF, true);
				g.pose().popMatrix();
			}
		});

		// ---- 5) Nama item di hotbar ----
		elems.add(new Elem() {
			public String key() { return "rpgcombat.edit.hotbar"; }
			public int w() { return Math.round(96 * ExtrasConfig.get().hotbarNameScale); }
			public int h() { return Math.round(11 * ExtrasConfig.get().hotbarNameScale); }
			public int x() { return HudCustomizerScreen.this.width / 2 + ExtrasConfig.get().hotbarNameOffsetX - w() / 2; }
			public int y() { return HotbarName.defaultY(HudCustomizerScreen.this.height) + ExtrasConfig.get().hotbarNameOffsetY; }
			public void setPos(int x, int y) {
				ExtrasConfig c = ExtrasConfig.get();
				c.hotbarNameOffsetX = x + w() / 2 - HudCustomizerScreen.this.width / 2;
				c.hotbarNameOffsetY = y - HotbarName.defaultY(HudCustomizerScreen.this.height);
			}
			public float scale() { return ExtrasConfig.get().hotbarNameScale; }
			public void setScale(float s) { ExtrasConfig.get().hotbarNameScale = Math.max(0.6f, Math.min(2.0f, s)); }
			public void reset() { ExtrasConfig c = ExtrasConfig.get(); c.hotbarNameOffsetX = 0; c.hotbarNameOffsetY = 0; c.hotbarNameScale = 1.0f; }
			public void preview(GuiGraphicsExtractor g, Minecraft m) {
				float s = scale();
				String t = "Netherite Sword";
				g.pose().pushMatrix();
				g.pose().translate(x() + w() / 2f, y());
				g.pose().scale(s, s);
				g.text(m.font, t, -m.font.width(t) / 2, 0, 0xFF8FEFFF, true);
				g.pose().popMatrix();
			}
		});
	}
}
