package com.slashrails.client.render;

import com.slashrails.core.Pt;
import com.slashrails.core.SmoothCurve;
import com.slashrails.run.SmoothRun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Geometry of the curved track, per rail block: the slice of curve that rail owns, laid as a strip of
 * rail-texture quads one block wide at the vanilla rail height. Quads are split at every whole block
 * of arc length (so the texture tiles continuously along the curve, like vanilla rails) and every
 * quarter block (so the strip bends smoothly).
 *
 * <p>Pure geometry — each loader turns {@link Quad}s into its own quad format.
 */
public final class TrackMesh {

    /** Half the track width, in blocks (vanilla rail texture is a full block wide). */
    private static final double HALF_WIDTH = 0.5;
    private static final double MAX_PIECE = 0.25;
    /** Vanilla draws flat rails at 1/16 above the block; nudge ours by a hair to be safe against the floor. */
    private static final double LIFT = 0.001;

    /**
     * One upward-facing quad in block-local coordinates, counter-clockwise seen from above.
     * {@code u} runs across the track (0..1) and {@code v} along it (0..1), both in sprite space.
     */
    public record Quad(float[] x, float[] y, float[] z, float[] u, float[] v) {
    }

    private static final Map<Integer, List<List<Quad>>> CACHE = new ConcurrentHashMap<>();

    private TrackMesh() {
    }

    public static void forget(int runId) {
        CACHE.remove(runId);
    }

    public static void forgetAll() {
        CACHE.clear();
    }

    /** Quads for rail {@code index} of {@code run}, relative to that rail's block origin. */
    public static List<Quad> forRail(SmoothRun run, int index) {
        List<List<Quad>> perRail = CACHE.computeIfAbsent(run.id(), id -> build(run));
        return index < perRail.size() ? perRail.get(index) : List.of();
    }

    private static List<List<Quad>> build(SmoothRun run) {
        SmoothCurve curve = run.curve();
        List<List<Quad>> out = new ArrayList<>(run.size());
        for (int i = 0; i < run.size(); i++) {
            double a = curve.railStart(i), b = curve.railEnd(i);
            List<Quad> quads = new ArrayList<>();
            int bx = run.rail(i).getX(), by = run.rail(i).getY(), bz = run.rail(i).getZ();
            if (b >= a) {
                slice(curve, a, b, bx, by, bz, quads);
            } else { // wraps round the start of a loop
                slice(curve, a, curve.length(), bx, by, bz, quads);
                slice(curve, 0, b, bx, by, bz, quads);
            }
            out.add(Collections.unmodifiableList(quads));
        }
        return out;
    }

    private static void slice(SmoothCurve c, double a, double b, int bx, int by, int bz, List<Quad> out) {
        double s = a;
        while (s < b - 1e-6) {
            double nextWhole = Math.floor(s + 1e-9) + 1.0;
            double e = Math.min(Math.min(b, nextWhole), s + MAX_PIECE);
            out.add(quad(c, s, e, bx, by, bz));
            s = e;
        }
    }

    private static Quad quad(SmoothCurve c, double s0, double s1, int bx, int by, int bz) {
        Pt p0 = c.pointAt(s0), p1 = c.pointAt(s1);
        Pt t0 = c.tangentAt(s0), t1 = c.tangentAt(s1);
        // Left-hand normals in plan view.
        double n0x = t0.z(), n0z = -t0.x();
        double n1x = t1.z(), n1z = -t1.x();
        float v0 = (float) (s0 - Math.floor(s0 + 1e-9));
        float v1 = (float) (v0 + (s1 - s0));
        float y0 = (float) (p0.y() - by + LIFT), y1 = (float) (p1.y() - by + LIFT);

        // Corners: p0 right, p1 right, p1 left, p0 left.
        float[] x = {
            (float) (p0.x() - n0x * HALF_WIDTH - bx), (float) (p1.x() - n1x * HALF_WIDTH - bx),
            (float) (p1.x() + n1x * HALF_WIDTH - bx), (float) (p0.x() + n0x * HALF_WIDTH - bx)
        };
        float[] z = {
            (float) (p0.z() - n0z * HALF_WIDTH - bz), (float) (p1.z() - n1z * HALF_WIDTH - bz),
            (float) (p1.z() + n1z * HALF_WIDTH - bz), (float) (p0.z() + n0z * HALF_WIDTH - bz)
        };
        float[] y = {y0, y1, y1, y0};
        float[] u = {1, 1, 0, 0};
        float[] v = {v0, v1, v1, v0};

        // Ensure counter-clockwise seen from above (+Y normal): (b - a) x (c - a) must point up.
        double cross = (x[1] - x[0]) * (z[2] - z[0]) - (z[1] - z[0]) * (x[2] - x[0]);
        if (cross > 0) { // clockwise seen from above in Minecraft's (x, z) with y up → reverse
            reverse(x);
            reverse(y);
            reverse(z);
            reverse(u);
            reverse(v);
        }
        return new Quad(x, y, z, u, v);
    }

    private static void reverse(float[] a) {
        float t = a[0];
        a[0] = a[3];
        a[3] = t;
        t = a[1];
        a[1] = a[2];
        a[2] = t;
    }
}
