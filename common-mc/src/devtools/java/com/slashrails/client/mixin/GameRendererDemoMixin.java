package com.slashrails.client.mixin;

import com.slashrails.client.demo.DemoScenes;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dev-only demo recording hooks; inert unless {@code -Dslashrails.demo} is set. */
@Mixin(GameRenderer.class)
public abstract class GameRendererDemoMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void slashrails$beforeFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        if (DemoScenes.active()) DemoScenes.beforeFrame(Minecraft.getInstance());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void slashrails$afterFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        if (DemoScenes.active()) DemoScenes.afterFrame(Minecraft.getInstance());
    }
}
