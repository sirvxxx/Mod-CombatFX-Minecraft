package dev.rpgcombat.gui;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/** Slider generik; nilai dibaca ulang lewat sync() saat elemen yang dipilih berganti. */
final class GuiSlider extends AbstractSliderButton {
	private final String label;
	private final double min;
	private final double max;
	private final String fmt;
	private final DoubleSupplier get;
	private final DoubleConsumer set;
	private final Runnable onChange;

	GuiSlider(int x, int y, int w, int h, String label, double min, double max, String fmt,
			DoubleSupplier get, DoubleConsumer set, Runnable onChange) {
		super(x, y, w, h, Component.empty(), 0.0);
		this.label = label;
		this.min = min;
		this.max = max;
		this.fmt = fmt;
		this.get = get;
		this.set = set;
		this.onChange = onChange;
		this.sync();
	}

	void sync() {
		this.value = Math.max(0.0, Math.min(1.0, (get.getAsDouble() - min) / (max - min)));
		this.updateMessage();
	}

	private double current() {
		return Math.round(min + this.value * (max - min));
	}

	@Override
	protected void updateMessage() {
		if (label == null) return; // dipanggil dari konstruktor super sebelum field terisi
		this.setMessage(Component.literal(label + ": " + String.format(Locale.ROOT, fmt, current())));
	}

	@Override
	protected void applyValue() {
		set.accept(current());
		if (onChange != null) onChange.run();
	}
}
