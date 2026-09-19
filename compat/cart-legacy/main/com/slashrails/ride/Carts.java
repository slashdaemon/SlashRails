package com.slashrails.ride;

import com.slashrails.mixin.AbstractMinecartAccessor;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.server.level.ServerLevel;

/** Vanilla minecart internals the curve ride needs (MC ≤ 1.21.1: the physics live on the cart). */
public final class Carts {

    private Carts() {
    }

    public static boolean isOnRails(AbstractMinecart cart) {
        return ((AbstractMinecartAccessor) cart).slashrails$isOnRails();
    }

    public static void setOnRails(AbstractMinecart cart, boolean onRails) {
        ((AbstractMinecartAccessor) cart).slashrails$setOnRails(onRails);
    }

    /** Vanilla's per-tick slowdown on the cart's current velocity (furnace carts add their push). */
    public static void applyNaturalSlowdown(AbstractMinecart cart) {
        ((AbstractMinecartAccessor) cart).slashrails$applyNaturalSlowdown();
    }

    /** Vanilla's per-tick movement cap on rails. */
    public static double vanillaMaxSpeed(AbstractMinecart cart) {
        return ((AbstractMinecartAccessor) cart).slashrails$getMaxSpeed();
    }

    /** A new (not yet added) rideable or hopper minecart at the given position. */
    public static AbstractMinecart create(ServerLevel level, double x, double y, double z, boolean hopper) {
        return hopper ? new MinecartHopper(level, x, y, z) : new Minecart(level, x, y, z);
    }
}
