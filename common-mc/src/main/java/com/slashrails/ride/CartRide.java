package com.slashrails.ride;

/**
 * Per-cart ride state, mixed into {@code AbstractMinecart}. Transient: after a reload the cart simply
 * re-projects onto the curve.
 */
public interface CartRide {

    /** Id of the run being ridden, or 0. */
    int slashrails$runId();

    /** Arc length along the run's curve (or NaN when unknown). */
    double slashrails$s();

    /** +1 when travelling towards increasing arc length, -1 otherwise. */
    int slashrails$sign();

    void slashrails$set(int runId, double s, int sign);

    default void slashrails$clear() {
        slashrails$set(0, Double.NaN, slashrails$sign());
    }
}
