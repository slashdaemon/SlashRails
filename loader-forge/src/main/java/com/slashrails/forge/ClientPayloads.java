package com.slashrails.forge;

import com.slashrails.net.RemoveRunPayload;
import com.slashrails.net.RunsPayload;

/**
 * Client-bound message handlers, kept in their own class so a dedicated server never loads the
 * client code they call (handlers only ever run on the client).
 */
final class ClientPayloads {

    private ClientPayloads() {
    }

    static void runs(RunsPayload payload) {
        com.slashrails.client.ClientRuns.handle(payload);
    }

    static void remove(RemoveRunPayload payload) {
        com.slashrails.client.ClientRuns.handle(payload);
    }
}
