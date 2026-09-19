package com.slashtracks.ride;

/**
 * Per-cart ride state, mixed into {@code AbstractMinecart}. Transient: after a reload the cart simply
 * re-projects onto the curve.
 */
public interface CartRide {

    /** Id of the run being ridden, or 0. */
    int slashtracks$runId();

    /** Arc length along the run's curve (or NaN when unknown). */
    double slashtracks$s();

    /** +1 when travelling towards increasing arc length, -1 otherwise. */
    int slashtracks$sign();

    void slashtracks$set(int runId, double s, int sign);

    default void slashtracks$clear() {
        slashtracks$set(0, Double.NaN, slashtracks$sign());
    }
}
