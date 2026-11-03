package dev.rpgcombat.gui;

import dev.combatfx.CombatConfig;
import dev.rpgcombat.config.ModConfig;
import dev.rpgcombat.extras.ExtrasConfig;
import dev.rpgcombat.extras.HotbarName;
import dev.rpgcombat.spatial.Spatial;
import dev.rpgcombat.spatial.SpatialSettings;
import dev.rpghud.BarLayout;
import dev.rpghud.HudAlpha;
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
 * Editor posisi HUD: klik elemen lalu geser (drag & drop), plus panel 2.5D (pitch / yaw / roll / depth).
 *
 * INPUT MOUSE
 *  Status tombol dan posisi kursor dibaca langsung dari GLFW (lihat MouseInput). Alur press -> drag ->
 *  release dijalankan tiap frame. Cara lama (mouseHandler.isLeftPressed) tidak pernah berubah di dalam
 *  layar, itulah sebabnya drag tidak jalan.
 *   - Klik kiri + tahan + geser : memindahkan elemen
 *   - Klik kanan + tahan + geser: memiringkan elemen (kiri/kanan = yaw, atas/bawah = pitch)
 *   - Slider di panel kiri      : pitch, yaw, roll, depth untuk elemen terpilih
 *
 * HIT-TEST 2.5D (screen -> bidang elemen)
 *  Elemen yang miring/dimajumundurkan digambar dengan matriks linear L (Spatial.linear) di sekitar pivot
 *  (tengah kotaknya). Titik kursor dipetakan balik: lokal = L^-1 * (kursor - pivot), lalu diuji terhadap
 *  kotak asli. Ini sama dengan memotong sinar kursor dengan bidang elemen. Drag memindahkan pivot
 *  sebesar delta kursor, jadi elemen mengikuti kursor tepat walau sedang miring.
 *
 * Posisi, skala, opacity dan pengaturan 2.5D disimpan ke rpgcombat.json tiap ~0,12 detik saat drag
 * dan sekali lagi saat tombol dilepas.
 */
public class HudCustomizerScreen extends Screen {
	private interface Elem {
		String key();

		/** Kunci pengaturan 2.5D (lihat Spatial). */
		String id();

		/** true = digambar oleh layar ini (mock); false = digambar oleh HudRenderer.preview. */
		boolean mock();

		int x();

		int y();

		int w();

		int h();

		void setPos(int x, int y);

		float scale();

		void setScale(float s);

		float opacity();

		void setOpacity(float o);

		void reset();

		void preview(GuiGraphicsExtractor g, Minecraft mc);
	}

	private final List<Elem> elems = new ArrayList<>();
	private final List<GuiSlider> sliders = new ArrayList<>();
	private Elem selected;
	private Elem dragging;
	private Elem tilting;

	// state mouse
	private final double[] cur = new double[2];
	private boolean synced;
	private boolean wasLeft;
	private boolean wasRight;
	private boolean inputOk = true;
	private double pressX;
	private double pressY;
	private int startX;
	private int startY;
	private double lastRx;
	private double lastRy;
	private long lastSaveMs;

	// geometri panel 3D
	private int panelX;
	private int panelY;
	private int panelW;
	private int panelH;

	// scratch (tanpa alokasi per frame)
	private final float[] lin = new float[4];
	private final float[] tmp = new float[2];
	private final float[] bounds = new float[4];

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
		sliders.clear();
		buildElems();
		String keep = selected == null ? null : selected.key();
		selected = elems.get(0);
		if (keep != null) {
			for (Elem e : elems) {
				if (e.key().equals(keep)) selected = e;
			}
		}
		dragging = null;
		tilting = null;

		// ---- toolbar ----
		int gap = 4;
		int yA = this.height - 50;
		int yB = this.height - 26;

