package com.slashrails.fabric.client;

import com.slashrails.client.ClientRuns;
import com.slashrails.client.VisualTest;
import com.slashrails.net.RemoveRunPayload;
import com.slashrails.net.RunsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class SlashRailsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RunsPayload.TYPE, (payload, context) -> ClientRuns.handle(payload));
        ClientPlayNetworking.registerGlobalReceiver(RemoveRunPayload.TYPE, (payload, context) -> ClientRuns.handle(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientRuns.clear());
        ClientTickEvents.END_CLIENT_TICK.register(VisualTest::tick);

        OverlayHook.register();

        RailModels.register();
    }
}
