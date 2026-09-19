package com.slashrails;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SlashRails {

    public static final String MOD_ID = "slashrails";
    public static final Logger LOG = LoggerFactory.getLogger("SlashRails");

    private SlashRails() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /** End of every server tick (both loaders). */
    public static void serverTick(net.minecraft.server.MinecraftServer server) {
        com.slashrails.run.RunService.tick(server);
        com.slashrails.command.RideProbe.tick(server);
        com.slashrails.command.SelfTest.tick(server);
    }

    /** Called by each loader entrypoint once the {@link com.slashrails.platform.Platform} is set. */
    public static void init() {
        Config.load();
        LOG.info("SlashRails initialised (max run {} rails, op-only tool: {})", Config.maxRunLength, Config.opOnlyTool);
    }
}