		int nA = 5;
		int bwA = Math.max(44, Math.min(76, (this.width - 16 - (nA - 1) * gap) / nA));
		int xA = (this.width - (nA * bwA + (nA - 1) * gap)) / 2;
		this.addRenderableWidget(Button.builder(Component.translatable("rpgcombat.edit.scale_minus"), b -> changeScale(-0.05f))
				.bounds(xA, yA, bwA, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("rpgcombat.edit.scale_plus"), b -> changeScale(0.05f))
				.bounds(xA + (bwA + gap), yA, bwA, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("rpgcombat.edit.opacity_minus"), b -> changeOpacity(-0.05f))
				.bounds(xA + 2 * (bwA + gap), yA, bwA, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("rpgcombat.edit.opacity_plus"), b -> changeOpacity(0.05f))
				.bounds(xA + 3 * (bwA + gap), yA, bwA, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("rpgcombat.edit.center"), b -> {
			Elem e = selected;
			setPosClamped(e, (this.width - e.w()) / 2, e.y());
			ModConfig.save();
		}).bounds(xA + 4 * (bwA + gap), yA, bwA, 20).build());

		int nB = 4;
		int bwB = Math.max(44, Math.min(76, (this.width - 16 - (nB - 1) * gap) / nB));
		int xB = (this.width - (nB * bwB + (nB - 1) * gap)) / 2;
		this.addRenderableWidget(Button.builder(Component.translatable("rpgcombat.edit.reset"), b -> {
			selected.reset();
			Spatial.get(selected.id()).reset();
			syncSliders();
			ModConfig.save();
		}).bounds(xB, yB, bwB, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("rpgcombat.edit.reset_all"), b -> {
			HudConfig hc = HudConfig.get();
			hc.x = 8;
			hc.y = 8;
			hc.scale = 1.0f;
			hc.resetSections();
			hc.faceOpacity = 1.0f;
			hc.barsOpacity = 1.0f;
			hc.infoOpacity = 1.0f;
			for (Elem e : elems) {
				e.reset();
				e.setOpacity(1.0f);
			}
			ModConfig.get().spatial.clear();
			syncSliders();
			ModConfig.save();
		}).bounds(xB + (bwB + gap), yB, bwB, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("rpgcombat.edit.settings"), b -> {
			ModConfig.save();
			McCompat.openScreen(new dev.rpgcombat.menu.CardDeckScreen());
		}).bounds(xB + 2 * (bwB + gap), yB, bwB, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("rpgcombat.done"), b -> this.onClose())
				.bounds(xB + 3 * (bwB + gap), yB, bwB, 20).build());

