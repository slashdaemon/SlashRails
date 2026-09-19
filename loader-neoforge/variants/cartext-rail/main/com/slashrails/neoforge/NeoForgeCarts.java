package com.slashrails.neoforge;

import com.slashrails.ride.Carts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;

/** NeoForge 21.2+: the minecart extension is gone; only the rail-block hooks remain. */
final class NeoForgeCarts {

    private NeoForgeCarts() {
    }

    static double maxRailSpeed(AbstractMinecart cart) {
        return Carts.vanillaMaxSpeed(cart);
    }

    static void onMinecartPass(BlockState rail, Level level, BlockPos pos, AbstractMinecart cart) {
        if (rail.getBlock() instanceof BaseRailBlock block) {
            block.onMinecartPass(rail, level, pos, cart);
        }
    }
}
