package com.slashrails.forge;

import com.slashrails.net.RemoveRunPayload;
import com.slashrails.net.RunsPayload;
import com.slashrails.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

final class ForgePlatform implements Platform {

    @Override
    public void sendRuns(ServerPlayer player, RunsPayload payload) {
        ForgeNet.send(player, payload);
    }

    @Override
    public void sendRemove(ServerPlayer player, RemoveRunPayload payload) {
        ForgeNet.send(player, payload);
    }

    @Override
    public boolean clientHasMod(ServerPlayer player) {
        return ForgeNet.isPresent(player);
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
