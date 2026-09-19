package com.slashrails.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;

/** NeoForge ≤ 21.1: carts carry IAbstractMinecartExtension (rail-aware max speed, rail functions switch). */
final class NeoForgeCarts {

    private NeoForgeCarts() {
    }

    static double maxRailSpeed(AbstractMinecart cart) {
        return cart.getMaxSpeedWithRail();
    }

    static void onMinecartPass(BlockState rail, Level level, BlockPos pos, AbstractMinecart cart) {
        if (cart.shouldDoRailFunctions() && rail.getBlock() instanceof BaseRailBlock block) {
            block.onMinecartPass(rail, level, pos, cart);
        }
    }
}
