package com.slashrails.net;

import com.slashrails.SlashRails;
import com.slashrails.run.SmoothRun;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server -> client: smoothed runs in the player's current dimension. {@code replace} means "this is
 * the full set" (join, dimension change); otherwise the runs are additions. (MC 1.20.2 - 1.20.4
 * payload: written by the message itself, keyed by id.)
 */
public record RunsPayload(boolean replace, List<SmoothRun> runs) implements CustomPacketPayload {

    public static final ResourceLocation ID = SlashRails.id("runs");

    @Override
    public void write(FriendlyByteBuf buf) {
        RunWire.writeRuns(buf, this);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
