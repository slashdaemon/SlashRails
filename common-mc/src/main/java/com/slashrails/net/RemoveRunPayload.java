package com.slashrails.net;

import com.slashrails.SlashRails;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → client: a smoothed run was reverted to vanilla rails. */
public record RemoveRunPayload(int id) implements CustomPacketPayload {

    public static final Type<RemoveRunPayload> TYPE = new Type<>(SlashRails.id("remove_run"));

    public static final StreamCodec<FriendlyByteBuf, RemoveRunPayload> CODEC =
            ByteBufCodecs.VAR_INT.<FriendlyByteBuf>cast().map(RemoveRunPayload::new, RemoveRunPayload::id);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
