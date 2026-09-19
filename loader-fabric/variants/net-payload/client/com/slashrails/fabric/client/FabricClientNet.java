package com.slashrails.fabric.client;

import com.slashrails.client.ClientRuns;
import com.slashrails.net.RemoveRunPayload;
import com.slashrails.net.RunsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client receivers (Fabric API payloads, MC 1.20.5+). */
final class FabricClientNet {

    private FabricClientNet() {
    }

    static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RunsPayload.TYPE, (payload, context) -> ClientRuns.handle(payload));
        ClientPlayNetworking.registerGlobalReceiver(RemoveRunPayload.TYPE, (payload, context) -> ClientRuns.handle(payload));
    }
}
