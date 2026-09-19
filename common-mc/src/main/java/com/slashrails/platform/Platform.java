package com.slashrails.platform;

import net.minecraft.core.BlockPos;
import com.slashrails.net.RemoveRunPayload;
import com.slashrails.net.RunsPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;

/** The few things the shared code needs from the mod loader. Set once by the loader entrypoint. */
public interface Platform {

    void sendRuns(ServerPlayer player, RunsPayload payload);

    void sendRemove(ServerPlayer player, RemoveRunPayload payload);

    /** Whether the player's client has SlashRails (it is required on both sides). */
    boolean clientHasMod(ServerPlayer player);

    /** The cart's max speed on rails, in blocks per tick (NeoForge lets rails and carts change it). */
    double maxRailSpeed(AbstractMinecart cart);

    /** Loader hook fired as a cart passes over a rail (NeoForge {@code onMinecartPass}; no-op on Fabric). */
    void onMinecartPass(BlockState rail, Level level, BlockPos pos, AbstractMinecart cart);

    /** Activator rails share {@code PoweredRailBlock} with powered rails (NeoForge lets mods add more). */
    boolean isActivatorRail(BlockState rail);

    Path configDir();

    Holder HOLDER = new Holder();

    static Platform get() {
        return HOLDER.platform;
    }

    static void set(Platform platform) {
        HOLDER.platform = platform;
    }

    final class Holder {
        private Platform platform;

        private Holder() {
        }
    }
}
