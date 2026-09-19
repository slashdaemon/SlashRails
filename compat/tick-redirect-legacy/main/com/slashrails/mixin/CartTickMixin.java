package com.slashrails.mixin;

import com.slashrails.ride.CurveRide;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * While a cart is on a smoothed run, its server tick moves it along the curve instead of along the
 * vanilla rail line. Everywhere else the vanilla code runs untouched. (Forge 1.20.1 ships no
 * MixinExtras, so this uses plain redirects where the other loaders wrap the calls.)
 */
@Mixin(AbstractMinecart.class)
public abstract class CartTickMixin {

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;moveAlongTrack(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void slashrails$moveAlongTrack(AbstractMinecart self, BlockPos pos, BlockState state) {
        if (!CurveRide.stepOnRail(self, pos)) {
            ((CartTrackInvoker) self).slashrails$moveAlongTrack(pos, state);
        }
    }

    /** A curve can cut across the corner of a block with no rail in it; keep riding instead of derailing. */
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;comeOffTrack()V"))
    private void slashrails$comeOffTrack(AbstractMinecart self) {
        if (!CurveRide.stepOffRail(self)) {
            ((CartTrackInvoker) self).slashrails$comeOffTrack();
        }
    }
}
