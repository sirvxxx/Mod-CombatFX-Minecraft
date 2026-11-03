package dev.rpgcombat.menu;

import dev.rpgcombat.gui.MouseInput;
import dev.rpgcombat.gui.SettingsScreen;
import dev.rpgcombat.preview.DollStage;
import dev.rpgcombat.preview.ParticlePool;
import dev.rpgcombat.ui.Anim;
import dev.rpgcombat.ui.Anim.Spring;
import dev.rpgcombat.ui.Draw;
import dev.rpgcombat.ui.Icon;
import dev.rpgcombat.ui.Theme;
import dev.rpgcombat.ui.UiConfig;
import dev.rpgcombat.ui.UiSave;
import dev.rpgcombat.ui.UiSounds;
import dev.rpgcombat.ui.row.Row;
import dev.rpghud.McCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Menu utama: "Floating Card Deck".
 *
 * DECK      : pemain di tengah, empat kartu kategori melayang melengkung di sekelilingnya.
 * CATEGORY  : kartu yang diklik meluncur menjadi panel navigasi di kiri, area kanan menjadi grid fitur
 *             (dengan search + favorit) atau halaman pengaturan sebuah fitur dengan preview.
 *
 * Satu Spring "open" (0..1) menggerakkan semua transisi, jadi klik "kembali" di tengah animasi tetap mulus.
 *
 * Input: semua dibaca dari GLFW (lihat MouseInput) karena status tombol bawaan Minecraft tidak
 * diperbarui di dalam layar. Satu-satunya widget vanilla adalah EditBox tak terlihat yang dipakai
 * untuk menerima ketikan; teksnya digambar sendiri supaya urutan lapisan tetap benar.
 */
public class CardDeckScreen extends Screen {
	private enum View { DECK, CATEGORY }

	private static final Category[] CATS = Category.values();
	private static final float[][] DECK = {
			{-176f, 14f, -0.10f, 0.92f},
			{-76f, 50f, -0.04f, 1.00f},
			{78f, 44f, 0.05f, 1.00f},
			{178f, 8f, 0.11f, 0.92f}
	};
	private static final int SEARCH_Y = 60;
	private static final int BODY_Y = 88;

	private View view = View.DECK;
	private Category cat = Category.COMBAT;
	private Feature page;

	private final Spring open = new Spring(0f, Theme.MOVE);
	private final Spring pageT = new Spring(0f, 170f);
	private final Spring scroll = new Spring(0f, 210f);
	private float scrollMax;

	private String query = "";
	private boolean favOnly;
	private EditBox search;
	private List<Feature> results = new ArrayList<>();
	private boolean dirtyResults = true;

	private final float[] cardX = new float[CATS.length];
	private final float[] cardY = new float[CATS.length];
	private final float[] cardW = new float[CATS.length];
	private final float[] cardH = new float[CATS.length];
	private final float[] cardRoll = new float[CATS.length];
	private final Spring[] cardHover = new Spring[CATS.length];
	private final Map<String, Spring> tileHover = new HashMap<>();
	private int hoverCard = -1;
	private String hoverTile;

	private final ParticlePool motes = new ParticlePool(64);
	private float time;
	private float dt;
	private long lastNs = System.nanoTime();
	private float moteAcc;

	// input
	private final double[] cur = new double[2];
	private boolean synced;
	private boolean wasLeft;
	private boolean inputOk = true;
	private int noInputFrames;
	private Row activeRow;
	private int activeX;
	private int activeY;
	private int activeW;
	private boolean draggingBar;

	public CardDeckScreen() {
		super(Component.translatable("rpgcombat.menu.title"));
		for (int i = 0; i < cardHover.length; i++) cardHover[i] = new Spring(0f, 240f);
	}

	// =====================================================================
	// Setup
	// =====================================================================
	@Override
	protected void init() {
		search = new EditBox(this.font, 0, 0, 100, 14, Component.empty());
		search.setMaxLength(32);
		search.setValue(query);
		search.setResponder(s -> {
			query = s;
			dirtyResults = true;
			scroll.snap(0);
			if (pageT.target > 0.5f) pageT.target = 0f; // mengetik di halaman fitur kembali ke grid hasil
		});
		this.addWidget(search); // hanya menerima input; digambar manual
	}

	@Override
	public void removed() {
		UiConfig.get().lastCategory = cat.name();
		UiSave.touch();
		UiSave.flush(true);
		super.removed();
	}

	@Override
	public void onClose() {
		if (view == View.CATEGORY) {
			back();
			return;
		}
		super.onClose();
	}

	private void forceClose() {
		super.onClose();
	}

