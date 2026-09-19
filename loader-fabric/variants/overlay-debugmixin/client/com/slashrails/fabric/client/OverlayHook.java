package com.slashrails.fabric.client;

/**
 * Fabric API for MC 1.21.9 - 1.21.10 has no world render events; the overlay is drawn from
 * {@code DebugRendererOverlayMixin} instead, so there is nothing to register.
 */
final class OverlayHook {

    private OverlayHook() {
    }

    static void register() {
    }
}
