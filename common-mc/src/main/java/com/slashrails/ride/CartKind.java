package com.slashrails.ride;

/** The vanilla cart a self-test ride uses. */
public enum CartKind {
    /** A minecart with a rider. */
    RIDDEN,
    /** An empty hopper minecart (lighter, coasts to a stop). */
    HOPPER,
    /** A fuelled furnace minecart pushing along its start direction. */
    FURNACE
}
