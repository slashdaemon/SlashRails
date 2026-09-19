package com.slashrails.client.mixin;

import com.slashrails.client.CartPose;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** MC <= 1.21.1: {@code MinecartRenderer} asks the cart itself for its rail pose. See {@link CartPose}. */
@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartClientMixin {

    @Inject(method = "getPos", at = @At("HEAD"), cancellable = true)
    private void slashrails$getPos(double x, double y, double z, CallbackInfoReturnable<Vec3> cir) {
        Vec3 p = CartPose.pos((AbstractMinecart) (Object) this, x, y, z);
        if (p != null) cir.setReturnValue(p);
    }

    @Inject(method = "getPosOffs", at = @At("HEAD"), cancellable = true)
    private void slashrails$getPosOffs(double x, double y, double z, double offset, CallbackInfoReturnable<Vec3> cir) {
        Vec3 p = CartPose.offset((AbstractMinecart) (Object) this, x, y, z, offset);
        if (p != null) cir.setReturnValue(p);
    }
}
