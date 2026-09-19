package com.slashrails.fabric.client;

import com.slashrails.client.render.CurveLines;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;

/** Draws the tool preview / debug overlay after translucent terrain (Fabric API rendering v1 23+, MC 26.1+). */
final class OverlayHook {

    private OverlayHook() {
    }

    static void register() {
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(ctx -> CurveLines.render(ctx.poseStack(), ctx.bufferSource(),
                Minecraft.getInstance().gameRenderer.getMainCamera().position()));
    }
}
