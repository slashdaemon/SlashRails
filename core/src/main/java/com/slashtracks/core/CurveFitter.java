package com.slashtracks.core;

import java.util.List;

/**
 * Fits a smooth curve over an ordered run of flat rails.
 *
 * <p>The rail centres of a gentle staircase zigzag around the line the builder meant. A Whittaker
 * smoother (minimise curvature, i.e. squared second differences, while staying near the centres) turns
 * the zigzag into that line or arc. Points that end up further than {@link #TOLERANCE} from their rail
 * are re-weighted and re-solved, then hard-clamped, so the curve never leaves the rails it replaces.
 *
 * <p>Open runs are pinned at the outer edge midpoint of each end rail — exactly where the next vanilla
 * rail's line begins — and two fixed ghost points beyond each end force the curve to leave along the
 * vanilla rail's axis. Closed loops are solved cyclically with nothing pinned.
 *
 * <p>Deterministic: plain double arithmetic and {@code Math.sqrt} only, so server and client compute
 * identical curves from identical rail lists.
 */
public final class CurveFitter {

    /** Max horizontal distance, in blocks, between the vanilla cart path and the smoothed curve. */
    public static final double TOLERANCE = 0.55;
    /** Vanilla rail surface height above the rail block's y ({@code AbstractMinecart.getPos}). */
    public static final double RAIL_HEIGHT = 0.0625;
    /** Spacing, in blocks, of the samples taken along the vanilla path before smoothing. */
    static final double SPACING = 0.5;
    /** Curvature weight. Higher = smoother, bounded by {@link #TOLERANCE}. */
    static final double LAMBDA = 3000.0;

    private static final double PINNED = -1.0;
    private static final int REWEIGHT_ROUNDS = 40;

    private CurveFitter() {
    }

    /**
     * @param nodes  rails in travel order; consecutive rails must be adjacent and connected
     * @param closed true if the last rail connects back to the first
     */
    public static SmoothCurve fit(List<RailNode> nodes, boolean closed) {
        validate(nodes, closed);
        int n = nodes.size();

        Pt[] centres = new Pt[n];
        for (int i = 0; i < n; i++) {
            RailNode r = nodes.get(i);
            centres[i] = new Pt(r.x() + 0.5, r.y() + RAIL_HEIGHT, r.z() + 0.5);
        }

        // The vanilla cart path runs straight from edge midpoint to edge midpoint across every rail
        // (through the centre of a straight rail, cutting across a corner). Smoothing that path —
        // not the rail centres — matters: on a staircase step two rails sit side by side, and a
        // curve pinned near both centres would have to jog between them.
        Pt[] anchors = new Pt[n];
        Pt[] edges = new Pt[closed ? n : n + 1]; // edges[i] = where rail i is entered
        for (int i = 0; i < n; i++) {
            RailNode r = nodes.get(i);
            Dir in = closed || i > 0
                    ? step(r, nodes.get((i - 1 + n) % n))
                    : r.otherExit(step(r, nodes.get(1)));
            edges[i] = edge(centres[i], in);
        }
        if (!closed) {
            RailNode last = nodes.get(n - 1);
            edges[n] = edge(centres[n - 1], last.otherExit(step(last, nodes.get(n - 2))));
        }
        for (int i = 0; i < n; i++) {
            Pt a = edges[i], b = edges[(i + 1) % edges.length];
            anchors[i] = a.add(b).scale(0.5);
        }

        Pt[] path = resample(edges, closed);

        if (closed) {
            double[] w = new double[path.length];
            java.util.Arrays.fill(w, 1.0);
            Pt[] smoothed = smooth(path, w, true, path);
            return SmoothCurve.build(smoothed, null, null, true, centres, anchors);
        }

        Dir startOut = nodes.get(0).otherExit(step(nodes.get(0), nodes.get(1)));
        Dir endOut = nodes.get(n - 1).otherExit(step(nodes.get(n - 1), nodes.get(n - 2)));
        int p = path.length;

        // [ghost2, ghost1, path0 (start edge) .. path(p-1) (end edge), ghost1, ghost2]
        int m = p + 4;
        Pt[] target = new Pt[m];
        double[] w = new double[m];
        target[0] = offset(path[0], startOut, 2 * SPACING);
        target[1] = offset(path[0], startOut, SPACING);
        for (int i = 0; i < p; i++) {
            target[2 + i] = path[i];
            w[2 + i] = 1.0;
        }
        target[m - 2] = offset(path[p - 1], endOut, SPACING);
        target[m - 1] = offset(path[p - 1], endOut, 2 * SPACING);
        w[0] = w[1] = w[2] = w[m - 3] = w[m - 2] = w[m - 1] = PINNED;

        Pt[] smoothedAll = smooth(target, w, false, target);
        Pt[] ctrl = new Pt[p];
        System.arraycopy(smoothedAll, 2, ctrl, 0, p);
        Pt startTangent = new Pt(-startOut.dx, 0, -startOut.dz);
        Pt endTangent = new Pt(endOut.dx, 0, endOut.dz);
        return SmoothCurve.build(ctrl, startTangent, endTangent, false, centres, anchors);
    }

