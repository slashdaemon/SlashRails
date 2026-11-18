package com.slashrails.client.mixin;

import com.slashrails.client.CartPose;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After a client-side cart tick (the step towards the server's position), show carts that run their
 * own rail physics on the smoothed curve. See {@link CartPose#presentOnCurve}.
 */
@Mixin(AbstractMinecart.class)
public abstract class CartCurvePresentMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void slashrails$presentOnCurve(CallbackInfo ci) {
        AbstractMinecart cart = (AbstractMinecart) (Object) this;
        if (cart.level().isClientSide()) CartPose.presentOnCurve(cart);
    }
}
