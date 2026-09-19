package com.slashrails.neoforge.client;

import com.slashrails.client.render.CurveLines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Draws the tool preview / debug overlay after particles ({@DOC}). */
final class OverlayHook {

    private OverlayHook() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, event -> {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
            MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            CurveLines.render(event.getPoseStack(), buffers, Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
            buffers.endBatch(RenderType.lines());
        });
    }
}
