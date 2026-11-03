package dev.rpgcombat.ui;

import java.lang.reflect.Method;
import java.util.EnumMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Suara UI khusus mod. TIDAK mendaftarkan apa pun ke registry global dan tidak menyentuh suara vanilla:
 * SoundEvent dibuat lokal dari Identifier "rpgcombat:ui.*" (didefinisikan di assets/rpgcombat/sounds.json)
 * lalu diputar sebagai suara UI.
 *
 * Nama method pemutaran berbeda antar versi, jadi dicari lewat reflection (gagal = senyap, tidak crash).
 */
public final class UiSounds {
	private UiSounds() {}

	public enum Snd {
		CLICK("ui.click", 90L), HOVER("ui.hover", 70L), WHOOSH("ui.whoosh", 160L);

		final String path;
		final long minGapMs;

		Snd(String path, long minGapMs) {
			this.path = path;
			this.minGapMs = minGapMs;
		}
	}

	private static final EnumMap<Snd, Object> EVENTS = new EnumMap<>(Snd.class);
	private static final EnumMap<Snd, Long> LAST = new EnumMap<>(Snd.class);
	private static boolean broken;
	private static Method forUi;

	public static void play(Snd s, float pitch, float volumeScale) {
		UiConfig c = UiConfig.get();
		if (broken || !c.sounds || c.soundVolume <= 0.01f) return;
		long now = System.currentTimeMillis();
		Long last = LAST.get(s);
		if (last != null && now - last < s.minGapMs) return; // cegah spam suara
		LAST.put(s, now);
		try {
			Object event = EVENTS.get(s);
			if (event == null) {
				event = makeEvent(s);
				if (event == null) {
					broken = true;
					return;
				}
				EVENTS.put(s, event);
			}
			if (forUi == null) forUi = findForUi();
			if (forUi == null) {
				broken = true;
				return;
			}
			Object instance = forUi.invoke(null, event, pitch, Math.min(1f, c.soundVolume * volumeScale));
			Object manager = Minecraft.getInstance().getSoundManager();
			for (Method m : manager.getClass().getMethods()) {
				if (m.getName().equals("play") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isInstance(instance)) {
					m.invoke(manager, instance);
					return;
				}
			}
			broken = true;
		} catch (Throwable t) {
			broken = true;
		}
	}

	public static void click() {
		play(Snd.CLICK, 1.0f, 0.8f);
	}

	public static void hover() {
		play(Snd.HOVER, 1.15f, 0.35f);
	}

	public static void whoosh() {
		play(Snd.WHOOSH, 1.0f, 0.55f);
	}

	private static Object makeEvent(Snd s) throws Exception {
		Identifier id = Identifier.fromNamespaceAndPath("rpgcombat", s.path);
		for (Method m : SoundEvent.class.getMethods()) {
			if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 1
					&& m.getParameterTypes()[0] == Identifier.class && SoundEvent.class.isAssignableFrom(m.getReturnType())
					&& m.getName().toLowerCase().contains("create")) {
				return m.invoke(null, id);
			}
		}
		return null;
	}

	private static Method findForUi() throws Exception {
		Class<?> cls = Class.forName("net.minecraft.client.resources.sounds.SimpleSoundInstance");
		for (Method m : cls.getMethods()) {
			Class<?>[] p = m.getParameterTypes();
			if (m.getName().equals("forUI") && p.length == 3 && p[0] == SoundEvent.class && p[1] == float.class && p[2] == float.class) {
				return m;
			}
		}
		return null;
	}
}
