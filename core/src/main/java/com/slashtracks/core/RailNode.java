package com.slashtracks.core;

/**
 * One flat rail block in a run: its block position and its two exits.
 * A straight rail has opposite exits; a corner rail has perpendicular ones.
 */
public record RailNode(int x, int y, int z, Dir a, Dir b) {

    public boolean hasExit(Dir d) {
        return a == d || b == d;
    }

    /** The exit that is not {@code d}. */
    public Dir otherExit(Dir d) {
        if (a == d) return b;
        if (b == d) return a;
        throw new IllegalArgumentException(d + " is not an exit of " + this);
    }

    public boolean isCorner() {
        return a.opposite() != b;
    }
}
