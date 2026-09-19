package com.slashrails.neoforge;

import com.slashrails.SlashRails;
import com.slashrails.net.RemoveRunPayload;
import com.slashrails.net.RunWire;
import com.slashrails.net.RunsPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

/** NeoForge 20.4: payload handlers, required on both sides; handled on the client thread. */
final class NeoForgeNet {

    private NeoForgeNet() {
    }

    static void register(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlerEvent event) -> {
            IPayloadRegistrar registrar = event.registrar(SlashRails.MOD_ID).versioned("1");
            registrar.play(RunsPayload.ID, RunWire::readRuns, handler -> handler.client(
                    (payload, ctx) -> ctx.workHandler().execute(() -> ClientPayloads.runs(payload))));
            registrar.play(RemoveRunPayload.ID, RunWire::readRemove, handler -> handler.client(
                    (payload, ctx) -> ctx.workHandler().execute(() -> ClientPayloads.remove(payload))));
        });
    }

    static void send(ServerPlayer player, CustomPacketPayload message) {
        PacketDistributor.PLAYER.with(player).send(message);
    }
}
