package com.slashrails.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
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
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.AfterParticles.class, event -> {
            MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            CurveLines.render(new PoseStack(), buffers, Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
            buffers.endBatch(RenderType.lines());
        });
    }
}
