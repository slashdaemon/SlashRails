package com.slashrails.client.render;

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
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * World-space line overlay:
 * <ul>
 *   <li>Tool preview — while holding the Track Smoother and looking at a rail, the curve it would
 *       fit (green) or the run it would revert (red).</li>
 *   <li>Debug — every smoothed run's centre line (yellow), when {@code debugOverlay} is on.</li>
 * </ul>
 * This class decides what to draw; {@code CurveLines} (per Minecraft version) draws it.
 */
public final class CurveOverlay {

    /** Lines are drawn this far above the curve so they sit on top of the track. */
    public static final double LIFT = 0.12;
    /** Spacing of the polyline that approximates a curve, in blocks of arc length. */
    public static final double STEP = 0.25;

    private static BlockPos previewPos;
    private static long previewTick;
    private static SmoothCurve previewCurve;

    private CurveOverlay() {
    }

    /** Receives each curve to draw and its colour. */
    public interface Sink {
        void curve(SmoothCurve curve, float r, float g, float b);
    }

    /** Receives each segment of a curve's polyline, already lifted by {@link #LIFT}. */
    public interface Segments {
        void segment(double x0, double y0, double z0, double x1, double y1, double z1);
    }

    /** Every curve the overlay should show this frame. */
    public static void collect(Sink sink) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        if (Config.debugOverlay && !ClientRuns.isEmpty()) {
            for (SmoothRun run : ClientRuns.all()) {
                sink.curve(run.curve(), 1f, 0.85f, 0.1f);
            }
        }

        if (Config.toolPreview && ModItems.trackSmoother != null
                && (mc.player.getMainHandItem().is(ModItems.trackSmoother) || mc.player.getOffhandItem().is(ModItems.trackSmoother))
                && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            ClientRuns.Slot slot = ClientRuns.at(pos);
            if (slot != null) {
                sink.curve(slot.run().curve(), 1f, 0.25f, 0.2f);
            } else {
                SmoothCurve preview = preview(mc, pos);
                if (preview != null) sink.curve(preview, 0.3f, 1f, 0.45f);
            }
        }
    }

    /** Walks a curve's polyline. */
    public static void segments(SmoothCurve c, Segments out) {
        Pt prev = c.pointAt(0);
        int steps = (int) Math.ceil(c.length() / STEP);
        for (int i = 1; i <= steps; i++) {
            Pt cur = c.pointAt(Math.min(c.length(), i * STEP));
            out.segment(prev.x(), prev.y() + LIFT, prev.z(), cur.x(), cur.y() + LIFT, cur.z());
            prev = cur;
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
}
