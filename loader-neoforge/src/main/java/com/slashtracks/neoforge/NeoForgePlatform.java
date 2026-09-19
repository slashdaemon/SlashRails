package com.slashtracks.neoforge;

import com.slashtracks.net.RunsPayload;
import com.slashtracks.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;

final class NeoForgePlatform implements Platform {

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public boolean clientHasMod(ServerPlayer player) {
        return player.connection.hasChannel(RunsPayload.TYPE);
    }

    @Override
    public double maxRailSpeed(AbstractMinecart cart) {
        return cart.getMaxSpeedWithRail();
    }

    @Override
    public void onMinecartPass(BlockState rail, Level level, BlockPos pos, AbstractMinecart cart) {
        if (cart.shouldDoRailFunctions() && rail.getBlock() instanceof BaseRailBlock block) {
            block.onMinecartPass(rail, level, pos, cart);
        }
    }

    @Override
    public boolean isActivatorRail(BlockState rail) {
        return rail.getBlock() instanceof PoweredRailBlock p && p.isActivatorRail();
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
