package com.slashrails.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.slashrails.client.render.CurveLines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;

/** Draws the tool preview / debug overlay after particles. */
final class OverlayHook {

    private OverlayHook() {
    }

    static void register() {
        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
            MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            CurveLines.render(new PoseStack(), buffers, event.getCamera().getPosition());
            buffers.endBatch(RenderType.lines());
        });
    }
}
