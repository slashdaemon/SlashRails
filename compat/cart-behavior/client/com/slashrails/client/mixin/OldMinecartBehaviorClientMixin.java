package com.slashrails.client.mixin;

import com.slashrails.client.CartPose;
import com.slashrails.mixin.MinecartBehaviorAccessor;
import net.minecraft.world.entity.vehicle.OldMinecartBehavior;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** MC 1.21.2+: the minecart renderer asks the cart's {@code OldMinecartBehavior} for its rail pose. See {@link CartPose}. */
@Mixin(OldMinecartBehavior.class)
public abstract class OldMinecartBehaviorClientMixin {

    @Inject(method = "getPos", at = @At("HEAD"), cancellable = true)
    private void slashrails$getPos(double x, double y, double z, CallbackInfoReturnable<Vec3> cir) {
        Vec3 p = CartPose.pos(((MinecartBehaviorAccessor) this).slashrails$minecart(), x, y, z);
        if (p != null) cir.setReturnValue(p);
    }

    @Inject(method = "getPosOffs", at = @At("HEAD"), cancellable = true)
    private void slashrails$getPosOffs(double x, double y, double z, double offset, CallbackInfoReturnable<Vec3> cir) {
        Vec3 p = CartPose.offset(((MinecartBehaviorAccessor) this).slashrails$minecart(), x, y, z, offset);
        if (p != null) cir.setReturnValue(p);
    }
}