    /** Points every {@link #SPACING} blocks along a polyline, keeping both ends of an open one. */
    private static Pt[] resample(Pt[] poly, boolean closed) {
        int segs = closed ? poly.length : poly.length - 1;
        double[] cum = new double[segs + 1];
        for (int i = 0; i < segs; i++) {
            cum[i + 1] = cum[i] + poly[(i + 1) % poly.length].sub(poly[i]).length();
        }
        double total = cum[segs];
        int count = Math.max(closed ? 4 : 2, (int) Math.round(total / SPACING));
        int pts = closed ? count : count + 1;
        Pt[] out = new Pt[pts];
        int seg = 0;
        for (int k = 0; k < pts; k++) {
            double s = total * k / count;
            while (seg < segs - 1 && cum[seg + 1] < s) seg++;
            double segLen = cum[seg + 1] - cum[seg];
            double f = segLen > 0 ? (s - cum[seg]) / segLen : 0;
            Pt a = poly[seg], b = poly[(seg + 1) % poly.length];
            out[k] = a.add(b.sub(a).scale(f));
        }
        if (!closed) out[pts - 1] = poly[poly.length - 1];
        return out;
    }

    private static Pt edge(Pt centre, Dir out) {
        return offset(centre, out, 0.5);
    }

    private static Pt offset(Pt p, Dir d, double k) {
        return new Pt(p.x() + d.dx * k, p.y(), p.z() + d.dz * k);
    }

    static Dir step(RailNode from, RailNode to) {
        return Dir.of(to.x() - from.x(), to.z() - from.z());
    }

    private static void validate(List<RailNode> nodes, boolean closed) {
        int n = nodes.size();
        if (n < (closed ? 4 : 2)) throw new IllegalArgumentException("run too short: " + n);
        int links = closed ? n : n - 1;
        for (int i = 0; i < links; i++) {
            RailNode a = nodes.get(i);
            RailNode b = nodes.get((i + 1) % n);
            int dx = b.x() - a.x(), dz = b.z() - a.z();
            if (Math.abs(dx) + Math.abs(dz) != 1 || a.y() != b.y()) {
                throw new IllegalArgumentException("rails " + i + " and " + (i + 1) + " are not flat neighbours");
            }
            Dir d = Dir.of(dx, dz);
            if (!a.hasExit(d) || !b.hasExit(d.opposite())) {
                throw new IllegalArgumentException("rails " + i + " and " + (i + 1) + " are not connected");
            }
        }
    }

    /**
     * Whittaker smoothing in x and z independently: minimise
     * {@code sum w_i (p_i - t_i)^2 + LAMBDA * sum |p_(i-1) - 2 p_i + p_(i+1)|^2},
     * with weight {@link #PINNED} meaning "fixed at target". Afterwards, points outside the tolerance
     * tube around {@code tube} are re-weighted and the system is re-solved.
     */
    private static Pt[] smooth(Pt[] target, double[] w, boolean cyclic, Pt[] tube) {
        int m = target.length;
        double[] tx = new double[m], tz = new double[m];
        for (int i = 0; i < m; i++) {
            tx[i] = target[i].x();
            tz[i] = target[i].z();
        }
        double[] x = tx.clone(), z = tz.clone();
        for (int round = 0; round < REWEIGHT_ROUNDS; round++) {
            x = solve(tx, w, cyclic, x);
            z = solve(tz, w, cyclic, z);
            boolean outside = false;
            for (int i = 0; i < m; i++) {
                if (w[i] == PINNED) continue;
                double dx = x[i] - tube[i].x(), dz = z[i] - tube[i].z();
                if (dx * dx + dz * dz > TOLERANCE * TOLERANCE) {
                    w[i] *= 3.0;
                    outside = true;
                }
            }
            if (!outside) break;
        }
        Pt[] out = new Pt[m];
        for (int i = 0; i < m; i++) {
            double dx = x[i] - tube[i].x(), dz = z[i] - tube[i].z();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (w[i] != PINNED && d > TOLERANCE) {
                dx *= TOLERANCE / d;
                dz *= TOLERANCE / d;
            }
            out[i] = new Pt(tube[i].x() + dx, target[i].y(), tube[i].z() + dz);
        }
        return out;
    }

