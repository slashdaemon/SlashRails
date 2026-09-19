package com.slashrails.neoforge;

import com.slashrails.client.ClientRuns;
import com.slashrails.net.RemoveRunPayload;
import com.slashrails.net.RunsPayload;

/**
 * Client-bound payload handlers, kept in their own class so a dedicated server never loads the
 * client code they call (handlers are only invoked on the client).
 */
final class ClientPayloads {

    private ClientPayloads() {
    }

    static void runs(RunsPayload payload) {
        ClientRuns.handle(payload);
    }

    static void remove(RemoveRunPayload payload) {
        ClientRuns.handle(payload);
    }
}
