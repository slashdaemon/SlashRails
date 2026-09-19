package com.slashrails.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.slashrails.Config;
import com.slashrails.client.ClientRuns;
import com.slashrails.core.CurveFitter;
import com.slashrails.core.Pt;
import com.slashrails.core.SmoothCurve;
import com.slashrails.item.ModItems;
import com.slashrails.run.Clearance;
import com.slashrails.run.RunDetector;
import com.slashrails.run.SmoothRun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * World-space line overlay, drawn after translucent terrain:
 * <ul>
 *   <li>Tool preview — while holding the Track Smoother and looking at a rail, the curve it would
 *       fit (green) or the run it would revert (red).</li>
 *   <li>Debug — every smoothed run's centre line (yellow), when {@code debugOverlay} is on.</li>
 * </ul>
 */
public final class CurveOverlay {

    private static BlockPos previewPos;
    private static long previewTick;
    private static SmoothCurve previewCurve;

    private CurveOverlay() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Vec3 camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        VertexConsumer lines = null;

        if (Config.debugOverlay && !ClientRuns.isEmpty()) {
            lines = buffers.getBuffer(RenderType.lines());
            for (SmoothRun run : ClientRuns.all()) {
                draw(lines, poseStack, camera, run.curve(), 1f, 0.85f, 0.1f);
            }
        }

        if (Config.toolPreview && ModItems.trackSmoother != null
                && (mc.player.getMainHandItem().is(ModItems.trackSmoother) || mc.player.getOffhandItem().is(ModItems.trackSmoother))
                && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            ClientRuns.Slot slot = ClientRuns.at(pos);
            if (lines == null) lines = buffers.getBuffer(RenderType.lines());
            if (slot != null) {
                draw(lines, poseStack, camera, slot.run().curve(), 1f, 0.25f, 0.2f);
            } else {
                SmoothCurve preview = preview(mc, pos);
                if (preview != null) draw(lines, poseStack, camera, preview, 0.3f, 1f, 0.45f);
            }
        }
    }

    /** Candidate curve at {@code pos}, recomputed when the target changes or every half second. */
    private static SmoothCurve preview(Minecraft mc, BlockPos pos) {
        long now = mc.level.getGameTime();
        if (pos.equals(previewPos) && now - previewTick < 10) return previewCurve;
        previewPos = pos.immutable();
        previewTick = now;
        previewCurve = null;
        RunDetector.Result r = RunDetector.detect(mc.level, pos, Config.maxRunLength, p -> ClientRuns.at(p) != null);
        if (r.ok()) {
            try {
                previewCurve = CurveFitter.fit(r.nodes(), r.closed(), Clearance.compute(mc.level, r.nodes(), r.closed()));
            } catch (RuntimeException ignored) {
                previewCurve = null;
            }
        }
        return previewCurve;
    }

    private static void draw(VertexConsumer lines, PoseStack poseStack, Vec3 cam, SmoothCurve c,
                             float r, float g, float b) {
        PoseStack.Pose pose = poseStack.last();
        double step = 0.25;
        Pt prev = c.pointAt(0);
        int steps = (int) Math.ceil(c.length() / step);
        for (int i = 1; i <= steps; i++) {
            Pt cur = c.pointAt(Math.min(c.length(), i * step));
            float dx = (float) (cur.x() - prev.x()), dy = (float) (cur.y() - prev.y()), dz = (float) (cur.z() - prev.z());
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > 1e-6) {
                dx /= len;
                dy /= len;
                dz /= len;
            }
            float lift = 0.12f;
            lines.addVertex(pose, (float) (prev.x() - cam.x), (float) (prev.y() + lift - cam.y), (float) (prev.z() - cam.z))
                    .setColor(r, g, b, 1f).setNormal(pose, dx, dy, dz);
            lines.addVertex(pose, (float) (cur.x() - cam.x), (float) (cur.y() + lift - cam.y), (float) (cur.z() - cam.z))
                    .setColor(r, g, b, 1f).setNormal(pose, dx, dy, dz);
            prev = cur;
        }
    }
}