	// =====================================================================
	// State / navigasi
	// =====================================================================
	private void openCategory(Category c) {
		cat = c;
		view = View.CATEGORY;
		open.target = 1f;
		pageT.target = 0f;
		page = null;
		query = "";
		search.setValue("");
		favOnly = false;
		dirtyResults = true;
		scroll.snap(0);
		UiSounds.click();
		UiSounds.whoosh();
	}

	private void switchCategory(Category c) {
		cat = c;
		pageT.target = 0f;
		query = "";
		search.setValue("");
		favOnly = false;
		dirtyResults = true;
		scroll.snap(0);
		UiSounds.click();
	}

	private void openPage(Feature f) {
		page = f;
		pageT.target = 1f;
		scroll.snap(0);
		this.setFocused(null);
		UiSounds.click();
	}

	private void back() {
		if (pageT.target > 0.5f) {
			pageT.target = 0f;
			scroll.snap(0);
			UiSounds.whoosh();
		} else if (view == View.CATEGORY) {
			view = View.DECK;
			open.target = 0f;
			this.setFocused(null);
			UiSounds.whoosh();
		}
	}

	private void toggleFavorite(Feature f) {
		var fav = UiConfig.get().favorites;
		if (!fav.remove(f.id)) fav.add(f.id);
		dirtyResults = true;
		UiSave.touch();
		UiSounds.click();
	}

	// =====================================================================
	// Geometri
	// =====================================================================
	private float u() {
		return Math.max(0.55f, Math.min(this.width / 480f, this.height / 270f));
	}

	private int navX() { return 10; }
	private int navY() { return 10; }
	private int navW() { return 124; }
	private int navH() { return this.height - 20; }
	private int railX() { return navX() + 8; }
	private int railW() { return navW() - 16; }
	private int railY(int i) { return navY() + 50 + i * 34; }
	private int railH() { return 28; }
	private int cx0() { return navX() + navW() + 14; }
	private int cw() { return this.width - cx0() - 14; }

	private static boolean inside(int x, int y, int w, int h, int mx, int my) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private void layoutCards(float t, float mouseX) {
		float u = u();
		float cx = this.width / 2f;
		float cy = this.height * 0.52f;
		for (int i = 0; i < CATS.length; i++) {
			float[] d = DECK[i];
			float bob = (float) Math.sin(time * 0.9f + i * 1.7f) * 2.5f * u * (1f - t);
			float par = -((mouseX - cx) / this.width) * (10f + 4f * i) * u * (1f - t);
			float dx = cx + d[0] * u + par;
			float dy = cy + d[1] * u + bob;
			float dw = 92f * u * d[3];
			float dh = 128f * u * d[3];
			float rx = railX() + railW() / 2f;
			float ry = railY(i) + railH() / 2f;
			cardX[i] = Anim.lerp(t, dx, rx);
			cardY[i] = Anim.lerp(t, dy, ry);
			cardW[i] = Anim.lerp(t, dw, railW());
			cardH[i] = Anim.lerp(t, dh, railH());
			cardRoll[i] = Anim.lerp(t, d[2], 0f);
		}
	}

	private boolean hitCard(int i, double mx, double my) {
		float dx = (float) mx - cardX[i];
		float dy = (float) my - cardY[i];
		float c = (float) Math.cos(cardRoll[i]);
		float s = (float) Math.sin(cardRoll[i]);
		float lx = dx * c + dy * s;
		float ly = -dx * s + dy * c;
		return Math.abs(lx) <= cardW[i] / 2f && Math.abs(ly) <= cardH[i] / 2f;
	}

	// =====================================================================
	// Mouse (GLFW): press -> drag -> release
	// =====================================================================
	private void pollMouse(Minecraft mc, int mouseX, int mouseY) {
		long win = MouseInput.window(mc);
		boolean left = false;
		if (win != 0L) {
			inputOk = true;
			MouseInput.cursor(mc, win, cur);
			left = MouseInput.left(win);
		} else {
			inputOk = false;
			cur[0] = mouseX;
			cur[1] = mouseY;
		}
		if (!synced) {
			synced = true;
			wasLeft = left; // klik yang masih ditahan saat layar dibuka bukan klik baru
		}
		int mx = (int) Math.floor(cur[0]);
		int my = (int) Math.floor(cur[1]);
		if (left && !wasLeft) {
			onPress(mx, my);
		} else if (left) {
			onDrag(mx, my);
		}
		if (!left && wasLeft) onRelease();
		wasLeft = left;

		// pengaman: kalau input tidak tersedia, buka menu klasik supaya pengguna tidak terkunci
		if (!inputOk && ++noInputFrames > 90) {
			McCompat.openScreen(new SettingsScreen());
		}
	}

