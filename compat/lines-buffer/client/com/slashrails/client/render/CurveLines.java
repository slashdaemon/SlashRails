package com.slashrails.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

/** Draws {@link CurveOverlay}'s curves as camera-relative line segments (MC 1.21 - 1.21.10). */
public final class CurveLines {

    private CurveLines() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Vec3 cam) {
        VertexConsumer[] lines = {null};
        PoseStack.Pose pose = poseStack.last();
        CurveOverlay.collect((curve, r, g, b) -> {
            if (lines[0] == null) lines[0] = buffers.getBuffer(RenderType.lines());
            VertexConsumer out = lines[0];
            CurveOverlay.segments(curve, (x0, y0, z0, x1, y1, z1) -> {
                float dx = (float) (x1 - x0), dy = (float) (y1 - y0), dz = (float) (z1 - z0);
                float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len > 1e-6) {
                    dx /= len;
                    dy /= len;
                    dz /= len;
                }
                out.addVertex(pose, (float) (x0 - cam.x), (float) (y0 - cam.y), (float) (z0 - cam.z))
                        .setColor(r, g, b, 1f).setNormal(pose, dx, dy, dz);
                out.addVertex(pose, (float) (x1 - cam.x), (float) (y1 - cam.y), (float) (z1 - cam.z))
                        .setColor(r, g, b, 1f).setNormal(pose, dx, dy, dz);
            });
        });
    }
}
