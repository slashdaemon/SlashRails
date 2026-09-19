package com.slashrails.fabric.client;

import com.slashrails.client.render.CurveLines;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/** Draws the tool preview / debug overlay after translucent terrain (Fabric API rendering v1, MC <= 1.20.4: the camera rotation is in the context's pose stack). */
final class OverlayHook {

    private OverlayHook() {
    }

    static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            if (ctx.consumers() != null) {
                CurveLines.render(ctx.matrixStack(), ctx.consumers(), ctx.camera().getPosition());
                if (ctx.consumers() instanceof MultiBufferSource.BufferSource source) {
                    source.endBatch(RenderType.lines());
                }
            }
        });
    }
}
