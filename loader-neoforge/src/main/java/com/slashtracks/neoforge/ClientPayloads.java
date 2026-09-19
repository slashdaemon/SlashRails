package com.slashtracks.neoforge;

import com.slashtracks.client.ClientRuns;
import com.slashtracks.net.RemoveRunPayload;
import com.slashtracks.net.RunsPayload;

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
