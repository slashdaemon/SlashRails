package com.slashrails.ride;

import com.slashrails.mixin.AbstractMinecartAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;

/** Vanilla minecart internals the curve ride needs (MC 1.21.2+: behind {@code MinecartBehavior}). */
public final class Carts {

    private Carts() {
    }

    public static boolean isOnRails(AbstractMinecart cart) {
        return cart.isOnRails();
    }

    public static void setOnRails(AbstractMinecart cart, boolean onRails) {
        cart.setOnRails(onRails);
    }

    /** Vanilla's per-tick slowdown on the cart's current velocity (furnace carts add their push). */
    public static void applyNaturalSlowdown(AbstractMinecart cart) {
        cart.setDeltaMovement(((AbstractMinecartAccessor) cart).slashrails$applyNaturalSlowdown(cart.getDeltaMovement()));
    }

    /** Vanilla's per-tick movement cap on rails. */
    public static double vanillaMaxSpeed(AbstractMinecart cart) {
        return ((AbstractMinecartAccessor) cart).slashrails$getMaxSpeed((ServerLevel) cart.level());
    }

    /** A new (not yet added) rideable or hopper minecart at the given position. */
    public static AbstractMinecart create(ServerLevel level, double x, double y, double z, boolean hopper) {
        if (hopper) {
            return AbstractMinecart.createMinecart(level, x, y, z, EntityType.HOPPER_MINECART, EntitySpawnReason.COMMAND, ItemStack.EMPTY, null);
        }
        return AbstractMinecart.createMinecart(level, x, y, z, EntityType.MINECART, EntitySpawnReason.COMMAND, ItemStack.EMPTY, null);
    }
}
