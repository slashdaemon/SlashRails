package com.slashrails.core;

/**
 * A G1-continuous piecewise cubic Hermite curve, parameterised by arc length {@code s}.
 *
 * <p>Each control point carries a unit tangent (the averaged direction of its two chords, or the
 * vanilla rail axis at an open end). Segment {@code k} scales both end tangents by its own chord
 * length, so direction is continuous across segments with no overshoot on uneven spacing.
 *
 * <p>Each member rail owns a contiguous interval of {@code s}; walking {@code s} forward visits every
 * rail once, in order ({@link #railIndexAt}).
 */
public final class SmoothCurve {

    static final int SAMPLES_PER_SEGMENT = 16;

    private final boolean closed;
    private final Pt[] p;       // control points
    private final Pt[] tan;     // unit tangents at control points
    private final int segments;
    private final double[] lutS;   // cumulative arc length at each sample
    private final double length;
    private final Pt[] centres; // rail centres, in run order
    private final Pt[] anchors; // midpoint of the vanilla path across each rail
    private final double[] railStart; // s at which each rail's interval starts
    private final double[] railEnd;

    private SmoothCurve(boolean closed, Pt[] p, Pt[] tan, Pt[] centres, Pt[] anchors) {
        this.closed = closed;
        this.p = p;
        this.tan = tan;
        this.centres = centres;
        this.anchors = anchors;
        this.segments = closed ? p.length : p.length - 1;

        int samples = segments * SAMPLES_PER_SEGMENT;
        lutS = new double[samples + 1];
        Pt prev = evalSegment(0, 0);
        for (int j = 1; j <= samples; j++) {
            int seg = Math.min((j - 1) / SAMPLES_PER_SEGMENT, segments - 1);
            double t = (double) (j - seg * SAMPLES_PER_SEGMENT) / SAMPLES_PER_SEGMENT;
            Pt cur = evalSegment(seg, t);
            lutS[j] = lutS[j - 1] + cur.sub(prev).length();
            prev = cur;
        }
        length = lutS[samples];

        int n = centres.length;
        double[] sc = new double[n];
        double hint = 0;
        for (int i = 0; i < n; i++) {
            sc[i] = project(anchors[i].x(), anchors[i].y(), anchors[i].z(), i == 0 ? Double.NaN : hint, 3.0);
            if (!closed && i > 0 && sc[i] < sc[i - 1]) sc[i] = sc[i - 1];
            hint = sc[i];
        }
        railStart = new double[n];
        railEnd = new double[n];
        for (int i = 0; i < n; i++) {
            if (closed) {
                double prevC = sc[(i - 1 + n) % n];
                double nextC = sc[(i + 1) % n];
                railStart[i] = wrap(midpointForward(prevC, sc[i]));
                railEnd[i] = wrap(midpointForward(sc[i], nextC));
            } else {
                railStart[i] = i == 0 ? 0 : (sc[i - 1] + sc[i]) / 2;
                railEnd[i] = i == n - 1 ? length : (sc[i] + sc[i + 1]) / 2;
            }
        }
    }

    static SmoothCurve build(Pt[] ctrl, Pt startTangent, Pt endTangent, boolean closed, Pt[] centres, Pt[] anchors) {
        int m = ctrl.length;
        Pt[] tan = new Pt[m];
        for (int i = 0; i < m; i++) {
            if (!closed && i == 0) {
                tan[i] = startTangent.normalize();
            } else if (!closed && i == m - 1) {
                tan[i] = endTangent.normalize();
            } else {
                Pt before = ctrl[i].sub(ctrl[(i - 1 + m) % m]).normalize();
                Pt after = ctrl[(i + 1) % m].sub(ctrl[i]).normalize();
                tan[i] = before.add(after).normalize();
            }
        }
        return new SmoothCurve(closed, ctrl, tan, centres, anchors);
    }

    // ---- queries -------------------------------------------------------------------------

    public boolean closed() {
        return closed;
    }

    public double length() {
        return length;
    }

    public int railCount() {
        return centres.length;
    }

    public Pt railCentre(int i) {
        return centres[i];
    }

    /** Midpoint of the vanilla cart path across rail {@code i} (its centre, or cut in on a corner). */
    public Pt railAnchor(int i) {
        return anchors[i];
    }

    /** Start of rail {@code i}'s interval. For a closed curve this may be greater than its end (wraps). */
    public double railStart(int i) {
        return railStart[i];
    }

    public double railEnd(int i) {
        return railEnd[i];
    }

    /** Normalises {@code s} into the curve's domain: wraps on a loop, clamps on an open run. */
    public double normalize(double s) {
        return closed ? wrap(s) : Math.max(0, Math.min(length, s));
    }

    public Pt pointAt(double s) {
        double[] st = locate(normalize(s));
        return evalSegment((int) st[0], st[1]);
    }

    /** Unit tangent in the direction of increasing {@code s}. */
    public Pt tangentAt(double s) {
        double[] st = locate(normalize(s));
        return derivSegment((int) st[0], st[1]).normalize();
    }

