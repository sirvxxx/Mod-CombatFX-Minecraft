package dev.rpghud;

import java.util.Arrays;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class RectBatch {
    private int[] x0;
    private int[] y0;
    private int[] x1;
    private int[] y1;
    private int[] col;
    private int n;
    private int[] prevAct = new int[160];
    private int[] curAct = new int[160];
    private int prevN;
    private int curN;
    private int curRow = Integer.MIN_VALUE;

    RectBatch(int capacity) {
        this.x0 = new int[capacity];
        this.y0 = new int[capacity];
        this.x1 = new int[capacity];
        this.y1 = new int[capacity];
        this.col = new int[capacity];
    }

    int size() {
        return this.n;
    }

    void clear() {
        this.n = 0;
        this.prevN = 0;
        this.curN = 0;
        this.curRow = Integer.MIN_VALUE;
    }

    void add(int ax0, int ay0, int ax1, int ay1, int color) {
        if (ax1 <= ax0 || ay1 <= ay0) {
            return;
        }
        if (this.n == this.x0.length) {
            this.grow();
        }
        this.x0[this.n] = ax0;
        this.y0[this.n] = ay0;
        this.x1[this.n] = ax1;
        this.y1[this.n] = ay1;
        this.col[this.n] = color;
        ++this.n;
    }

    void addRun(int rx0, int rx1, int y, int color) {
        if (rx1 <= rx0) {
            return;
        }
        if (y != this.curRow) {
            if (y == this.curRow + 1) {
                int[] t = this.prevAct;
                this.prevAct = this.curAct;
                this.curAct = t;
                this.prevN = this.curN;
            } else {
                this.prevN = 0;
            }
            this.curN = 0;
            this.curRow = y;
        }
        for (int j = 0; j < this.prevN; ++j) {
            int i = this.prevAct[j];
            if (this.x0[i] != rx0 || this.x1[i] != rx1 || this.col[i] != color || this.y1[i] != y) continue;
            this.y1[i] = y + 1;
            if (this.curN < this.curAct.length) {
                this.curAct[this.curN++] = i;
            }
            return;
        }
        int idx = this.n;
        this.add(rx0, y, rx1, y + 1, color);
        if (this.curN < this.curAct.length) {
            this.curAct[this.curN++] = idx;
        }
    }

    void draw(GuiGraphicsExtractor g, int ox, int oy) {
        for (int i = 0; i < this.n; ++i) {
            HudAlpha.fill(g, ox + this.x0[i], oy + this.y0[i], ox + this.x1[i], oy + this.y1[i], this.col[i]);
        }
    }

    private void grow() {
        int cap = this.x0.length * 2;
        this.x0 = Arrays.copyOf(this.x0, cap);
        this.y0 = Arrays.copyOf(this.y0, cap);
        this.x1 = Arrays.copyOf(this.x1, cap);
        this.y1 = Arrays.copyOf(this.y1, cap);
        this.col = Arrays.copyOf(this.col, cap);
    }
}

