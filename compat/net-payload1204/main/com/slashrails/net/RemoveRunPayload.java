package com.slashrails.net;

import com.slashrails.SlashRails;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: a smoothed run was reverted to vanilla rails. (MC 1.20.2 - 1.20.4 payload.) */
public record RemoveRunPayload(int runId) implements CustomPacketPayload {

    public static final ResourceLocation ID = SlashRails.id("remove_run");

    @Override
    public void write(FriendlyByteBuf buf) {
        RunWire.writeRemove(buf, this);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
