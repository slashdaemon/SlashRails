package com.slashrails.fabric.client;

import com.slashrails.client.ClientRuns;
import com.slashrails.client.VisualTest;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class SlashRailsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricClientNet.register();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientRuns.clear());
        ClientTickEvents.END_CLIENT_TICK.register(VisualTest::tick);

        OverlayHook.register();

        RailModels.register();
    }
}
