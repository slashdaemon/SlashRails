package com.slashrails.ride;

import com.slashrails.SlashRails;
import com.slashrails.mixin.AbstractMinecartAccessor;
import com.slashrails.mixin.MinecartFurnaceAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/** Vanilla minecart internals the curve ride needs (MC ≤ 1.21.1: the physics live on the cart). */
public final class Carts {

    private Carts() {
    }

    /**
     * Rail-movement methods a modded cart can override to run its own physics: vanilla
     * {@code moveAlongTrack(BlockPos, BlockState)} and, on NeoForge/Forge, the loader's
     * {@code moveMinecartOnRail(BlockPos)}. Looked up once. {@code moveAlongTrack} is found by its
     * signature (the only {@code (BlockPos, BlockState) -> void} method on the cart), so this works
     * under intermediary and SRG names in production; the loader method keeps its name everywhere.
     */
    private static final List<Method> MOVEMENT_METHODS = movementMethods();

    private static final ClassValue<Boolean> OWNS_MOVEMENT = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            for (Class<?> c = type; c != null && c != AbstractMinecart.class; c = c.getSuperclass()) {
                // Vanilla subclasses (the furnace cart) are mirrored by CurveRide itself.
                if (c.getName().startsWith("net.minecraft.")) continue;
                for (Method m : MOVEMENT_METHODS) {
                    try {
                        c.getDeclaredMethod(m.getName(), m.getParameterTypes());
                        return true;
                    } catch (NoSuchMethodException ignored) {
                        // not overridden here
                    }
                }
            }
            return false;
        }
    };

    /**
     * Whether the cart runs its own rail physics (a modded locomotive, say). Such carts are left to
     * vanilla on smoothed runs: skipping their override would skip their engine.
     */
    public static boolean ownsRailMovement(AbstractMinecart cart) {
        return OWNS_MOVEMENT.get(cart.getClass());
    }

    private static List<Method> movementMethods() {
        List<Method> out = new ArrayList<>();
        Method track = null;
        int found = 0;
        for (Method m : AbstractMinecart.class.getDeclaredMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (m.getReturnType() == void.class && !Modifier.isStatic(m.getModifiers()) && p.length == 2
                    && p[0] == BlockPos.class && p[1] == BlockState.class) {
                found++;
                if (track == null || m.getName().equals("moveAlongTrack")) track = m;
            }
        }
        if (track != null) out.add(track);
        if (found != 1) SlashRails.LOG.warn("Expected one (BlockPos, BlockState) method on AbstractMinecart, found {}", found);
        try {
            out.add(AbstractMinecart.class.getMethod("moveMinecartOnRail", BlockPos.class));
        } catch (NoSuchMethodException ignored) {
            // Fabric: no loader rail-movement hook
        }
        return out;
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

    /** A new (not yet added) minecart of the given kind at the given position. */
    public static AbstractMinecart create(ServerLevel level, double x, double y, double z, CartKind kind) {
        return switch (kind) {
            case RIDDEN -> new Minecart(level, x, y, z);
            case HOPPER -> new MinecartHopper(level, x, y, z);
            case FURNACE -> new MinecartFurnace(level, x, y, z);
        };
    }

    /** Fuel a furnace cart and point its push along (dx, dz). */
    public static void startFurnace(AbstractMinecart cart, double dx, double dz) {
        MinecartFurnace furnace = (MinecartFurnace) cart;
        ((MinecartFurnaceAccessor) furnace).slashrails$setFuel(3600);
        furnace.xPush = dx;
        furnace.zPush = dz;
    }
}