		// ---- panel 2.5D di kiri-tengah ----
		panelW = 118;
		panelH = 16 + 4 * 22 + 22;
		panelX = 6;
		panelY = Math.max(36, this.height / 2 - panelH / 2);
		int sy = panelY + 16;
		addSlider(sy, "rpgcombat.spatial.pitch", -SpatialSettings.MAX_ANGLE, SpatialSettings.MAX_ANGLE,
				() -> Spatial.get(selected.id()).pitch, v -> Spatial.get(selected.id()).pitch = (float) v);
		addSlider(sy + 22, "rpgcombat.spatial.yaw", -SpatialSettings.MAX_ANGLE, SpatialSettings.MAX_ANGLE,
				() -> Spatial.get(selected.id()).yaw, v -> Spatial.get(selected.id()).yaw = (float) v);
		addSlider(sy + 44, "rpgcombat.spatial.roll", -180, 180,
				() -> Spatial.get(selected.id()).roll, v -> Spatial.get(selected.id()).roll = (float) v);
		addSlider(sy + 66, "rpgcombat.spatial.depth", -SpatialSettings.MAX_DEPTH, SpatialSettings.MAX_DEPTH,
				() -> Spatial.get(selected.id()).depth, v -> Spatial.get(selected.id()).depth = (float) v);
		this.addRenderableWidget(Button.builder(Component.translatable("rpgcombat.spatial.reset"), b -> {
			Spatial.get(selected.id()).reset();
			syncSliders();
			ModConfig.save();
		}).bounds(panelX + 4, sy + 88, panelW - 8, 18).build());
	}

	private void addSlider(int y, String key, double min, double max,
			java.util.function.DoubleSupplier get, java.util.function.DoubleConsumer set) {
		GuiSlider s = new GuiSlider(panelX + 4, y, panelW - 8, 20, Component.translatable(key).getString(),
				min, max, "%.0f", get, set, this::maybeSave);
		sliders.add(s);
		this.addRenderableWidget(s);
	}

	private void syncSliders() {
		for (GuiSlider s : sliders) s.sync();
	}

	private void maybeSave() {
		long now = System.currentTimeMillis();
		if (now - lastSaveMs > 120L) {
			lastSaveMs = now;
			ModConfig.save();
		}
	}

	private void changeScale(float delta) {
		Elem e = selected;
		e.setScale(e.scale() + delta);
		setPosClamped(e, e.x(), e.y());
		ModConfig.save();
	}

	private void changeOpacity(float delta) {
		Elem e = selected;
		e.setOpacity(Math.max(0.1f, Math.min(1.0f, e.opacity() + delta)));
		ModConfig.save();
	}

	// =====================================================================
	// Geometri 2.5D
	// =====================================================================
	private float cx(Elem e) {
		return e.x() + e.w() / 2f;
	}

	private float cy(Elem e) {
		return e.y() + e.h() / 2f;
	}

	/** Apakah titik layar (mx, my) jatuh di dalam bidang elemen (setelah dimiringkan)? */
	private boolean hits(Elem e, double mx, double my, float pad) {
		float dx = (float) mx - cx(e);
		float dy = (float) my - cy(e);
		SpatialSettings st = Spatial.get(e.id());
		if (!st.isIdentity()) {
			Spatial.linear(st, lin);
			if (!Spatial.unmap(lin, dx, dy, tmp)) return false;
			dx = tmp[0];
			dy = tmp[1];
		}
		return Math.abs(dx) <= e.w() / 2f + pad && Math.abs(dy) <= e.h() / 2f + pad;
	}

	/** Luas bidang di layar (dipakai memilih elemen terkecil saat bertumpuk). */
	private float area(Elem e) {
		SpatialSettings st = Spatial.get(e.id());
		float a = (float) e.w() * e.h();
		if (st.isIdentity()) return a;
		Spatial.linear(st, lin);
		return a * Math.abs(lin[0] * lin[3] - lin[1] * lin[2]);
	}

	private Elem pick(double mx, double my) {
		Elem best = null;
		float bestArea = Float.MAX_VALUE;
		for (Elem e : elems) {
			if (hits(e, mx, my, 2f)) {
				float a = area(e);
				if (a < bestArea) {
					best = e;
					bestArea = a;
				}
			}
		}
		return best;
	}

	/** Kotak pembatas (min/max layar) dari bidang elemen yang dimiringkan, kalau berada di (x, y). */
	private void quadBounds(Elem e, int x, int y, float[] out) {
		float w = e.w();
		float h = e.h();
		float cxx = x + w / 2f;
		float cyy = y + h / 2f;
		SpatialSettings st = Spatial.get(e.id());
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
		boolean flat = st.isIdentity();
		if (!flat) Spatial.linear(st, lin);
		for (int i = 0; i < 4; i++) {
			float rx = (i & 1) == 0 ? -w / 2f : w / 2f;
			float ry = (i & 2) == 0 ? -h / 2f : h / 2f;
			float px = flat ? rx : lin[0] * rx + lin[1] * ry;
			float py = flat ? ry : lin[2] * rx + lin[3] * ry;
			minX = Math.min(minX, cxx + px);
			maxX = Math.max(maxX, cxx + px);
			minY = Math.min(minY, cyy + py);
			maxY = Math.max(maxY, cyy + py);
		}
		out[0] = minX;
		out[1] = minY;
		out[2] = maxX;
		out[3] = maxY;
	}

	/** Pindahkan elemen ke (x, y) tapi jaga seluruh bidangnya (yang miring) tetap di dalam layar. */
	private void setPosClamped(Elem e, int x, int y) {
		quadBounds(e, x, y, bounds);
		if (bounds[0] < 0) x -= Math.round(bounds[0]);
		if (bounds[1] < 0) y -= Math.round(bounds[1]);
		if (bounds[2] > this.width) x -= Math.round(bounds[2] - this.width);
		if (bounds[3] > this.height) y -= Math.round(bounds[3] - this.height);
		e.setPos(x, y);
	}

	// =====================================================================
	// Mouse: press -> drag -> release (GLFW)
	// =====================================================================
	private boolean inUi(double mx, double my) {
		if (my >= this.height - 56) return true; // toolbar
		return mx >= panelX - 2 && mx <= panelX + panelW + 2 && my >= panelY - 2 && my <= panelY + panelH + 2;
	}

	private void pollMouse(Minecraft mc, int mouseX, int mouseY) {
		long win = MouseInput.window(mc);
		boolean left = false;
		boolean right = false;
		if (win != 0L) {
			inputOk = true;
			MouseInput.cursor(mc, win, cur);
			left = MouseInput.left(win);
			right = MouseInput.right(win);
		} else {
			inputOk = false;
			cur[0] = mouseX;
			cur[1] = mouseY;
		}
		if (!synced) { // tombol yang masih ditahan dari layar sebelumnya bukan klik baru
			synced = true;
			wasLeft = left;
			wasRight = right;
		}

		if (left && !wasLeft) {
			onLeftPress(cur[0], cur[1]);
		} else if (left && dragging != null) {
			onLeftDrag(cur[0], cur[1]);
		}
		if (!left && wasLeft) onLeftRelease();

		if (right && !wasRight) {
			onRightPress(cur[0], cur[1]);
		} else if (right && tilting != null) {
			onRightDrag(cur[0], cur[1]);
		}
		if (!right && wasRight) onRightRelease();

		wasLeft = left;
		wasRight = right;
	}

	private void onLeftPress(double mx, double my) {
		if (inUi(mx, my)) return;
		Elem hit = pick(mx, my);
		if (hit != null) {
			selected = hit;
			dragging = hit;
			pressX = mx;
			pressY = my;
			startX = hit.x();
			startY = hit.y();
			syncSliders();
		}
	}

	private void onLeftDrag(double mx, double my) {
		int nx = startX + (int) Math.round(mx - pressX);
		int ny = startY + (int) Math.round(my - pressY);
		setPosClamped(dragging, nx, ny);
		maybeSave();
	}

	private void onLeftRelease() {
		if (dragging != null) {
			dragging = null;
			ModConfig.save();
		}
	}

	private void onRightPress(double mx, double my) {
		if (inUi(mx, my)) return;
		Elem hit = pick(mx, my);
		tilting = hit != null ? hit : selected;
		selected = tilting;
		lastRx = mx;
		lastRy = my;
		syncSliders();
	}

	private void onRightDrag(double mx, double my) {
		SpatialSettings st = Spatial.get(tilting.id());
		st.yaw = clamp(st.yaw + (float) (mx - lastRx) * 0.4f, SpatialSettings.MAX_ANGLE);
		st.pitch = clamp(st.pitch - (float) (my - lastRy) * 0.4f, SpatialSettings.MAX_ANGLE);
		lastRx = mx;
		lastRy = my;
		syncSliders();
		maybeSave();
	}

	private void onRightRelease() {
		if (tilting != null) {
			tilting = null;
			ModConfig.save();
		}
	}

	private static float clamp(float v, float lim) {
		return v < -lim ? -lim : (v > lim ? lim : v);
	}

	// =====================================================================
	// Render
	// =====================================================================
	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		Minecraft mc = Minecraft.getInstance();
		pollMouse(mc, mouseX, mouseY);

		super.extractRenderState(g, mouseX, mouseY, delta); // latar + tombol + slider

		HudTheme theme = HudTheme.byId(HudConfig.get().theme);
		int accent = theme.text;
		Elem hover = (dragging == null && tilting == null && !inUi(cur[0], cur[1])) ? pick(cur[0], cur[1]) : null;

		// seluruh RPG HUD sekali (tiap bagian sudah memakai opacity + 2.5D-nya sendiri)
		HudRenderer.preview(g);
		for (Elem e : elems) {
			if (!e.mock()) continue;
			SpatialSettings st = Spatial.get(e.id());
			HudAlpha.set(e.opacity() * Spatial.fade(st));
			boolean sp = Spatial.begin(g, st, cx(e), cy(e), e.w(), e.h(), false, e.opacity());
			e.preview(g, mc);
			Spatial.end(g, sp);
			HudAlpha.set(1.0f);
		}

		// kerangka: ikut miring bersama bidang elemen
		for (Elem e : elems) {
			int col;
			if (e == dragging || e == tilting) col = 0xFFFFFFFF;
			else if (e == selected) col = accent;
			else if (e == hover) col = 0xCCFFFFFF;
			else col = 0x55FFFFFF;
			boolean sp = Spatial.begin(g, Spatial.get(e.id()), cx(e), cy(e), e.w(), e.h(), false, 1f);
			g.outline(e.x() - 2, e.y() - 2, e.w() + 4, e.h() + 4, col);
			Spatial.end(g, sp);
		}

		Elem s = selected;
		String name = Component.translatable(s.key()).getString();
		quadBounds(s, s.x(), s.y(), bounds);
		int ly = bounds[1] - 14 >= 2 ? Math.round(bounds[1]) - 14 : Math.round(bounds[3]) + 4;
		g.text(this.font, name, Math.round(bounds[0]), ly, accent, true);

		// panel 2.5D
		g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xC0101820);
		g.outline(panelX, panelY, panelW, panelH, 0x66FFFFFF);
		g.text(this.font, Component.translatable("rpgcombat.edit.spatial").getString(), panelX + 5, panelY + 4, accent, true);

		String title = Component.translatable("rpgcombat.edit.title").getString();
		String hint = Component.translatable("rpgcombat.edit.hint").getString();
		g.text(this.font, title, (this.width - this.font.width(title)) / 2, 8, accent, true);
		g.text(this.font, hint, (this.width - this.font.width(hint)) / 2, 20, 0xFFFFFFFF, true);
		if (!inputOk) {
			String warn = Component.translatable("rpgcombat.edit.no_mouse").getString();
			g.text(this.font, warn, (this.width - this.font.width(warn)) / 2, 32, 0xFFFF6060, true);
		}

		SpatialSettings st = Spatial.get(s.id());
		String info = name + "   X: " + s.x() + "  Y: " + s.y() + "  "
				+ Component.translatable("rpgcombat.edit.scale").getString() + ": "
				+ String.format(Locale.ROOT, "%.2fx", s.scale()) + "  "
				+ Component.translatable("rpgcombat.edit.opacity").getString() + ": "
				+ Math.round(s.opacity() * 100f) + "%"
				+ (st.isIdentity() ? "" : String.format(Locale.ROOT, "   P%.0f Y%.0f R%.0f D%.0f", st.pitch, st.yaw, st.roll, st.depth));
		g.text(this.font, info, (this.width - this.font.width(info)) / 2, this.height - 50 - 13, 0xFFC8D0DC, true);
	}

	@Override
	public void removed() {
		HudRenderer.suppressHud = false;
		dragging = null;
		tilting = null;
		ModConfig.save();
		super.removed();
	}

	// =====================================================================
	// Elemen yang bisa digeser
	// =====================================================================
	private static int secDx(HudConfig c, int sec) { return sec == 0 ? c.faceDx : (sec == 1 ? c.barsDx : c.infoDx); }
	private static int secDy(HudConfig c, int sec) { return sec == 0 ? c.faceDy : (sec == 1 ? c.barsDy : c.infoDy); }
	private static float secScale(HudConfig c, int sec) { return sec == 0 ? c.faceScale : (sec == 1 ? c.barsScale : c.infoScale); }
	private static float secOpacity(HudConfig c, int sec) { return sec == 0 ? c.faceOpacity : (sec == 1 ? c.barsOpacity : c.infoOpacity); }

	private static void setSec(HudConfig c, int sec, int dx, int dy) {
		if (sec == 0) { c.faceDx = dx; c.faceDy = dy; }
		else if (sec == 1) { c.barsDx = dx; c.barsDy = dy; }
		else { c.infoDx = dx; c.infoDy = dy; }
	}

	private static void setSecScale(HudConfig c, int sec, float v) {
		v = Math.max(0.5f, Math.min(3.0f, v));
		if (sec == 0) c.faceScale = v; else if (sec == 1) c.barsScale = v; else c.infoScale = v;
	}

	private static void setSecOpacity(HudConfig c, int sec, float v) {
		v = Math.max(0.1f, Math.min(1.0f, v));
		if (sec == 0) c.faceOpacity = v; else if (sec == 1) c.barsOpacity = v; else c.infoOpacity = v;
	}

	/** Satu bagian RPG HUD (layout bar) sebagai elemen yang bisa digeser sendiri-sendiri. */
	private void addSection(final int sec, final String key, final String id) {
		HudRenderer.width(HudConfig.get()); // pastikan ukuran teks sudah dihitung
		if (BarLayout.sectionRect(HudConfig.get(), sec) == null) return; // bagian disembunyikan
		elems.add(new Elem() {
			private int[] r() { return BarLayout.sectionRect(HudConfig.get(), sec); }
			public String key() { return key; }
			public String id() { return id; }
			public boolean mock() { return false; }
			public int x() { HudConfig c = HudConfig.get(); return c.x + Math.round((r()[0] + secDx(c, sec)) * c.scale); }
			public int y() { HudConfig c = HudConfig.get(); return c.y + Math.round((r()[1] + secDy(c, sec)) * c.scale); }
			public int w() { HudConfig c = HudConfig.get(); return Math.max(1, Math.round(r()[2] * secScale(c, sec) * c.scale)); }
			public int h() { HudConfig c = HudConfig.get(); return Math.max(1, Math.round(r()[3] * secScale(c, sec) * c.scale)); }
			public void setPos(int ax, int ay) {
				HudConfig c = HudConfig.get();
				int dx = Math.round((ax - c.x) / c.scale - r()[0]);
				int dy = Math.round((ay - c.y) / c.scale - r()[1]);
				setSec(c, sec, dx, dy);
			}
			public float scale() { return secScale(HudConfig.get(), sec); }
			public void setScale(float v) { setSecScale(HudConfig.get(), sec, v); }
			public float opacity() { return secOpacity(HudConfig.get(), sec); }
			public void setOpacity(float o) { setSecOpacity(HudConfig.get(), sec, o); }
			public void reset() { HudConfig c = HudConfig.get(); setSec(c, sec, 0, 0); setSecScale(c, sec, 1.0f); }
			public void preview(GuiGraphicsExtractor g, Minecraft m) { /* digambar oleh HudRenderer.preview */ }
		});
	}

	private void buildElems() {
		final Minecraft mc = Minecraft.getInstance();

		// ---- RPG HUD: layout bar = 3 bagian terpisah, layout lingkaran = satu blok ----
		if (HudConfig.get().layoutType == HudConfig.TYPE_BAR) {
			addSection(BarLayout.SEC_FACE, "rpgcombat.edit.face", Spatial.FACE);
			addSection(BarLayout.SEC_BARS, "rpgcombat.edit.bars", Spatial.BARS);
			addSection(BarLayout.SEC_INFO, "rpgcombat.edit.info", Spatial.INFO);
		} else {
			elems.add(new Elem() {
				public String key() { return "rpgcombat.edit.rpg_hud"; }
				public String id() { return Spatial.CIRCLE; }
				public boolean mock() { return false; }
				public int x() { return HudConfig.get().x; }
				public int y() { return HudConfig.get().y; }
				public int w() { return Math.max(1, Math.round(HudRenderer.width(HudConfig.get()) * HudConfig.get().scale)); }
				public int h() { return Math.max(1, Math.round(HudRenderer.height(HudConfig.get()) * HudConfig.get().scale)); }
				public void setPos(int x, int y) { HudConfig.get().x = x; HudConfig.get().y = y; }
				public float scale() { return HudConfig.get().scale; }
				public void setScale(float s) { HudConfig.get().scale = Math.max(0.5f, Math.min(3.0f, s)); }
				public float opacity() { return HudConfig.get().barsOpacity; }
				public void setOpacity(float o) { HudConfig.get().barsOpacity = Math.max(0.1f, Math.min(1.0f, o)); }
				public void reset() { HudConfig c = HudConfig.get(); c.x = 8; c.y = 8; c.scale = 1.0f; }
				public void preview(GuiGraphicsExtractor g, Minecraft m) { /* digambar oleh HudRenderer.preview */ }
			});
		}

		// ---- Combo counter ----
		elems.add(new Elem() {
			private float base() { return 1.8f * CombatConfig.get().comboScale; }
			public String key() { return "rpgcombat.edit.combo"; }
			public String id() { return Spatial.COMBO; }
			public boolean mock() { return true; }
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
			public float opacity() { return CombatConfig.get().comboOpacity; }
			public void setOpacity(float o) { CombatConfig.get().comboOpacity = Math.max(0.1f, Math.min(1.0f, o)); }
			public void reset() { CombatConfig c = CombatConfig.get(); c.comboOffsetX = 34; c.comboOffsetY = -8; c.comboScale = 1.0f; }
			public void preview(GuiGraphicsExtractor g, Minecraft m) {
				float b = base();
				int x = x();
				int y = y();
				float lw = m.font.width("x12") * b;
				g.pose().pushMatrix();
				g.pose().translate(x, y);
				g.pose().scale(b, b);
				HudAlpha.text(g, m.font, "x12", 0, 0, 0xFFFFA032, true);
				g.pose().popMatrix();
				g.pose().pushMatrix();
				g.pose().translate(x + lw + 3f, y + 2f * b);
				g.pose().scale(0.75f, 0.75f);
				HudAlpha.text(g, m.font, "COMBO", 0, 0, 0xFFFFFFFF, true);
				g.pose().popMatrix();
				int by = y + Math.round(9 * b) + 3;
				int bw = Math.max(40, Math.round(lw));
				HudAlpha.fill(g, x, by, x + bw, by + 3, 0xAA202020);
				HudAlpha.fill(g, x, by, x + Math.round(bw * 0.65f), by + 3, 0xFFFFA032);
			}
		});

		// ---- Popup item pickup: [+N Nama][ikon] satu baris, lalu enchant di bawahnya ----
		elems.add(new Elem() {
			public String key() { return "rpgcombat.edit.pickup"; }
			public String id() { return Spatial.PICKUP; }
			public boolean mock() { return true; }
			public int w() { return Math.round(150 * ExtrasConfig.get().pickupScale); }
			public int h() { return Math.round(29 * ExtrasConfig.get().pickupScale); }
			public int x() { return HudCustomizerScreen.this.width - ExtrasConfig.get().pickupMarginRight - w(); }
			public int y() { return HudCustomizerScreen.this.height / 2 + ExtrasConfig.get().pickupOffsetY - 30; }
			public void setPos(int x, int y) {
				ExtrasConfig c = ExtrasConfig.get();
				c.pickupMarginRight = Math.max(0, HudCustomizerScreen.this.width - (x + w()));
				c.pickupOffsetY = y - HudCustomizerScreen.this.height / 2 + 30;
			}
			public float scale() { return ExtrasConfig.get().pickupScale; }
			public void setScale(float s) { ExtrasConfig.get().pickupScale = Math.max(0.6f, Math.min(1.6f, s)); }
			public float opacity() { return ExtrasConfig.get().pickupOpacity; }
			public void setOpacity(float o) { ExtrasConfig.get().pickupOpacity = Math.max(0.1f, Math.min(1.0f, o)); }
			public void reset() { ExtrasConfig c = ExtrasConfig.get(); c.pickupMarginRight = 10; c.pickupOffsetY = 0; c.pickupScale = 1.0f; }
			public void preview(GuiGraphicsExtractor g, Minecraft m) {
				float s = scale();
				g.pose().pushMatrix();
				g.pose().translate(x(), y());
				g.pose().scale(s, s);
				int ty = 4; // teks dipusatkan terhadap ikon setinggi 16 px
				int cnt = m.font.width("+1");
				int nm = m.font.width("Netherite Sword");
				HudAlpha.text(g, m.font, "+1", 0, ty, 0xFF55FF55, true);
				HudAlpha.text(g, m.font, "Netherite Sword", cnt + 4, ty, 0xFF8FEFFF, true);
				int ix = cnt + 4 + nm + 4;
				HudAlpha.fill(g, ix, 0, ix + 16, 16, 0xFF3C4350);
				HudAlpha.fill(g, ix + 1, 1, ix + 15, 15, 0xFF8A93A6);
				HudAlpha.text(g, m.font, "Sharpness", 0, 18, 0xFFCFD8E6, true);
				int bx = m.font.width("Sharpness") + 3;
				HudAlpha.fill(g, bx, 17, bx + m.font.width("V") + 4, 27, 0xFFD04040);
				HudAlpha.text(g, m.font, "V", bx + 2, 18, 0xFFFFFFFF, false);
				g.pose().popMatrix();
			}
		});

		// ---- Bar hancur blok ----
		elems.add(new Elem() {
			public String key() { return "rpgcombat.edit.mining"; }
			public String id() { return Spatial.MINING; }
			public boolean mock() { return true; }
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
			public float opacity() { return ExtrasConfig.get().breakOpacity; }
			public void setOpacity(float o) { ExtrasConfig.get().breakOpacity = Math.max(0.1f, Math.min(1.0f, o)); }
			public void reset() { ExtrasConfig c = ExtrasConfig.get(); c.breakOffsetX = 0; c.breakOffsetY = 14; c.breakScale = 1.0f; }
			public void preview(GuiGraphicsExtractor g, Minecraft m) {
				float s = scale();
				int bw = w();
				int bh = Math.max(2, Math.round(3 * s));
				int x = x();
				int y = y();
				HudAlpha.fill(g, x - 1, y - 1, x + bw + 1, y + bh + 1, 0x99000000);
				HudAlpha.fill(g, x, y, x + bw, y + bh, 0xD94A4A4A);
				HudAlpha.fill(g, x, y, x + Math.round(bw * 0.7f), y + bh, 0xFFFFFFFF);
				g.pose().pushMatrix();
				g.pose().translate(x + bw / 2f, y + bh + 3);
				g.pose().scale(0.8f * s, 0.8f * s);
				HudAlpha.text(g, m.font, "70%", -m.font.width("70%") / 2, 0, 0xFFFFFFFF, true);
				g.pose().popMatrix();
			}
		});

		// ---- Nama + enchant item di hotbar (ditambatkan di tepi bawah) ----
		elems.add(new Elem() {
			public String key() { return "rpgcombat.edit.hotbar"; }
			public String id() { return Spatial.HOTBAR; }
			public boolean mock() { return true; }
			public int w() { return Math.round(110 * ExtrasConfig.get().hotbarNameScale); }
			public int h() { return Math.round(HotbarName.blockHeight(1) * ExtrasConfig.get().hotbarNameScale); }
			public int x() { return HudCustomizerScreen.this.width / 2 + ExtrasConfig.get().hotbarNameOffsetX - w() / 2; }
			public int y() { return bottom() - h(); }
			private int bottom() {
				return HotbarName.defaultY(HudCustomizerScreen.this.height) + HotbarName.NAME_H + ExtrasConfig.get().hotbarNameOffsetY;
			}
			public void setPos(int x, int y) {
				ExtrasConfig c = ExtrasConfig.get();
				c.hotbarNameOffsetX = x + w() / 2 - HudCustomizerScreen.this.width / 2;
				c.hotbarNameOffsetY = y + h() - (HotbarName.defaultY(HudCustomizerScreen.this.height) + HotbarName.NAME_H);
			}
			public float scale() { return ExtrasConfig.get().hotbarNameScale; }
			public void setScale(float s) { ExtrasConfig.get().hotbarNameScale = Math.max(0.6f, Math.min(2.0f, s)); }
			public float opacity() { return ExtrasConfig.get().hotbarNameOpacity; }
			public void setOpacity(float o) { ExtrasConfig.get().hotbarNameOpacity = Math.max(0.1f, Math.min(1.0f, o)); }
			public void reset() { ExtrasConfig c = ExtrasConfig.get(); c.hotbarNameOffsetX = 0; c.hotbarNameOffsetY = 0; c.hotbarNameScale = 1.0f; }
			public void preview(GuiGraphicsExtractor g, Minecraft m) {
				float s = scale();
				String t = "Netherite Sword";
				String e1 = "Sharpness";
				String lv = "V";
				String e2 = "Mending";
				int wLv = m.font.width(lv) + 4;
				int total = m.font.width(e1) + 3 + wLv + 10 + m.font.width(e2);
				int x0 = -total / 2;
				g.pose().pushMatrix();
				g.pose().translate(x() + w() / 2f, y());
				g.pose().scale(s, s);
				HudAlpha.text(g, m.font, t, -m.font.width(t) / 2, 0, 0xFF8FEFFF, true);
				HudAlpha.text(g, m.font, e1, x0, 12, 0xFFCFD8E6, true);
				int bx = x0 + m.font.width(e1) + 3;
				HudAlpha.fill(g, bx, 11, bx + wLv, 21, 0xFFD04040);
				HudAlpha.text(g, m.font, lv, bx + 2, 12, 0xFFFFFFFF, false);
				HudAlpha.text(g, m.font, e2, bx + wLv + 10, 12, 0xFFCFD8E6, true);
				g.pose().popMatrix();
			}
		});
	}
}
