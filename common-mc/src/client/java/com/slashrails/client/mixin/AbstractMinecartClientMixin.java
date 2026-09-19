package com.slashrails.client.mixin;

import com.slashrails.client.ClientRuns;
import com.slashrails.core.Pt;
import com.slashrails.core.SmoothCurve;
import com.slashrails.ride.CartRide;
import com.slashrails.run.SmoothRun;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code MinecartRenderer} places and orients the cart model from {@code getPos} (the rail point under
 * the cart) and {@code getPosOffs(±0.3)} (points just ahead and behind). On a smoothed run those
 * answer from the curve, so the model glides along it with a continuous heading.
 */
@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartClientMixin {

    @Unique
    private static final double SEARCH_WINDOW = 2.5;
    @Unique
    private static final double MAX_OFFSET = 1.25;

    @Inject(method = "getPos", at = @At("HEAD"), cancellable = true)
    private void slashrails$getPos(double x, double y, double z, CallbackInfoReturnable<Vec3> cir) {
        Double s = slashrails$arcLength(x, y, z);
        if (s != null) {
            SmoothRun run = ClientRuns.byId(((CartRide) this).slashrails$runId());
            Pt p = run.curve().pointAt(s);
            cir.setReturnValue(new Vec3(p.x(), p.y(), p.z()));
        }
    }

    @Inject(method = "getPosOffs", at = @At("HEAD"), cancellable = true)
    private void slashrails$getPosOffs(double x, double y, double z, double offset, CallbackInfoReturnable<Vec3> cir) {
        Double s = slashrails$arcLength(x, y, z);
        if (s != null) {
            SmoothRun run = ClientRuns.byId(((CartRide) this).slashrails$runId());
            Pt p = run.curve().pointAt(s + offset);
            cir.setReturnValue(new Vec3(p.x(), p.y(), p.z()));
        }
    }

    /** Arc length of (x, y, z) on the smoothed run under the cart, or null to fall back to vanilla. */
    @Unique
    @Nullable
    private Double slashrails$arcLength(double x, double y, double z) {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        if (!self.level().isClientSide || ClientRuns.isEmpty()) return null;
        CartRide ride = (CartRide) this;

        int bx = Mth.floor(x), by = Mth.floor(y), bz = Mth.floor(z);
        ClientRuns.Slot slot = ClientRuns.at(BlockPos.asLong(bx, by, bz));
        if (slot == null) slot = ClientRuns.at(BlockPos.asLong(bx, by - 1, bz));
        SmoothRun run = slot != null ? slot.run() : ClientRuns.byId(ride.slashrails$runId());
        if (run == null) {
            if (ride.slashrails$runId() != 0) ride.slashrails$clear();
            return null;
        }
        double hint = run.id() == ride.slashrails$runId() ? ride.slashrails$s() : Double.NaN;
        SmoothCurve curve = run.curve();
        double s = curve.project(x, y, z, hint, Double.isNaN(hint) ? 0 : SEARCH_WINDOW);
        Pt p = curve.pointAt(s);
        if (Math.hypot(p.x() - x, p.z() - z) > MAX_OFFSET) {
            ride.slashrails$clear();
            return null;
        }
        // Past an open end the cart is back on vanilla rail.
        if (!curve.closed() && slot == null && (s <= 0 || s >= curve.length())) {
            ride.slashrails$clear();
            return null;
        }
        ride.slashrails$set(run.id(), s, ride.slashrails$sign());
        return s;
    }
}
