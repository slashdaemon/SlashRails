package com.slashrails.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.slashrails.ride.CurveRide;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * While a cart is on a smoothed run, its server tick moves it along the curve instead of along the
 * vanilla rail line. Everywhere else the vanilla code runs untouched. (MC ≤ 1.21.1: the rail physics
 * are called from {@code AbstractMinecart#tick}.)
 */
@Mixin(AbstractMinecart.class)
public abstract class CartTickMixin {

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;moveAlongTrack(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void slashrails$moveAlongTrack(AbstractMinecart self, BlockPos pos, BlockState state, Operation<Void> original) {
        if (!CurveRide.stepOnRail(self, pos)) {
            original.call(self, pos, state);
        }
    }

    /** A curve can cut across the corner of a block with no rail in it; keep riding instead of derailing. */
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;comeOffTrack()V"))
    private void slashrails$comeOffTrack(AbstractMinecart self, Operation<Void> original) {
        if (!CurveRide.stepOffRail(self)) {
            original.call(self);
        }
    }
}
