package com.slashrails.fabric.client;

import com.slashrails.client.ClientRuns;
import com.slashrails.client.VisualTest;
import com.slashrails.client.render.CurveOverlay;
import com.slashrails.net.RemoveRunPayload;
import com.slashrails.net.RunsPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public final class SlashRailsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RunsPayload.TYPE, (payload, context) -> ClientRuns.handle(payload));
        ClientPlayNetworking.registerGlobalReceiver(RemoveRunPayload.TYPE, (payload, context) -> ClientRuns.handle(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientRuns.clear());
        ClientTickEvents.END_CLIENT_TICK.register(VisualTest::tick);

        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            if (ctx.consumers() != null) {
                CurveOverlay.render(new PoseStack(), ctx.consumers(), ctx.camera().getPosition());
                if (ctx.consumers() instanceof MultiBufferSource.BufferSource source) {
                    source.endBatch(RenderType.lines());
                }
            }
        });

        RailModels.register();
    }
}
