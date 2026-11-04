package dev.rpgcombat.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;

// Busur tebasan sebagai quad custom. Dipanggil dari server/client setiap kali ada hit.
// Render pakai RenderType additive (lightning) supaya terlihat menyala.
public final class SlashArcRenderer {
    private static final List<Slash> ACTIVE = new ArrayList<>();

    public static void init() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> render(ctx.matrixStack(), ctx.consumers(), ctx.camera().getPosition()));
    }

    // origin: titik kena, dir: arah tebasan (sudah dinormalisasi, tangen permukaan), normal: arah keluar permukaan
    public static void spawn(Vec3 origin, Vec3 dir, Vec3 normal, float length, int lifetimeTicks, int color) {
        ACTIVE.add(new Slash(origin, dir.normalize(), normal.normalize(), length, lifetimeTicks, color));
    }

    public static void tick() {
        ACTIVE.removeIf(s -> ++s.age >= s.lifetime);
    }

    private static void render(PoseStack ps, MultiBufferSource buffers, Vec3 cam) {
        if (ACTIVE.isEmpty()) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
        for (Slash s : ACTIVE) s.draw(vc, m);
        ps.popPose();
        if (buffers instanceof MultiBufferSource.BufferSource bs) bs.endBatch(RenderType.lightning());
    }

    private static final class Slash {
        final Vec3 origin, dir, normal;
        final float length;
        final int lifetime, color;
        int age = 0;

        Slash(Vec3 origin, Vec3 dir, Vec3 normal, float length, int lifetime, int color) {
            this.origin = origin; this.dir = dir; this.normal = normal;
            this.length = length; this.lifetime = lifetime; this.color = color;
        }

        void draw(VertexConsumer vc, Matrix4f m) {
            float t = Math.min(1f, age / (lifetime * 0.35f)); // sapuan 35% pertama
            float fade = 1f - Math.max(0f, (age - lifetime * 0.5f) / (lifetime * 0.5f));
            if (fade <= 0f) return;
            Vec3 side = normal.cross(dir).normalize();
            int segs = 16;
            int a = (int) (255 * fade);
            int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
            for (int i = 0; i < segs; i++) {
                float u0 = (float) i / segs, u1 = (float) (i + 1) / segs;
                if (u0 > t) break;
                Vec3 p0 = arc(u0, side), p1 = arc(u1, side);
                float w0 = thickness(u0) * fade, w1 = thickness(u1) * fade;
                Vec3 o0 = p0.add(side.scale(w0)), q0 = p0.add(side.scale(-w0));
                Vec3 o1 = p1.add(side.scale(w1)), q1 = p1.add(side.scale(-w1));
                quad(vc, m, o0, q0, q1, o1, r, g, b, a);
            }
        }

        // Titik pada busur: lengkung ke arah normal
        Vec3 arc(float u, Vec3 side) {
            float ang = (u - 0.5f) * (float) Math.PI * 0.9f;
            float along = (float) Math.sin(ang) * length * 0.5f;
            float bow = (float) (Math.cos(ang) - 1.0) * length * 0.15f;
            return origin.add(dir.scale(along)).add(normal.scale(bow));
        }

        float thickness(float u) {
            return (float) Math.sin(Math.PI * u) * 0.12f + 0.02f;
        }

        private static void quad(VertexConsumer vc, Matrix4f m,
                                 Vec3 a, Vec3 b, Vec3 c, Vec3 d, int r, int g, int bl, int al) {
            vertex(vc, m, a, r, g, bl, al);
            vertex(vc, m, b, r, g, bl, al);
            vertex(vc, m, c, r, g, bl, al);
            vertex(vc, m, d, r, g, bl, al);
        }

        private static void vertex(VertexConsumer vc, Matrix4f m, Vec3 p, int r, int g, int b, int a) {
            vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, a);
        }
    }

    private SlashArcRenderer() {}
}
