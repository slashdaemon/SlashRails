package com.slashrails.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.slashrails.client.render.CurveOverlay;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the tool preview / debug overlay right after vanilla's debug renderers, which run every frame
 * in the main pass with the camera-relative pose and the shared buffer source (MC 1.21.9 - 1.21.10).
 */
@Mixin(DebugRenderer.class)
public abstract class DebugRendererOverlayMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void slashrails$overlay(PoseStack poseStack, Frustum frustum, MultiBufferSource.BufferSource buffers,
                                   double camX, double camY, double camZ, boolean translucent, CallbackInfo ci) {
        if (translucent) return;
        CurveOverlay.render(poseStack, buffers, new Vec3(camX, camY, camZ));
        buffers.endBatch(RenderType.lines());
    }
}
