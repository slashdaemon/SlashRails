package com.slashtracks.fabric.client;

import com.slashtracks.client.ClientRuns;
import com.slashtracks.client.render.CurveOverlay;
import com.slashtracks.net.RemoveRunPayload;
import com.slashtracks.net.RunsPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class SlashTracksFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RunsPayload.TYPE, (payload, context) -> ClientRuns.handle(payload));
        ClientPlayNetworking.registerGlobalReceiver(RemoveRunPayload.TYPE, (payload, context) -> ClientRuns.handle(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientRuns.clear());

        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            if (ctx.consumers() != null) {
                CurveOverlay.render(new PoseStack(), ctx.consumers(), ctx.camera().getPosition());
                if (ctx.consumers() instanceof MultiBufferSource.BufferSource source) {
                    source.endBatch(RenderType.lines());
                }
            }
        });

        // Wrap every rail block-state model: smoothed rails draw their slice of the curve instead.
        ModelLoadingPlugin.register(plugin -> plugin.modifyModelAfterBake().register((model, context) -> {
            if (model == null || context.topLevelId() == null) return model;
            Block block = BuiltInRegistries.BLOCK.get(context.topLevelId().id());
            if (!(block instanceof BaseRailBlock)) return model;
            return new SmoothRailModel(model, block == Blocks.RAIL);
        }));
    }
}
