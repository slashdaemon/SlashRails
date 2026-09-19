package com.slashrails.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.slashrails.ride.CurveRide;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.OldMinecartBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * While a cart is on a smoothed run, its server tick moves it along the curve instead of along the
 * vanilla rail line. Everywhere else the vanilla code runs untouched. (MC 1.21.2+: the rail physics
 * live in {@code OldMinecartBehavior}. Carts under the experimental {@code NewMinecartBehavior} are
 * left to vanilla.)
 */
@Mixin(OldMinecartBehavior.class)
public abstract class CartTickMixin {

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/vehicle/OldMinecartBehavior;moveAlongTrack(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void slashrails$moveAlongTrack(OldMinecartBehavior self, ServerLevel level, Operation<Void> original) {
        AbstractMinecart cart = ((MinecartBehaviorAccessor) self).slashrails$minecart();
        if (!CurveRide.stepOnRail(cart, cart.getCurrentBlockPosOrRailBelow())) {
            original.call(self, level);
        }
    }

    /** A curve can cut across the corner of a block with no rail in it; keep riding instead of derailing. */
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;comeOffTrack(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void slashrails$comeOffTrack(AbstractMinecart cart, ServerLevel level, Operation<Void> original) {
        if (!CurveRide.stepOffRail(cart)) {
            original.call(cart, level);
        }
    }
}
