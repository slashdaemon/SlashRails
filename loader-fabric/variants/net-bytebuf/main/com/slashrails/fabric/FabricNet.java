package com.slashrails.fabric;

import com.slashrails.net.RemoveRunPayload;
import com.slashrails.net.RunWire;
import com.slashrails.net.RunsPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Server-to-client messages (Fabric API channels, MC 1.20.1). */
final class FabricNet {

    private FabricNet() {
    }

    static void register() {
    }

    /** Whether the player's client has SlashRails (it registers the channel). */
    static boolean canSend(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, RunsPayload.ID);
    }

    static void sendRuns(ServerPlayer player, RunsPayload payload) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        RunWire.writeRuns(buf, payload);
        ServerPlayNetworking.send(player, RunsPayload.ID, buf);
    }

    static void sendRemove(ServerPlayer player, RemoveRunPayload payload) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        RunWire.writeRemove(buf, payload);
        ServerPlayNetworking.send(player, RemoveRunPayload.ID, buf);
    }
}
