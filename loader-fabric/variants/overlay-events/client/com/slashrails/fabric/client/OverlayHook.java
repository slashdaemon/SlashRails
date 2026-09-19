package com.slashrails.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.slashrails.client.render.CurveOverlay;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/** Draws the tool preview / debug overlay after translucent terrain (Fabric API rendering v1, MC <= 1.21.8). */
final class OverlayHook {

    private OverlayHook() {
    }

    static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            if (ctx.consumers() != null) {
                CurveOverlay.render(new PoseStack(), ctx.consumers(), ctx.camera().getPosition());
                if (ctx.consumers() instanceof MultiBufferSource.BufferSource source) {
                    source.endBatch(RenderType.lines());
                }
            }
        });
    }
}
