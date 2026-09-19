package com.slashtracks.core;

import java.util.ArrayList;
import java.util.List;

/** Builds rail runs from grid walks, the way a player would lay them. */
final class Tracks {

    private Tracks() {
    }

    /** An open walk of unit steps from (0,0); the end rails continue straight. */
    static List<RailNode> open(List<Dir> steps) {
        int n = steps.size() + 1;
        int[] xs = new int[n], zs = new int[n];
        for (int i = 0; i < steps.size(); i++) {
            xs[i + 1] = xs[i] + steps.get(i).dx;
            zs[i + 1] = zs[i] + steps.get(i).dz;
        }
        List<RailNode> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Dir back = i > 0 ? steps.get(i - 1).opposite() : null;
            Dir fwd = i < n - 1 ? steps.get(i) : null;
            if (back == null) back = fwd.opposite();
            if (fwd == null) fwd = back.opposite();
            out.add(new RailNode(xs[i], 64, zs[i], back, fwd));
        }
        return out;
    }

    /** A closed walk (must return to the start). */
    static List<RailNode> closed(List<Dir> steps) {
        int n = steps.size();
        List<RailNode> out = new ArrayList<>();
        int x = 0, z = 0;
        for (int i = 0; i < n; i++) {
            Dir back = steps.get((i - 1 + n) % n).opposite();
            Dir fwd = steps.get(i);
            out.add(new RailNode(x, 64, z, back, fwd));
            x += fwd.dx;
            z += fwd.dz;
        }
        if (x != 0 || z != 0) throw new IllegalStateException("walk does not close");
        return out;
    }

    static List<Dir> repeat(Dir d, int k) {
        List<Dir> l = new ArrayList<>();
        for (int i = 0; i < k; i++) l.add(d);
        return l;
    }

    /** 1:N staircase heading east, stepping north once every N, with straight approaches. */
    static List<Dir> staircase(int n, int stepsCount) {
        List<Dir> l = new ArrayList<>(repeat(Dir.EAST, 6));
        for (int s = 0; s < stepsCount; s++) {
            l.addAll(repeat(Dir.EAST, n));
            l.add(Dir.NORTH);
        }
        l.addAll(repeat(Dir.EAST, 6));
        return l;
    }

    /** Alternating corners heading north-east, with straight approaches. */
    static List<Dir> diagonal(int pairs) {
        List<Dir> l = new ArrayList<>(repeat(Dir.EAST, 4));
        for (int i = 0; i < pairs; i++) {
            l.add(Dir.EAST);
            l.add(Dir.NORTH);
        }
        l.addAll(repeat(Dir.NORTH, 4));
        return l;
    }

    /** 4-connected rasterisation of a quarter circle, from heading east to heading north. */
    static List<Dir> quarterArc(int r) {
        List<Dir> l = new ArrayList<>();
        // Centre at (0, -r); start at (0,0) heading east; end at (r, -r) heading north.
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
        List<Dir> l = new ArrayList<>(repeat(body.get(0), 5));
        l.addAll(body);
        l.addAll(repeat(body.get(body.size() - 1), 5));
        return l;
    }

    /** A left-turning arc into a right-turning arc. */
    static List<Dir> sCurve(int r) {
        List<Dir> first = quarterArc(r);
        List<Dir> l = new ArrayList<>(first);
        for (Dir d : first) {
            l.add(d == Dir.EAST ? Dir.NORTH : Dir.EAST);
        }
        return withApproaches(l);
    }

    /** Closed rasterised circle made of four quarter arcs. */
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
