package com.slashrails.neoforge;

import com.slashrails.net.RemoveRunPayload;
import com.slashrails.net.RunsPayload;
import com.slashrails.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

final class NeoForgePlatform implements Platform {

    @Override
    public void sendRuns(ServerPlayer player, RunsPayload payload) {
        NeoForgeNet.send(player, payload);
    }

    @Override
    public void sendRemove(ServerPlayer player, RemoveRunPayload payload) {
        NeoForgeNet.send(player, payload);
    }

    /** The channel is required on both sides, so a player without SlashRails never gets this far. */
    @Override
    public boolean clientHasMod(ServerPlayer player) {
        return true;
    }

    @Override
    public double maxRailSpeed(AbstractMinecart cart) {
        return NeoForgeCarts.maxRailSpeed(cart);
    }

    @Override
    public void onMinecartPass(BlockState rail, Level level, BlockPos pos, AbstractMinecart cart) {
        NeoForgeCarts.onMinecartPass(rail, level, pos, cart);
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
