package com.slashtracks;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SlashTracks {

    public static final String MOD_ID = "slashtracks";
    public static final Logger LOG = LoggerFactory.getLogger("SlashTracks");

    private SlashTracks() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /** End of every server tick (both loaders). */
    public static void serverTick(net.minecraft.server.MinecraftServer server) {
        com.slashtracks.run.RunService.tick(server);
        com.slashtracks.command.RideProbe.tick(server);
        com.slashtracks.command.SelfTest.tick(server);
    }

    /** Called by each loader entrypoint once the {@link com.slashtracks.platform.Platform} is set. */
    public static void init() {
        Config.load();
        LOG.info("SlashTracks initialised (max run {} rails, op-only tool: {})", Config.maxRunLength, Config.opOnlyTool);
    }
}