    /** The rail whose interval contains {@code s}. */
    public int railIndexAt(double s) {
        s = normalize(s);
        int n = centres.length;
        if (!closed) {
            int lo = 0, hi = n - 1;
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (railStart[mid] <= s) lo = mid;
                else hi = mid - 1;
            }
            return lo;
        }
        for (int i = 0; i < n; i++) {
            double a = railStart[i], b = railEnd[i];
            if (a <= b ? (s >= a && s < b) : (s >= a || s < b)) return i;
        }
        return 0;
    }

    /**
     * Arc length of the curve point nearest to (x, z) — y is ignored, the curve is followed in plan view.
     *
     * @param hint   a previous {@code s} to search around, or {@code NaN} to search the whole curve
     * @param window how far either side of {@code hint} to search, in blocks
     */
    public double project(double x, double y, double z, double hint, double window) {
        int samples = lutS.length - 1;
        int from = 0, to = samples;
        boolean wrapSearch = false;
        if (!Double.isNaN(hint) && window < length / 2) {
            from = sampleIndex(hint - window);
            to = sampleIndex(hint + window);
            wrapSearch = closed && (hint - window < 0 || hint + window > length);
            if (!closed) {
                from = sampleIndex(Math.max(0, hint - window));
                to = sampleIndex(Math.min(length, hint + window));
            }
        }
        int best = -1;
        double bestD = Double.MAX_VALUE;
        int count = wrapSearch ? samples : (to - from);
        for (int k = 0; k <= count; k++) {
            int j = wrapSearch ? k : from + k;
            if (wrapSearch) {
                double sj = lutS[j];
                if (Math.abs(circularDelta(hint, sj)) > window) continue;
            }
            Pt q = sampleAt(j);
            double dx = q.x() - x, dz = q.z() - z;
            double d = dx * dx + dz * dz;
            if (d < bestD) {
                bestD = d;
                best = j;
            }
        }
        if (best < 0) best = 0;
        // Refine between the neighbouring samples with golden-section search.
        double a = lutS[Math.max(0, best - 1)];
        double b = lutS[Math.min(samples, best + 1)];
        if (closed && best == 0) a = -(length - lutS[samples - 1]);
        if (closed && best == samples) b = length + lutS[1];
        final double g = 0.6180339887498949;
        double c = b - g * (b - a), d = a + g * (b - a);
        double fc = dist2(c, x, z), fd = dist2(d, x, z);
        for (int it = 0; it < 40; it++) {
            if (fc < fd) {
                b = d;
                d = c;
                fd = fc;
                c = b - g * (b - a);
                fc = dist2(c, x, z);
            } else {
                a = c;
                c = d;
                fc = fd;
                d = a + g * (b - a);
                fd = dist2(d, x, z);
            }
        }
        return normalize((a + b) / 2);
    }

    /** Signed shortest distance from {@code a} to {@code b} along the curve (wraps on a loop). */
    public double delta(double a, double b) {
        return closed ? circularDelta(a, b) : b - a;
    }

    // ---- internals -----------------------------------------------------------------------

    private double circularDelta(double a, double b) {
        double d = (b - a) % length;
        if (d > length / 2) d -= length;
        if (d < -length / 2) d += length;
        return d;
    }

    private double wrap(double s) {
        double r = s % length;
        return r < 0 ? r + length : r;
    }

    private static double midpointForwardStatic(double a, double b, double len) {
        double d = b - a;
        if (d < 0) d += len;
        return a + d / 2;
    }

    private double midpointForward(double a, double b) {
        return midpointForwardStatic(a, b, length);
    }

    private double dist2(double s, double x, double z) {
        Pt q = pointAt(s);
        double dx = q.x() - x, dz = q.z() - z;
        return dx * dx + dz * dz;
    }

    private int sampleIndex(double s) {
        s = normalize(s);
        int lo = 0, hi = lutS.length - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (lutS[mid] <= s) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    private Pt sampleAt(int j) {
        int seg = Math.min(j / SAMPLES_PER_SEGMENT, segments - 1);
        double t = (double) (j - seg * SAMPLES_PER_SEGMENT) / SAMPLES_PER_SEGMENT;
        return evalSegment(seg, t);
    }

    /** {segment, t} for an in-domain arc length. */
    private double[] locate(double s) {
        int j = sampleIndex(s);
        int samples = lutS.length - 1;
        if (j >= samples) return new double[]{segments - 1, 1.0};
        double s0 = lutS[j], s1 = lutS[j + 1];
        double f = s1 > s0 ? (s - s0) / (s1 - s0) : 0;
        int seg = Math.min(j / SAMPLES_PER_SEGMENT, segments - 1);
        double t = (j - seg * SAMPLES_PER_SEGMENT + f) / SAMPLES_PER_SEGMENT;
        return new double[]{seg, Math.min(1.0, t)};
    }

    private Pt evalSegment(int k, double t) {
        Pt p0 = p[k], p1 = p[(k + 1) % p.length];
        double len = p1.sub(p0).length();
        Pt m0 = tan[k].scale(len), m1 = tan[(k + 1) % p.length].scale(len);
        double t2 = t * t, t3 = t2 * t;
        double h00 = 2 * t3 - 3 * t2 + 1, h10 = t3 - 2 * t2 + t, h01 = -2 * t3 + 3 * t2, h11 = t3 - t2;
        return p0.scale(h00).add(m0.scale(h10)).add(p1.scale(h01)).add(m1.scale(h11));
    }

    private Pt derivSegment(int k, double t) {
        Pt p0 = p[k], p1 = p[(k + 1) % p.length];
        double len = p1.sub(p0).length();
        Pt m0 = tan[k].scale(len), m1 = tan[(k + 1) % p.length].scale(len);
        double t2 = t * t;
        double d00 = 6 * t2 - 6 * t, d10 = 3 * t2 - 4 * t + 1, d01 = -6 * t2 + 6 * t, d11 = 3 * t2 - 2 * t;
        return p0.scale(d00).add(m0.scale(d10)).add(p1.scale(d01)).add(m1.scale(d11));
    }
}
