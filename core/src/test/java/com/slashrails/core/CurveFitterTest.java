package com.slashrails.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CurveFitterTest {

    /** Vanilla max cart speed on rails is 0.4 blocks per tick. */
    private static final double TICK = 0.4;
    private static final double EPS = 1e-6;

    // ---- smoothness: the reason the mod exists ------------------------------------------

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 8, 10})
    void gentleStaircaseTurnsLessThanFiveDegreesPerTick(int n) {
        SmoothCurve c = CurveFitter.fit(Tracks.open(Tracks.staircase(n, 6)), false);
        double worst = maxHeadingChangePerStep(c, TICK);
        assertTrue(worst <= 5.0, "1:" + n + " staircase turns " + worst + " deg in one tick");
    }

    @Test
    void diagonalBecomesStraight() {
        SmoothCurve c = CurveFitter.fit(Tracks.open(Tracks.diagonal(24)), false);
        double mid = c.length() / 2;
        for (double s = mid - 4; s <= mid + 4; s += 0.25) {
            Pt t = c.tangentAt(s);
            double deg = Math.toDegrees(Math.atan2(-t.z(), t.x()));
            assertEquals(45.0, deg, 3.0, "heading at s=" + s);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 12, 20, 32})
    void rasterisedArcRidesSmoothly(int r) {
        SmoothCurve c = CurveFitter.fit(Tracks.open(Tracks.withApproaches(Tracks.quarterArc(r))), false);
        double worst = maxHeadingChangePerStep(c, TICK);
        // A true circle of radius r turns 0.4/r rad per tick; allow wobble on top of that.
        double ideal = Math.toDegrees(TICK / r);
        assertTrue(worst <= ideal + 4.0, "radius " + r + ": " + worst + " deg/tick (ideal " + ideal + ")");
    }

    @Test
    void sCurveIsSmooth() {
        SmoothCurve c = CurveFitter.fit(Tracks.open(Tracks.sCurve(12)), false);
        double worst = maxHeadingChangePerStep(c, TICK);
        assertTrue(worst <= 6.0, "s-curve turns " + worst + " deg in one tick");
    }

    // ---- geometry contract ----------------------------------------------------------------

    @Test
    void straightRunStaysOnItsRails() {
        SmoothCurve c = CurveFitter.fit(Tracks.open(Tracks.repeat(Dir.EAST, 20)), false);
        for (double s = 0; s <= c.length(); s += 0.1) {
            assertEquals(0.5, c.pointAt(s).z(), EPS);
        }
        assertEquals(21.0, c.length(), 1e-3);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 5, 10})
    void curveNeverLeavesItsRails(int n) {
        SmoothCurve c = CurveFitter.fit(Tracks.open(Tracks.staircase(n, 5)), false);
        for (int i = 0; i < c.railCount(); i++) {
            Pt anchor = c.railAnchor(i);
            double s = c.project(anchor.x(), anchor.y(), anchor.z(), Double.NaN, 0);
            double d = c.pointAt(s).horizontalDistance(anchor);
            assertTrue(d <= CurveFitter.TOLERANCE + 0.02, "rail " + i + " is " + d + " from the curve");
        }
    }

    @Test
    void openEndsMeetTheVanillaRailTangentially() {
        List<RailNode> rails = Tracks.open(Tracks.staircase(4, 4));
        SmoothCurve c = CurveFitter.fit(rails, false);
        Pt start = c.pointAt(0);
        assertEquals(0.0, start.x(), EPS);  // west edge of the first rail
        assertEquals(0.5, start.z(), EPS);
        Pt t0 = c.tangentAt(0);
        assertEquals(1.0, t0.x(), 1e-3);    // heading east into the run
        assertEquals(0.0, t0.z(), 1e-3);

        RailNode last = rails.get(rails.size() - 1);
        Pt end = c.pointAt(c.length());
        assertEquals(last.x() + 1.0, end.x(), EPS);  // east edge of the last rail
        assertEquals(last.z() + 0.5, end.z(), EPS);
        Pt t1 = c.tangentAt(c.length());
        assertEquals(1.0, t1.x(), 1e-3);
    }

    @Test
    void railIntervalsVisitEveryRailOnceInOrder() {
        SmoothCurve c = CurveFitter.fit(Tracks.open(Tracks.staircase(3, 6)), false);
        int expected = 0;
        for (double s = 0; s <= c.length(); s += 0.02) {
            int i = c.railIndexAt(s);
            assertTrue(i == expected || i == expected + 1, "jumped from rail " + expected + " to " + i);
            expected = i;
        }
        assertEquals(c.railCount() - 1, expected);
    }

    @Test
    void cartOnARailIntervalIsOverThatRail() {
        // The rail that owns s must be the block (or a neighbour of the block) the cart is over,
        // so detector and powered rails see the cart.
        SmoothCurve c = CurveFitter.fit(Tracks.open(Tracks.staircase(2, 6)), false);
        for (double s = 0.01; s < c.length(); s += 0.05) {
            Pt p = c.pointAt(s);
            Pt centre = c.railCentre(c.railIndexAt(s));
            assertTrue(Math.abs(p.x() - centre.x()) <= 1.0 && Math.abs(p.z() - centre.z()) <= 1.0,
                    "at s=" + s + " the cart is far from its owning rail");
        }
    }

    @Test
    void projectionRoundTrips() {
        SmoothCurve c = CurveFitter.fit(Tracks.open(Tracks.withApproaches(Tracks.quarterArc(10))), false);
        for (double s = 0.1; s < c.length(); s += 0.37) {
            Pt p = c.pointAt(s);
            assertEquals(s, c.project(p.x(), p.y(), p.z(), Double.NaN, 0), 1e-3);
            assertEquals(s, c.project(p.x(), p.y(), p.z(), s + 1.0, 3.0), 1e-3);
        }
    }

    @Test
    void railHeightMatchesVanilla() {
        SmoothCurve c = CurveFitter.fit(Tracks.open(Tracks.staircase(2, 3)), false);
        assertEquals(64.0625, c.pointAt(c.length() / 3).y(), EPS);
    }

    @Test
    void fittingIsDeterministic() {
        List<RailNode> rails = Tracks.open(Tracks.sCurve(9));
        SmoothCurve a = CurveFitter.fit(rails, false);
        SmoothCurve b = CurveFitter.fit(rails, false);
        for (double s = 0; s < a.length(); s += 0.5) {
            assertEquals(a.pointAt(s), b.pointAt(s));
        }
    }

    @Test
    void longRunFitsQuickly() {
        List<Dir> steps = new ArrayList<>();
        for (int i = 0; i < 40; i++) steps.addAll(Tracks.staircase(5, 2));
        long t0 = System.nanoTime();
        SmoothCurve c = CurveFitter.fit(Tracks.open(steps), false);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertTrue(c.railCount() > 500);
        assertTrue(ms < 2000, "fitting " + c.railCount() + " rails took " + ms + " ms");
    }

    @Test
    void tightRailsKeepTheVanillaLine() {
        // A wall beside part of the track: the curve must not leave those rails' vanilla line.
        List<RailNode> rails = Tracks.open(Tracks.staircase(3, 6));
        boolean[] tight = new boolean[rails.size()];
        for (int i = 10; i < 16; i++) tight[i] = true;
        SmoothCurve c = CurveFitter.fit(rails, false, tight);
        for (int i = 10; i < 16; i++) {
            Pt anchor = c.railAnchor(i);
            double s = c.project(anchor.x(), anchor.y(), anchor.z(), Double.NaN, 0);
            assertTrue(c.pointAt(s).horizontalDistance(anchor) <= 0.02,
                    "tight rail " + i + " moved " + c.pointAt(s).horizontalDistance(anchor));
        }
        // Rails away from the wall are still smoothed.
        SmoothCurve free = CurveFitter.fit(rails, false);
        Pt a = c.railAnchor(rails.size() - 10);
        double s1 = c.project(a.x(), a.y(), a.z(), Double.NaN, 0);
        double s2 = free.project(a.x(), a.y(), a.z(), Double.NaN, 0);
        assertEquals(free.pointAt(s2).horizontalDistance(a), c.pointAt(s1).horizontalDistance(a), 0.2);
    }

    // ---- loops ----------------------------------------------------------------------------

    @Test
    void closedLoopIsSmoothAndWraps() {
        SmoothCurve c = CurveFitter.fit(Tracks.closed(Tracks.loop(10)), true);
        assertTrue(c.closed());
        double worst = maxHeadingChangePerStep(c, TICK);
        assertTrue(worst <= Math.toDegrees(TICK / 10) + 4.0, "loop turns " + worst + " deg in one tick");
        assertEquals(c.pointAt(0).x(), c.pointAt(c.length()).x(), 1e-6);
        assertEquals(c.pointAt(0.3).x(), c.pointAt(c.length() + 0.3).x(), 1e-6);

        int last = c.railIndexAt(0);
        boolean[] seen = new boolean[c.railCount()];
        int visited = 0;
        for (double s = 0; s < c.length(); s += 0.02) {
            int i = c.railIndexAt(s);
            if (i != last) {
                assertEquals((last + 1) % c.railCount(), i, "loop skipped a rail");
                last = i;
            }
            if (!seen[i]) {
                seen[i] = true;
                visited++;
            }
        }
        assertEquals(c.railCount(), visited);
    }

    // ---- validation -----------------------------------------------------------------------

    @Test
    void rejectsDisconnectedRails() {
        List<RailNode> rails = new ArrayList<>(Tracks.open(Tracks.repeat(Dir.EAST, 4)));
        RailNode r = rails.get(2);
        rails.set(2, new RailNode(r.x(), r.y(), r.z(), Dir.NORTH, Dir.SOUTH));
        assertThrows(IllegalArgumentException.class, () -> CurveFitter.fit(rails, false));
    }

    // ---- helpers --------------------------------------------------------------------------

    /** Largest heading change, in degrees, between curve points {@code step} apart. */
    private static double maxHeadingChangePerStep(SmoothCurve c, double step) {
        double worst = 0;
        double end = c.closed() ? c.length() : c.length() - step;
        for (double s = 0; s <= end; s += 0.05) {
            double a = heading(c.tangentAt(s));
            double b = heading(c.tangentAt(s + step));
            double d = Math.abs(b - a);
            if (d > 180) d = 360 - d;
            worst = Math.max(worst, d);
        }
        return worst;
    }

    private static double heading(Pt t) {
        return Math.toDegrees(Math.atan2(t.z(), t.x()));
    }
}
