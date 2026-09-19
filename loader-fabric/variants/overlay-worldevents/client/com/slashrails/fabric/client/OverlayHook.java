package com.slashrails.fabric.client;

import com.slashrails.client.render.CurveOverlay;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;

/** Draws the tool preview / debug overlay before translucent terrain (Fabric API rendering v1 16.2+, MC 1.21.11+). */
final class OverlayHook {

    private OverlayHook() {
    }

    static void register() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(ctx -> CurveOverlay.render(ctx.matrices(), ctx.consumers(),
                Minecraft.getInstance().gameRenderer.getMainCamera().position()));
    }
}
