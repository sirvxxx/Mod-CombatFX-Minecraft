package dev.rpgcombat.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Satu adegan preview. render() memperbarui sekaligus menggambar; semua harus tetap di dalam kotak. */
public interface PreviewScene {
	void render(GuiGraphicsExtractor g, Minecraft mc, int x, int y, int w, int h, float dt, float time, float alpha);
}
