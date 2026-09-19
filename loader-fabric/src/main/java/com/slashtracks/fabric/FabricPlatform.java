package com.slashtracks.fabric;

import com.slashtracks.SlashTracks;
import com.slashtracks.mixin.AbstractMinecartAccessor;
import com.slashtracks.net.RunsPayload;
import com.slashtracks.platform.Platform;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;

final class FabricPlatform implements Platform {

    static String modVersion() {
        return FabricLoader.getInstance().getModContainer(SlashTracks.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public boolean clientHasMod(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, RunsPayload.TYPE);
    }

    @Override
    public double maxRailSpeed(AbstractMinecart cart) {
        return ((AbstractMinecartAccessor) cart).slashtracks$getMaxSpeed();
    }

    @Override
    public void onMinecartPass(BlockState rail, Level level, BlockPos pos, AbstractMinecart cart) {
        // Vanilla has no per-rail pass hook.
    }

    @Override
    public boolean isActivatorRail(BlockState rail) {
        return rail.is(net.minecraft.world.level.block.Blocks.ACTIVATOR_RAIL);
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
