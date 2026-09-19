package com.slashrails.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.slashrails.SlashRails;
import com.slashrails.client.ClientRuns;
import com.slashrails.client.VisualTest;
import com.slashrails.client.render.CurveOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;


@Mod(value = SlashRails.MOD_ID, dist = Dist.CLIENT)
public final class SlashRailsNeoForgeClient {

    public SlashRailsNeoForgeClient(IEventBus modBus) {
        modBus.addListener(ModelEvent.ModifyBakingResult.class, RailModels::wrap);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> ClientRuns.clear());
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, SlashRailsNeoForgeClient::renderOverlay);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> VisualTest.tick(Minecraft.getInstance()));
    }

    private static void renderOverlay(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        CurveOverlay.render(new PoseStack(), buffers, event.getCamera().getPosition());
        buffers.endBatch(RenderType.lines());
    }
}
