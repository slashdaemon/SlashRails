package com.slashrails.net;

import com.slashrails.SlashRails;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: a smoothed run was reverted to vanilla rails. (MC 1.20.1 channel message.) */
public record RemoveRunPayload(int id) {

    public static final ResourceLocation ID = SlashRails.id("remove_run");
}