	private void onPress(int mx, int my) {
		if (view == View.DECK) {
			if (open.value > 0.2f) return;
			int best = -1;
			for (int i = 0; i < CATS.length; i++) {
				if (hitCard(i, mx, my) && (best < 0 || cardHover[i].value >= cardHover[best].value)) best = i;
			}
			if (best >= 0) openCategory(CATS[best]);
			return;
		}
		if (open.value < 0.75f) return;

		if (inside(this.width - 30, 14, 16, 16, mx, my)) { // tutup
			UiSounds.click();
			forceClose();
			return;
		}
		if (inside(cx0() - 2, 12, 20, 20, mx, my)) { // kembali
			UiSounds.click();
			back();
			return;
		}
		for (int j = 0; j < CATS.length; j++) { // rail navigasi
			if (inside(railX(), railY(j), railW(), railH(), mx, my)) {
				if (CATS[j] != cat || pageT.target > 0.5f) switchCategory(CATS[j]);
				return;
			}
		}

		boolean grid = pageT.target < 0.5f;
		if (scrollMax > 0f && inside(this.width - 14, BODY_Y, 12, this.height - BODY_Y - 12, mx, my)) {
			draggingBar = true;
			dragBar(my);
			return;
		}
		if (grid) {
			if (inside(cx0(), SEARCH_Y, cw() - 96, 18, mx, my)) {
				this.setFocused(search);
				return;
			}
			this.setFocused(null);
			if (inside(cx0() + cw() - 88, SEARCH_Y, 88, 18, mx, my)) {
				favOnly = !favOnly;
				dirtyResults = true;
				scroll.snap(0);
				UiSounds.click();
				return;
			}
			pressTile(mx, my);
		} else {
			pressRow(mx, my);
		}
	}

	private void onDrag(int mx, int my) {
		if (draggingBar) {
			dragBar(my);
		} else if (activeRow != null) {
			activeRow.drag(activeX, activeY, activeW, mx, my);
		}
	}

	private void onRelease() {
		draggingBar = false;
		if (activeRow != null) {
			activeRow.release();
			activeRow = null;
			UiSave.touch();
		}
	}

	private void dragBar(int my) {
		int trackY = BODY_Y;
		int trackH = this.height - BODY_Y - 12;
		float k = Anim.clamp01((my - trackY) / (float) trackH);
		scroll.target = k * scrollMax;
		scroll.value = scroll.target;
		scroll.velocity = 0f;
	}

	// ------------------------------------------------------------- grid geometry
	private int cols() {
		return Math.max(1, (cw() + 10) / (150 + 10));
	}

	private int tileW() {
		int c = cols();
		return (cw() - 10 * (c - 1)) / c;
	}

	private int tileX(int k) {
		return cx0() + (k % cols()) * (tileW() + 10);
	}

	private int tileY(int k) {
		return BODY_Y + (k / cols()) * (70 + 10) - Math.round(scroll.value);
	}

	private void pressTile(int mx, int my) {
		if (my < BODY_Y) return;
		for (int k = 0; k < results.size(); k++) {
			Feature f = results.get(k);
			int x = tileX(k), y = tileY(k), w = tileW();
			if (!inside(x, y, w, 70, mx, my)) continue;
			if (inside(x + w - 20, y + 4, 16, 16, mx, my)) { // bintang
				toggleFavorite(f);
			} else if (f.hasToggle() && inside(x + w - 36, y + 70 - 24, 30, 16, mx, my)) { // toggle
				f.setEnabled.accept(!f.enabled.getAsBoolean());
				UiSounds.click();
				UiSave.touch();
			} else {
				openPage(f);
			}
			return;
		}
	}

	private void pressRow(int mx, int my) {
		if (page == null) return;
		int x0 = cx0();
		int panelW = Math.min(cw(), 320);
		int panelH = this.height - BODY_Y - 12;
		if (!inside(x0, BODY_Y, panelW, panelH, mx, my)) return;
		int y = BODY_Y + 6 - Math.round(scroll.value);
		for (Row r : page.rows()) {
			int h = r.height();
			if (my >= y && my < y + h && my >= BODY_Y + 4 && my < BODY_Y + panelH - 4) {
				if (r.press(x0 + 4, y, panelW - 8, mx, my)) {
					activeRow = r;
					activeX = x0 + 4;
					activeY = y;
					activeW = panelW - 8;
				}
				return;
			}
			y += h;
		}
	}

