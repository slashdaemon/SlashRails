package com.slashrails.fabric;

import net.minecraft.world.entity.vehicle.AbstractMinecart;

/** Default: the mod is required on every client, so a client without it is turned away at join. */
final class VanillaClients {

    static final boolean SUPPORTED = false;

    private VanillaClients() {
    }

    static void init() {
    }

    static boolean requireClientMod() {
        return true;
    }

    static void onCurveStep(AbstractMinecart cart) {
    }
}
