package com.slashrails.net;

import com.slashrails.SlashRails;
import com.slashrails.run.SmoothRun;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Server -> client: smoothed runs in the player's current dimension. {@code replace} means "this is
 * the full set" (join, dimension change); otherwise the runs are additions. (MC 1.20.5+ payload.)
 */
public record RunsPayload(boolean replace, List<SmoothRun> runs) implements CustomPacketPayload {

    public static final Type<RunsPayload> TYPE = new Type<>(SlashRails.id("runs"));

    public static final StreamCodec<FriendlyByteBuf, RunsPayload> CODEC = StreamCodec.of(RunWire::writeRuns, RunWire::readRuns);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
