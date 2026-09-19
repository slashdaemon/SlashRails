package com.slashrails.fabric.client;

import com.slashrails.client.ClientRuns;
import com.slashrails.net.RemoveRunPayload;
import com.slashrails.net.RunWire;
import com.slashrails.net.RunsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client receivers (Fabric API channels, MC 1.20.1). Decoded on the network thread, handled on the client thread. */
final class FabricClientNet {

    private FabricClientNet() {
    }

    static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RunsPayload.ID, (client, handler, buf, sender) -> {
            RunsPayload payload = RunWire.readRuns(buf);
            client.execute(() -> ClientRuns.handle(payload));
        });
        ClientPlayNetworking.registerGlobalReceiver(RemoveRunPayload.ID, (client, handler, buf, sender) -> {
            RemoveRunPayload payload = RunWire.readRemove(buf);
            client.execute(() -> ClientRuns.handle(payload));
        });
    }
}
