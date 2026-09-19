package com.slashtracks.command;

import com.slashtracks.core.Dir;

import java.util.ArrayList;
import java.util.List;

/** Grid walks for the {@code /slashtracks testtrack} fixtures (mirrors the core test fixtures). */
final class TestTracks {

    private TestTracks() {
    }

    static List<Dir> repeat(Dir d, int k) {
        List<Dir> l = new ArrayList<>();
        for (int i = 0; i < k; i++) l.add(d);
        return l;
    }

    static List<Dir> staircase(int n, int steps) {
        List<Dir> l = new ArrayList<>(repeat(Dir.EAST, 8));
        for (int s = 0; s < steps; s++) {
            l.addAll(repeat(Dir.EAST, n));
            l.add(Dir.NORTH);
        }
        l.addAll(repeat(Dir.EAST, 8));
        return l;
    }

    static List<Dir> diagonal(int pairs) {
        List<Dir> l = new ArrayList<>(repeat(Dir.EAST, 8));
        for (int i = 0; i < pairs; i++) {
            l.add(Dir.EAST);
            l.add(Dir.NORTH);
        }
        l.addAll(repeat(Dir.NORTH, 8));
        return l;
    }

    static List<Dir> quarterArc(int r) {
        List<Dir> l = new ArrayList<>();
        int x = 0, z = 0;
        while (!(x == r && z == -r)) {
            double ex = err(x + 1, z, r), en = err(x, z - 1, r);
            boolean canE = x < r, canN = z > -r;
            if (canE && (!canN || ex <= en)) {
                l.add(Dir.EAST);
                x++;
            } else {
                l.add(Dir.NORTH);
                z--;
            }
        }
        return l;
    }

    private static double err(int x, int z, int r) {
        double dx = x, dz = z + r;
        return Math.abs(Math.sqrt(dx * dx + dz * dz) - r);
    }

    static List<Dir> withApproaches(List<Dir> body) {
        List<Dir> l = new ArrayList<>(repeat(body.get(0), 8));
        l.addAll(body);
        l.addAll(repeat(body.get(body.size() - 1), 8));
        return l;
    }

    static List<Dir> arc(int r) {
        return withApproaches(quarterArc(r));
    }

    static List<Dir> sCurve(int r) {
        List<Dir> first = quarterArc(r);
        List<Dir> l = new ArrayList<>(first);
        for (Dir d : first) l.add(d == Dir.EAST ? Dir.NORTH : Dir.EAST);
        return withApproaches(l);
    }

    static List<Dir> corner() {
        List<Dir> l = new ArrayList<>(repeat(Dir.EAST, 10));
        l.addAll(repeat(Dir.NORTH, 10));
        return l;
    }

    static List<Dir> loop(int r) {
        List<Dir> q = quarterArc(r);
        List<Dir> l = new ArrayList<>();
        Dir[][] rot = {
            {Dir.EAST, Dir.NORTH}, {Dir.NORTH, Dir.WEST}, {Dir.WEST, Dir.SOUTH}, {Dir.SOUTH, Dir.EAST}
        };
        for (Dir[] map : rot) {
            for (Dir d : q) l.add(d == Dir.EAST ? map[0] : map[1]);
        }
        return l;
    }
}
