package com.slashrails.net;

import com.slashrails.SlashRails;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: a smoothed run was reverted to vanilla rails. (MC 1.20.5+ payload.) */
public record RemoveRunPayload(int runId) implements CustomPacketPayload {

    public static final Type<RemoveRunPayload> TYPE = new Type<>(SlashRails.id("remove_run"));

    public static final StreamCodec<FriendlyByteBuf, RemoveRunPayload> CODEC = StreamCodec.of(RunWire::writeRemove, RunWire::readRemove);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
