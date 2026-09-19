package com.slashrails.core;

import org.junit.jupiter.api.Test;

/** Prints the worst per-tick heading change of the smoothed curve vs. the vanilla path. Always passes. */
class SmoothnessReport {

    @Test
    void report() {
        StringBuilder sb = new StringBuilder("\nfixture            rails  smoothed deg/tick   vanilla deg/tick\n");
        for (int n : new int[]{1, 2, 3, 4, 6, 10}) {
            line(sb, "staircase 1:" + n, CurveFitter.fit(Tracks.open(Tracks.staircase(n, 6)), false));
        }
        for (int r : new int[]{8, 16, 32}) {
            line(sb, "arc r=" + r, CurveFitter.fit(Tracks.open(Tracks.withApproaches(Tracks.quarterArc(r))), false));
        }
        line(sb, "s-curve r=12", CurveFitter.fit(Tracks.open(Tracks.sCurve(12)), false));
        line(sb, "loop r=10", CurveFitter.fit(Tracks.closed(Tracks.loop(10)), true));
        System.out.println(sb);
    }

    private static void line(StringBuilder sb, String name, SmoothCurve c) {
        double worst = 0;
        double end = c.closed() ? c.length() : c.length() - 0.4;
        for (double s = 0; s <= end; s += 0.05) {
            double a = Math.toDegrees(Math.atan2(c.tangentAt(s).z(), c.tangentAt(s).x()));
            double b = Math.toDegrees(Math.atan2(c.tangentAt(s + 0.4).z(), c.tangentAt(s + 0.4).x()));
            double d = Math.abs(b - a);
            if (d > 180) d = 360 - d;
            worst = Math.max(worst, d);
        }
        // Vanilla: the path through edge midpoints turns 45 degrees at every corner, instantly.
        sb.append(String.format("%-18s %5d  %17.2f   %16s%n", name, c.railCount(), worst, "45 (per kink)"));
    }
}
