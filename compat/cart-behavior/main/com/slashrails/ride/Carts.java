package com.slashrails.ride;

import com.slashrails.mixin.AbstractMinecartAccessor;
import com.slashrails.mixin.MinecartFurnaceAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.entity.vehicle.OldMinecartBehavior;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;

/** Vanilla minecart internals the curve ride needs (MC 1.21.2+: behind {@code MinecartBehavior}). */
public final class Carts {

    private Carts() {
    }

    /**
     * Whether the cart runs its own rail physics. From 1.21.2 a cart's rail movement lives in its
     * {@code MinecartBehavior}; anything but vanilla's {@code OldMinecartBehavior} (a modded
     * behaviour, or a subclass of the old one) is left to itself on smoothed runs.
     */
    public static boolean ownsRailMovement(AbstractMinecart cart) {
        return cart.getBehavior().getClass() != OldMinecartBehavior.class;
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

    /** A new (not yet added) minecart of the given kind at the given position. */
    public static AbstractMinecart create(ServerLevel level, double x, double y, double z, CartKind kind) {
        return switch (kind) {
            case RIDDEN -> AbstractMinecart.createMinecart(level, x, y, z, EntityType.MINECART, EntitySpawnReason.COMMAND, ItemStack.EMPTY, null);
            case HOPPER -> AbstractMinecart.createMinecart(level, x, y, z, EntityType.HOPPER_MINECART, EntitySpawnReason.COMMAND, ItemStack.EMPTY, null);
            case FURNACE -> AbstractMinecart.createMinecart(level, x, y, z, EntityType.FURNACE_MINECART, EntitySpawnReason.COMMAND, ItemStack.EMPTY, null);
        };
    }

    /** Fuel a furnace cart and point its push along (dx, dz). */
    public static void startFurnace(AbstractMinecart cart, double dx, double dz) {
        MinecartFurnace furnace = (MinecartFurnace) cart;
        ((MinecartFurnaceAccessor) furnace).slashrails$setFuel(3600);
        furnace.push = new Vec3(dx, 0, dz);
    }
}
