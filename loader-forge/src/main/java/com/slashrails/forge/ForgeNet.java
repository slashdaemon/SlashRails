package com.slashrails.forge;

import com.slashrails.SlashRails;
import com.slashrails.mixin.ServerGamePacketListenerAccessor;
import com.slashrails.net.RemoveRunPayload;
import com.slashrails.net.RunWire;
import com.slashrails.net.RunsPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * One channel, two server-to-client messages. The mod is required on both sides, so both ends must
 * speak the same protocol version; handlers run on the client thread.
 */
final class ForgeNet {

    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(SlashRails.id("main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ForgeNet() {
    }

    static void register() {
        CHANNEL.messageBuilder(RunsPayload.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((msg, buf) -> RunWire.writeRuns(buf, msg))
                .decoder(RunWire::readRuns)
                .consumerMainThread((msg, ctx) -> ClientPayloads.runs(msg))
                .add();
        CHANNEL.messageBuilder(RemoveRunPayload.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((msg, buf) -> RunWire.writeRemove(buf, msg))
                .decoder(RunWire::readRemove)
                .consumerMainThread((msg, ctx) -> ClientPayloads.remove(msg))
                .add();
    }

    static boolean isPresent(ServerPlayer player) {
        return CHANNEL.isRemotePresent(((ServerGamePacketListenerAccessor) player.connection).slashrails$connection());
    }

    static void send(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
