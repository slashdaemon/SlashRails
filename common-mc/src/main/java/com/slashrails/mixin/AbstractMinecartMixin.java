package com.slashrails.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.slashrails.ride.CartRide;
import com.slashrails.ride.CurveRide;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * While a cart is on a smoothed run, its server tick moves it along the curve instead of along the
 * vanilla rail line. Everywhere else the vanilla code runs untouched.
 */
@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin implements CartRide {

    @Unique
    private int slashrails$runId;
    @Unique
    private double slashrails$s = Double.NaN;
    @Unique
    private int slashrails$sign = 1;

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

    @Override
    public int slashrails$runId() {
        return slashrails$runId;
    }

    @Override
    public double slashrails$s() {
        return slashrails$s;
    }

    @Override
    public int slashrails$sign() {
        return slashrails$sign;
    }

    @Override
    public void slashrails$set(int runId, double s, int sign) {
        slashrails$runId = runId;
        slashrails$s = s;
        slashrails$sign = sign;
    }
}
