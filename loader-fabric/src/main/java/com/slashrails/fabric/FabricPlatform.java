package com.slashrails.fabric;

import com.slashrails.SlashRails;
import com.slashrails.net.RemoveRunPayload;
import com.slashrails.net.RunsPayload;
import com.slashrails.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;

final class FabricPlatform implements Platform {

    static String modVersion() {
        return FabricLoader.getInstance().getModContainer(SlashRails.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
    }

    @Override
    public void sendRuns(ServerPlayer player, RunsPayload payload) {
        FabricNet.sendRuns(player, payload);
    }

    @Override
    public void sendRemove(ServerPlayer player, RemoveRunPayload payload) {
        FabricNet.sendRemove(player, payload);
    }

    @Override
    public boolean clientHasMod(ServerPlayer player) {
        return FabricNet.canSend(player);
    }

    @Override
    public double maxRailSpeed(AbstractMinecart cart) {
        return com.slashrails.ride.Carts.vanillaMaxSpeed(cart);
    }

    @Override
    public void onMinecartPass(BlockState rail, Level level, BlockPos pos, AbstractMinecart cart) {
        // Vanilla has no per-rail pass hook.
    }

    @Override
    public boolean supportsVanillaClients() {
        return VanillaClients.SUPPORTED;
    }

    @Override
    public void onCurveStep(AbstractMinecart cart) {
        VanillaClients.onCurveStep(cart);
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
