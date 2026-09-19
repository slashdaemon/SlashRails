package com.slashrails.core;

/** Immutable 3D point or vector. */
public record Pt(double x, double y, double z) {

    public Pt add(Pt o) {
        return new Pt(x + o.x, y + o.y, z + o.z);
    }

    public Pt sub(Pt o) {
        return new Pt(x - o.x, y - o.y, z - o.z);
    }

    public Pt scale(double k) {
        return new Pt(x * k, y * k, z * k);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double horizontalDistance(Pt o) {
        double dx = x - o.x, dz = z - o.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public Pt normalize() {
        double l = length();
        return l < 1e-12 ? this : scale(1.0 / l);
    }
}
