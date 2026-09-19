package com.slashrails.fabric;

import com.slashrails.net.RemoveRunPayload;
import com.slashrails.net.RunsPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Server-to-client messages (Fabric API payloads, MC 1.20.5+). */
final class FabricNet {

    private FabricNet() {
    }

    static void register() {
        PayloadTypeRegistry.playS2C().register(RunsPayload.TYPE, RunsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RemoveRunPayload.TYPE, RemoveRunPayload.CODEC);
    }

    /** Whether the player's client has SlashRails (it registers the channel). */
    static boolean canSend(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, RunsPayload.TYPE);
    }

    static void sendRuns(ServerPlayer player, RunsPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    static void sendRemove(ServerPlayer player, RemoveRunPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
}
