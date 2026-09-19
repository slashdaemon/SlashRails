package com.slashrails.net;

import com.slashrails.SlashRails;
import com.slashrails.run.SmoothRun;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server -> client: smoothed runs in the player's current dimension. {@code replace} means "this is
 * the full set" (join, dimension change); otherwise the runs are additions. (MC 1.20.1: a plain
 * message on its own channel; see {@link RunWire}.)
 */
public record RunsPayload(boolean replace, List<SmoothRun> runs) {

    public static final ResourceLocation ID = SlashRails.id("runs");
}
