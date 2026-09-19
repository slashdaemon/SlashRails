package com.slashtracks.core;

/** Horizontal grid direction of a rail exit. North is -Z, east is +X (Minecraft convention). */
public enum Dir {
    NORTH(0, -1), SOUTH(0, 1), EAST(1, 0), WEST(-1, 0);

    public final int dx;
    public final int dz;

    Dir(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    public Dir opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }

    public static Dir of(int dx, int dz) {
        for (Dir d : values()) {
            if (d.dx == dx && d.dz == dz) return d;
        }
        throw new IllegalArgumentException("not a unit grid step: " + dx + "," + dz);
    }
}
