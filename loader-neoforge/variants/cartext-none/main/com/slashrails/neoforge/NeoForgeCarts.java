package com.slashrails.neoforge;

import com.slashrails.ride.Carts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** NeoForge 26.1+: rails no longer carry pass or speed hooks; vanilla behaviour only. */
final class NeoForgeCarts {

    private NeoForgeCarts() {
    }

    static double maxRailSpeed(AbstractMinecart cart) {
        return Carts.vanillaMaxSpeed(cart);
    }

    static void onMinecartPass(BlockState rail, Level level, BlockPos pos, AbstractMinecart cart) {
    }
}