	// bergulir roda mouse. (Tanpa @Override: kalau signature di versi ini berbeda, method ini diabaikan
	// dan scroll tetap bisa lewat drag scrollbar.)
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (view == View.CATEGORY && scrollMax > 0f) {
			scroll.target = Math.max(0f, Math.min(scrollMax, scroll.target - (float) scrollY * 24f));
			return true;
		}
		return false;
	}

	// =====================================================================
	// Render
	// =====================================================================
	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		long now = System.nanoTime();
		dt = Math.max(0f, Math.min(0.1f, (now - lastNs) / 1.0e9f));
		lastNs = now;
		time += dt;
		open.update(dt);
		pageT.update(dt);
		scroll.update(dt);
		if (scroll.target > scrollMax) scroll.target = scrollMax;
		if (pageT.target < 0.5f && pageT.value < 0.02f) page = null;

		Minecraft mc = Minecraft.getInstance();
		pollMouse(mc, mouseX, mouseY);
		int mx = (int) Math.floor(cur[0]);
		int my = (int) Math.floor(cur[1]);

		if (dirtyResults) {
			results = FeatureRegistry.query(query, cat, favOnly);
			dirtyResults = false;
		}

		super.extractRenderState(g, mouseX, mouseY, delta); // latar vanilla (blur)

		float t = Anim.clamp01(open.value);
		layoutCards(t, (float) cur[0]);

		// scrim supaya kartu dan teks terbaca di atas dunia
		g.fill(0, 0, this.width, this.height, Draw.argb(0.34f, 0x05030A));

		// panel navigasi (muncul saat kategori terbuka)
		if (t > 0.02f) drawNav(g, t);

		// partikel ambient + karakter + cincin lantai
		drawStage(g, mc, t);

		// kartu
		drawCards(g, t, mx, my);

		// judul hub
		float deckA = 1f - Anim.clamp01(t * 2.2f);
		if (deckA > 0.02f) {
			Draw.textScaled(g, this.font, "COMBATFX RPG", this.width / 2f - this.font.width("COMBATFX RPG") * 1.6f / 2f, 16, 1.6f, Theme.ACCENT, deckA);
			Draw.textCentered(g, this.font, tr("rpgcombat.menu.subtitle"), this.width / 2, 34, Theme.TEXT_DIM, deckA);
			Draw.textCentered(g, this.font, tr("rpgcombat.menu.tap_hint"), this.width / 2, this.height - 16, Theme.TEXT_DIM, deckA * (0.6f + 0.4f * (float) Math.sin(time * 2f)));
		}

		// konten kategori
		float ca = Anim.clamp01((t - 0.55f) / 0.45f);
		if (ca > 0.02f) {
			drawHeader(g, ca, mx, my);
			drawGrid(g, ca, mx, my);
			drawPage(g, mc, ca, mx, my);
		}

		UiSave.flush(false);
	}

	// ------------------------------------------------------------------ nav + stage
	private void drawNav(GuiGraphicsExtractor g, float t) {
		Draw.shadow(g, navX(), navY(), navW(), navH(), Theme.RADIUS + 2, t);
		Draw.glass(g, navX(), navY(), navW(), navH(), Theme.RADIUS + 2, t, Theme.PANEL, 0x6E6788);
		Draw.text(g, this.font, "COMBATFX RPG", navX() + 10, navY() + 12, Theme.ACCENT, t);
		Draw.text(g, this.font, tr("rpgcombat.menu.settings"), navX() + 10, navY() + 24, Theme.TEXT_DIM, t);
		g.fill(navX() + 10, navY() + 38, navX() + navW() - 10, navY() + 39, Draw.argb(0.2f * t, 0xFFFFFF));
	}

	private void drawStage(GuiGraphicsExtractor g, Minecraft mc, float t) {
		float u = u();
		float h = this.height;
		float deckS = h * 0.0144f;
		float navS = 1.9f;
		float s = Anim.lerp(t, deckS, navS);
		float x = Anim.lerp(t, this.width / 2f, navX() + navW() / 2f);
		float foot = Anim.lerp(t, h * 0.80f, navY() + navH() - 26f);

		// partikel ambient di sekitar karakter (hanya di hub)
		float deckA = 1f - Anim.clamp01(t * 2f);
		if (deckA > 0.05f) {
			moteAcc += dt * 16f;
			ThreadLocalRandom r = ThreadLocalRandom.current();
			while (moteAcc >= 1f) {
				moteAcc -= 1f;
				float px = x + (r.nextFloat() - 0.5f) * 150f * u;
				float py = foot - r.nextFloat() * 120f * u;
				int c = r.nextBoolean() ? Theme.ACCENT : 0xFFFFFF;
				motes.spawn(px, py, (r.nextFloat() - 0.5f) * 6f, -8f - r.nextFloat() * 16f, 2.5f + r.nextFloat() * 2f, 1f + r.nextInt(2), c);
			}
		}
		motes.tick(dt, 0f);
		motes.draw(g, deckA);

		// cincin lantai berputar
		float rx = Anim.lerp(t, 52f * u, 30f);
		float ry = rx * 0.26f;
		int cxp = Math.round(x);
		int cyp = Math.round(foot);
		g.fill(cxp - Math.round(rx), cyp - 1, cxp + Math.round(rx), cyp, Draw.argb(0.0f, 0xFFFFFF));
		Draw.arc(g, cxp, cyp, rx, ry, 1, Draw.argb(0.55f, Theme.ACCENT), time * 40f, 360f);
		Draw.arc(g, cxp, cyp, rx * 0.72f, ry * 0.72f, 1, Draw.argb(0.8f, Theme.GOLD), -time * 70f, 220f);
		Draw.arc(g, cxp, cyp, rx * 1.18f, ry * 1.18f, 1, Draw.argb(0.3f, 0xFFFFFF), time * 20f, 300f);

		DollStage.draw(g, mc, x, foot, s, time, 0f, 0f, 1f);
	}

	// ------------------------------------------------------------------ cards
	private void drawCards(GuiGraphicsExtractor g, float t, int mx, int my) {
		int hov = -1;
		if (view == View.DECK && open.value < 0.2f) {
			for (int i = 0; i < CATS.length; i++) {
				if (hitCard(i, mx, my) && (hov < 0 || cardX[i] != cardX[hov])) hov = i;
			}
		}
		if (hov != hoverCard) {
			if (hov >= 0) UiSounds.hover();
			hoverCard = hov;
		}
		for (int i = 0; i < CATS.length; i++) {
			cardHover[i].target = i == hov ? 1f : 0f;
			cardHover[i].update(dt);
		}
		// urutan: kartu biasa dulu, yang di-hover / terpilih terakhir (di atas)
		for (int pass = 0; pass < 2; pass++) {
			for (int i = 0; i < CATS.length; i++) {
				boolean top = i == hov || (view == View.CATEGORY && CATS[i] == cat);
				if ((pass == 1) != top) continue;
				drawCard(g, i, t, cardHover[i].value, view == View.CATEGORY && CATS[i] == cat);
			}
		}
	}

	private void drawCard(GuiGraphicsExtractor g, int i, float t, float hover, boolean selected) {
		Category c = CATS[i];
		float w = cardW[i], h = cardH[i];
		float u = u();
		int iw = Math.round(w), ih = Math.round(h);
		int x = -iw / 2, y = -ih / 2;
		float lift = hover * 3f * (1f - t);

		g.pose().pushMatrix();
		g.pose().translate(cardX[i], cardY[i] - lift);
		g.pose().rotate(cardRoll[i]);

		Draw.shadow(g, x, y, iw, ih, Theme.RADIUS, 1f - 0.5f * t);
		int tint = selected && t > 0.5f ? Theme.PANEL_HOT : (hover > 0.5f ? Theme.PANEL_HOT : Theme.PANEL);
		int border = selected || hover > 0.5f ? Theme.ACCENT : 0x6E6788;
		Draw.glass(g, x, y, iw, ih, Theme.RADIUS, 1f, tint, border);

		// ikon dalam lingkaran: di dek di atas tengah kartu, di rail di kiri
		int ir = Math.round(Anim.lerp(t, 17f * u, 9f));
		float ix = Anim.lerp(t, 0f, -w / 2f + 16f);
		float iy = Anim.lerp(t, -h * 0.17f, 0f);
		int icx = Math.round(ix), icy = Math.round(iy);
		Draw.roundRect(g, icx - ir, icy - ir, ir * 2, ir * 2, ir, Draw.argb(1f, selected || hover > 0.5f ? Theme.ACCENT_DIM : 0x2A2342));
		Draw.icon(g, c.icon, icx, icy, Math.max(4, ir / 2 + 1), selected || hover > 0.5f ? 0xFFFFFF : Theme.ACCENT, 1f);

		// judul
		String title = tr(c.titleKey);
		float ts = Anim.lerp(t, 1.15f, 1.0f);
		float tx = Anim.lerp(t, -this.font.width(title) * ts / 2f, -w / 2f + 30f);
		float ty = Anim.lerp(t, h * 0.04f, -4f);
		Draw.textScaled(g, this.font, title, tx, ty, ts, 0xFFFFFF, 1f);

		// subjudul + jumlah fitur (hanya di dek)
		float sa = 1f - Anim.clamp01(t * 2.5f);
		if (sa > 0.02f) {
			String sub = fit(tr(c.subKey), iw - 12);
			Draw.textCentered(g, this.font, sub, 0, Math.round(h * 0.04f) + 14, Theme.TEXT_DIM, sa);
			int n = 0;
			for (Feature f : FeatureRegistry.ALL) if (f.cat == c) n++;
			String cnt = Component.translatable("rpgcombat.menu.features_n", n).getString();
			Draw.textCentered(g, this.font, cnt, 0, y + ih - 16, Theme.ACCENT, sa);
		}
		g.pose().popMatrix();
	}

	// ------------------------------------------------------------------ header + search
	private void drawHeader(GuiGraphicsExtractor g, float ca, int mx, int my) {
		int x0 = cx0();
		Feature shown = pageT.value > 0.5f ? page : null;

		boolean hb = inside(x0 - 2, 12, 20, 20, mx, my);
		Draw.roundRect(g, x0, 14, 16, 16, 8, Draw.argb(ca * (hb ? 0.95f : 0.6f), 0x2A2342));
		Draw.text(g, this.font, "<", x0 + 5, 18, hb ? 0xFFFFFF : Theme.ACCENT, ca);

		String label = (shown == null ? tr("rpgcombat.menu.category") : tr(cat.titleKey)).toUpperCase(java.util.Locale.ROOT);
		Draw.textScaled(g, this.font, label, x0 + 24, 12, 0.75f, Theme.ACCENT, ca);
		String title = shown == null ? tr(cat.titleKey) : tr(shown.titleKey);
		Draw.textScaled(g, this.font, title, x0 + 24, 22, 1.5f, 0xFFFFFF, ca);
		String sub = shown == null ? tr(cat.subKey) : tr(shown.subKey);
		Draw.text(g, this.font, fit(sub, cw() - 60), x0 + 24, 42, Theme.TEXT_DIM, ca);

		int cxn = this.width - 30;
		boolean hx = inside(cxn, 14, 16, 16, mx, my);
		Draw.roundRect(g, cxn, 14, 16, 16, 8, Draw.argb(ca * (hx ? 0.95f : 0.6f), 0x2A2342));
		Draw.line(g, cxn + 5, 19, cxn + 10, 24, 1, Draw.argb(ca, hx ? 0xFFFFFF : Theme.ACCENT));
		Draw.line(g, cxn + 10, 19, cxn + 5, 24, 1, Draw.argb(ca, hx ? 0xFFFFFF : Theme.ACCENT));

		// search + favorit (hanya mode grid)
		float sa = ca * (1f - pageT.value);
		if (sa > 0.02f) {
			int sw = cw() - 96;
			Draw.glass(g, x0, SEARCH_Y, sw, 18, 6, sa, 0x120E1E, 0x6E6788);
			Draw.arc(g, x0 + 11, SEARCH_Y + 8, 3.5f, 3.5f, 1, Draw.argb(sa, Theme.TEXT_DIM), 0f, 360f);
			Draw.line(g, x0 + 14, SEARCH_Y + 11, x0 + 17, SEARCH_Y + 14, 1, Draw.argb(sa, Theme.TEXT_DIM));
			String v = search.getValue();
			int maxW = sw - 30;
			if (v.isEmpty()) {
				Draw.text(g, this.font, tr("rpgcombat.menu.search_hint"), x0 + 24, SEARCH_Y + 5, 0x6F6888, sa);
			} else {
				String vis = v;
				while (vis.length() > 1 && this.font.width(vis) > maxW) vis = vis.substring(1);
				Draw.text(g, this.font, vis, x0 + 24, SEARCH_Y + 5, Theme.TEXT, sa);
				if (this.getFocused() == search && ((int) (time * 2f)) % 2 == 0) {
					g.fill(x0 + 24 + this.font.width(vis), SEARCH_Y + 4, x0 + 25 + this.font.width(vis), SEARCH_Y + 14, Draw.argb(sa, Theme.ACCENT));
				}
			}
			if (v.isEmpty() && this.getFocused() == search && ((int) (time * 2f)) % 2 == 0) {
				g.fill(x0 + 24, SEARCH_Y + 4, x0 + 25, SEARCH_Y + 14, Draw.argb(sa, Theme.ACCENT));
			}

			int fx = x0 + cw() - 88;
			boolean hf = inside(fx, SEARCH_Y, 88, 18, mx, my);
			Draw.glass(g, fx, SEARCH_Y, 88, 18, 6, sa, favOnly ? Theme.PANEL_HOT : 0x120E1E, favOnly ? Theme.GOLD : 0x6E6788);
			Draw.icon(g, Icon.STAR, fx + 11, SEARCH_Y + 9, 4, favOnly || hf ? Theme.GOLD : Theme.TEXT_DIM, sa);
			Draw.text(g, this.font, tr("rpgcombat.menu.favorites"), fx + 20, SEARCH_Y + 5, favOnly ? Theme.GOLD : Theme.TEXT, sa);
		}
	}

	// ------------------------------------------------------------------ grid
	private static float edgeFade(float y, float h, float top, float bottom) {
		float c = y + h / 2f;
		float k = Math.max(8f, h * 0.6f);
		return Math.min(Anim.clamp01((c - top) / k), Anim.clamp01((bottom - c) / k));
	}

	private void drawGrid(GuiGraphicsExtractor g, float ca, int mx, int my) {
		float a = ca * (1f - pageT.value);
		if (a < 0.02f) return;
		int bodyH = this.height - BODY_Y - 12;
		int rows = (results.size() + cols() - 1) / cols();
		int totalH = Math.max(0, rows * 80 - 10);
		scrollMax = Math.max(0, totalH - bodyH);

		String hovered = null;
		for (int k = 0; k < results.size(); k++) {
			Feature f = results.get(k);
			int x = tileX(k), y = tileY(k), w = tileW();
			if (y + 70 < BODY_Y - 4 || y > BODY_Y + bodyH) continue;
			float edge = edgeFade(y, 70, BODY_Y, BODY_Y + bodyH);
			float stag = Anim.clamp01(ca * 3.2f - k * 0.22f);
			boolean hot = my >= BODY_Y && inside(x, y, w, 70, mx, my) && pageT.target < 0.5f;
			Spring hs = tileHover.computeIfAbsent(f.id, id -> new Spring(0f, 220f));
			hs.target = hot ? 1f : 0f;
			hs.update(dt);
			if (hot) hovered = f.id;
			drawTile(g, f, x, y + Math.round((1f - stag) * 10f), w, a * edge * stag, hs.value, mx, my);
		}
		if (hovered != null && !hovered.equals(hoverTile)) UiSounds.hover();
		hoverTile = hovered;

		if (results.isEmpty()) {
			Draw.textCentered(g, this.font, tr("rpgcombat.menu.no_results"), cx0() + cw() / 2, BODY_Y + 30, Theme.TEXT_DIM, a);
		}
		drawScrollbar(g, a, totalH, bodyH);
	}

	private void drawTile(GuiGraphicsExtractor g, Feature f, int x, int y, int w, float a, float hover, int mx, int my) {
		if (a < 0.02f) return;
		boolean on = f.hasToggle() && f.enabled.getAsBoolean();
		boolean fav = UiConfig.get().favorites.contains(f.id);
		int yy = y - Math.round(hover * 2f);
		Draw.shadow(g, x, yy, w, 70, Theme.RADIUS, a * 0.7f);
		Draw.glass(g, x, yy, w, 70, Theme.RADIUS, a, on || hover > 0.5f ? Theme.PANEL_HOT : Theme.PANEL,
				on ? Theme.ACCENT : (hover > 0.3f ? Theme.ACCENT_DIM : 0x4B4560));

		int icx = x + 20, icy = yy + 20;
		Draw.roundRect(g, icx - 11, icy - 11, 22, 22, 11, Draw.argb(a, on ? Theme.ACCENT_DIM : 0x2A2342));
		Draw.icon(g, f.icon, icx, icy, 5, on ? 0xFFFFFF : Theme.ACCENT, a);

		Draw.text(g, this.font, fit(tr(f.titleKey), w - 20), x + 10, yy + 70 - 28, 0xFFFFFF, a);
		String status;
		if (f.hasToggle()) {
			status = tr(on ? "rpgcombat.menu.on" : "rpgcombat.menu.off");
		} else {
			status = Component.translatable("rpgcombat.menu.settings_n", f.settingCount()).getString();
		}
		boolean showCat = !query.isEmpty() || favOnly;
		if (showCat) status = tr(f.cat.titleKey) + "  -  " + status;
		Draw.text(g, this.font, fit(status, w - 48), x + 10, yy + 70 - 16, on ? Theme.GOOD : Theme.TEXT_DIM, a);

		// bintang favorit
		boolean hs = inside(x + w - 20, yy + 4, 16, 16, mx, my);
		Draw.icon(g, Icon.STAR, x + w - 12, yy + 12, 4, fav ? Theme.GOLD : (hs ? 0xFFFFFF : 0x6F6888), a);

		// toggle pill
		if (f.hasToggle()) {
			int px = x + w - 36, py = yy + 70 - 24;
			Draw.roundRect(g, px, py, 30, 14, 7, Draw.argb(a, on ? Theme.ACCENT_DIM : 0x3A3350));
			Draw.roundBorder(g, px, py, 30, 14, 7, 1, Draw.argb(0.5f * a, on ? Theme.ACCENT : 0x6E6788));
			int kx = on ? px + 30 - 12 : px + 2;
			Draw.roundRect(g, kx, py + 2, 10, 10, 5, Draw.argb(a, on ? 0xFFFFFF : 0xB8B2CC));
		}
	}

	private void drawScrollbar(GuiGraphicsExtractor g, float a, int totalH, int viewH) {
		if (scrollMax <= 0f) return;
		int tx = this.width - 10;
		int ty = BODY_Y;
		g.fill(tx, ty, tx + 3, ty + viewH, Draw.argb(0.12f * a, 0xFFFFFF));
		int th = Math.max(18, Math.round(viewH * (viewH / (float) Math.max(viewH, totalH))));
		int py = ty + Math.round((viewH - th) * (scroll.value / scrollMax));
		Draw.roundRect(g, tx, py, 3, th, 1, Draw.argb(0.7f * a, Theme.ACCENT));
	}

	// ------------------------------------------------------------------ page + preview
	private void drawPage(GuiGraphicsExtractor g, Minecraft mc, float ca, int mx, int my) {
		float a = ca * pageT.value;
		if (a < 0.02f || page == null) return;
		int x0 = cx0() + Math.round((1f - pageT.value) * 24f);
		int panelW = Math.min(cw(), 320);
		int panelH = this.height - BODY_Y - 12;
		Draw.glass(g, x0, BODY_Y, panelW, panelH, Theme.RADIUS, a, Theme.PANEL, 0x6E6788);

		int y = BODY_Y + 6 - Math.round(scroll.value);
		int total = 0;
		for (Row r : page.rows()) {
			int h = r.height();
			float edge = edgeFade(y, h, BODY_Y + 4, BODY_Y + panelH - 4);
			if (edge > 0.04f) r.render(g, this.font, x0 + 4, y, panelW - 8, a * edge, mx, my);
			y += h;
			total += h;
		}
		int viewH = panelH - 12;
		scrollMax = Math.max(0, total - viewH);
		if (scrollMax > 0f) {
			int tx = x0 + panelW - 6;
			g.fill(tx, BODY_Y + 6, tx + 2, BODY_Y + 6 + viewH, Draw.argb(0.12f * a, 0xFFFFFF));
			int th = Math.max(16, Math.round(viewH * (viewH / (float) total)));
			int py = BODY_Y + 6 + Math.round((viewH - th) * (scroll.value / scrollMax));
			Draw.roundRect(g, tx, py, 2, th, 1, Draw.argb(0.7f * a, Theme.ACCENT));
		}

		// kanvas preview (fondasi): karakter berjalan di tempat. Adegan efek menyusul di fase berikutnya.
		int px = cx0() + panelW + 12 + Math.round((1f - pageT.value) * 24f);
		int pw = cx0() + cw() - px;
		if (pw >= 110) drawPreview(g, mc, px, BODY_Y, pw, panelH, a);
	}

	private void drawPreview(GuiGraphicsExtractor g, Minecraft mc, int x, int y, int w, int h, float a) {
		Draw.glass(g, x, y, w, h, Theme.RADIUS, a, 0x080612, 0x6E6788);
		Draw.text(g, this.font, tr("rpgcombat.menu.preview"), x + 10, y + 8, Theme.GOLD, a);
		Draw.text(g, this.font, fit(tr("rpgcombat.menu.preview_note"), w - 20), x + 10, y + 20, Theme.TEXT_DIM, a);

		float s = Math.max(1.2f, Math.min((w - 24) / (float) (DollStage.WIDTH * 1.6f), (h - 70) / (float) DollStage.HEIGHT));
		s = Math.min(s, 4.2f);
		float cxp = x + w / 2f;
		float foot = y + h - 22;
		int fx = Math.round(cxp), fy = Math.round(foot);
		Draw.arc(g, fx, fy, 26f * s / 2.2f, 6f * s / 2.2f, 1, Draw.argb(0.5f * a, Theme.ACCENT), time * 40f, 360f);
		DollStage.draw(g, mc, cxp, foot, s, time, 0.55f, time * 7.5f, a);
	}

	// ------------------------------------------------------------------ utils
	private static String tr(String key) {
		return Component.translatable(key).getString();
	}

	private String fit(String s, int maxW) {
		if (this.font.width(s) <= maxW) return s;
		String t = s;
		while (t.length() > 1 && this.font.width(t + "..") > maxW) t = t.substring(0, t.length() - 1);
		return t + "..";
	}
}
