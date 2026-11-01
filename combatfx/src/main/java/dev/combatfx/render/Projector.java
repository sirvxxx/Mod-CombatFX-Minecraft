package dev.combatfx.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Proyeksi posisi dunia (x,y,z) ke koordinat layar GUI.
 * Dihitung sendiri dari posisi mata + yaw/pitch player + FOV, jadi tidak
 * bergantung pada internal pipeline render dunia yang sering berubah di 26.x.
 * Nol alokasi per titik: dipakai ulang tiap frame.
 */
public final class Projector {
	private double ex, ey, ez;
	private double fx, fy, fz; // forward
	private double rx, rz;     // right (ry selalu 0)
	private double ux, uy, uz; // up
	private double tanHalfFov, aspect;
	private float guiW, guiH;

	// hasil project()
	public float sx, sy, depth;

	public void begin(Minecraft mc, LocalPlayer p, float partialTick) {
		Vec3 eye = p.getEyePosition(partialTick);
		ex = eye.x;
		ey = eye.y;
		ez = eye.z;

		double yaw = Math.toRadians(p.getViewYRot(partialTick));
		double pitch = Math.toRadians(p.getViewXRot(partialTick));
		double cp = Math.cos(pitch), sp = Math.sin(pitch);
		double cy = Math.cos(yaw), sy_ = Math.sin(yaw);

		fx = -sy_ * cp;
		fy = -sp;
		fz = cy * cp;

		rx = -cy;
		rz = -sy_;

		// up = right x forward  (ry = 0)
		ux = -rz * fy;
		uy = rz * fx - rx * fz;
		uz = rx * fy;

		var window = mc.getWindow();
		tanHalfFov = Math.tan(Math.toRadians(mc.options.fov().get()) * 0.5);
		aspect = (double) window.getWidth() / (double) window.getHeight();
		guiW = window.getGuiScaledWidth();
		guiH = window.getGuiScaledHeight();
	}

	/** @return false kalau titik ada di belakang kamera / jauh di luar layar. */
	public boolean project(double x, double y, double z) {
		double dx = x - ex, dy = y - ey, dz = z - ez;

		double zc = dx * fx + dy * fy + dz * fz;
		if (zc < 0.1) return false;

		double xc = dx * rx + dz * rz;
		double yc = dx * ux + dy * uy + dz * uz;

		double ndcX = xc / (zc * tanHalfFov * aspect);
		double ndcY = yc / (zc * tanHalfFov);
		if (Math.abs(ndcX) > 1.4 || Math.abs(ndcY) > 1.4) return false;

		sx = (float) ((ndcX * 0.5 + 0.5) * guiW);
		sy = (float) ((0.5 - ndcY * 0.5) * guiH);
		depth = (float) zc;
		return true;
	}
}