    /** Conjugate gradient on the free (non-pinned) unknowns of {@code (W + LAMBDA D^T D) p = W t}. */
    private static double[] solve(double[] t, double[] w, boolean cyclic, double[] warm) {
        int m = t.length;
        // Right-hand side: W t, minus the operator applied to the pinned values (moved across).
        double[] pinnedOnly = new double[m];
        for (int i = 0; i < m; i++) if (w[i] == PINNED) pinnedOnly[i] = t[i];
        double[] pinnedTerm = applyDtD(pinnedOnly, cyclic);
        double[] b = new double[m];
        for (int i = 0; i < m; i++) {
            if (w[i] != PINNED) b[i] = w[i] * t[i] - LAMBDA * pinnedTerm[i];
        }

        double[] x = new double[m];
        for (int i = 0; i < m; i++) x[i] = w[i] == PINNED ? 0 : warm[i];
        double[] r = sub(b, apply(x, w, cyclic));
        double[] p = r.clone();
        double rr = dot(r, r);
        double bb = Math.max(dot(b, b), 1e-30);
        for (int it = 0; it < 4 * m + 200 && rr > 1e-22 * bb; it++) {
            double[] ap = apply(p, w, cyclic);
            double alpha = rr / dot(p, ap);
            for (int i = 0; i < m; i++) {
                x[i] += alpha * p[i];
                r[i] -= alpha * ap[i];
            }
            double rrNew = dot(r, r);
            double beta = rrNew / rr;
            for (int i = 0; i < m; i++) p[i] = r[i] + beta * p[i];
            rr = rrNew;
        }
        for (int i = 0; i < m; i++) if (w[i] == PINNED) x[i] = t[i];
        return x;
    }

    /** (W + LAMBDA D^T D) v restricted to free unknowns (pinned entries of v are treated as 0). */
    private static double[] apply(double[] v, double[] w, boolean cyclic) {
        int m = v.length;
        double[] free = new double[m];
        for (int i = 0; i < m; i++) free[i] = w[i] == PINNED ? 0 : v[i];
        double[] dtd = applyDtD(free, cyclic);
        double[] out = new double[m];
        for (int i = 0; i < m; i++) {
            if (w[i] != PINNED) out[i] = w[i] * free[i] + LAMBDA * dtd[i];
        }
        return out;
    }

    /** D^T D v, where D takes second differences (rows 1..m-2 open, all rows cyclic). */
    private static double[] applyDtD(double[] v, boolean cyclic) {
        int m = v.length;
        double[] d = new double[m];
        double[] out = new double[m];
        if (cyclic) {
            for (int i = 0; i < m; i++) {
                d[i] = v[(i - 1 + m) % m] - 2 * v[i] + v[(i + 1) % m];
            }
            for (int i = 0; i < m; i++) {
                out[(i - 1 + m) % m] += d[i];
                out[i] -= 2 * d[i];
                out[(i + 1) % m] += d[i];
            }
        } else {
            for (int i = 1; i < m - 1; i++) {
                d[i] = v[i - 1] - 2 * v[i] + v[i + 1];
                out[i - 1] += d[i];
                out[i] -= 2 * d[i];
                out[i + 1] += d[i];
            }
        }
        return out;
    }

    private static double[] sub(double[] a, double[] b) {
        double[] o = new double[a.length];
        for (int i = 0; i < a.length; i++) o[i] = a[i] - b[i];
        return o;
    }

    private static double dot(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }
}
