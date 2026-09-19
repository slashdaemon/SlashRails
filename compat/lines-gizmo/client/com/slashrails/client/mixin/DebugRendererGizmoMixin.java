package com.slashrails.client.mixin;

import com.slashrails.client.render.CurveOverlay;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MC 1.21.11+: world-space debug lines are gizmos, emitted once per frame while vanilla's per-frame
 * gizmo collector is active. The overlay's curves are added right after vanilla's own debug
 * renderers, the same on every loader.
 */
@Mixin(DebugRenderer.class)
public abstract class DebugRendererGizmoMixin {

    @Inject(method = "emitGizmos", at = @At("TAIL"))
    private void slashrails$overlay(Frustum frustum, double camX, double camY, double camZ, float partialTick, CallbackInfo ci) {
        CurveOverlay.collect((curve, r, g, b) -> {
            int argb = 0xFF000000 | (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
            CurveOverlay.segments(curve, (x0, y0, z0, x1, y1, z1) ->
                    Gizmos.line(new Vec3(x0, y0, z0), new Vec3(x1, y1, z1), argb, 2f));
        });
    }
}
