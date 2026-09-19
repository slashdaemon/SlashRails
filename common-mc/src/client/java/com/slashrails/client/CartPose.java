package com.slashrails.client;

import com.slashrails.core.Pt;
import com.slashrails.core.SmoothCurve;
import com.slashrails.ride.CartRide;
import com.slashrails.run.SmoothRun;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Where the cart model sits on a smoothed run. The minecart renderer places and orients the model
 * from {@code getPos} (the rail point under the cart) and {@code getPosOffs(+-0.3)} (points just ahead
 * and behind); the version-specific client mixins answer those from here, so the model glides along
 * the curve with a continuous heading.
 */
public final class CartPose {

    private static final double SEARCH_WINDOW = 2.5;
    private static final double MAX_OFFSET = 1.25;

    private CartPose() {
    }

    /** The curve point for {@code getPos}, or null to let vanilla answer. */
    @Nullable
    public static Vec3 pos(AbstractMinecart cart, double x, double y, double z) {
        return offset(cart, x, y, z, 0);
    }

    /** The curve point for {@code getPosOffs}, or null to let vanilla answer. */
    @Nullable
    public static Vec3 offset(AbstractMinecart cart, double x, double y, double z, double offset) {
        Double s = arcLength(cart, x, y, z);
        if (s == null) return null;
        SmoothRun run = ClientRuns.byId(((CartRide) cart).slashrails$runId());
        Pt p = run.curve().pointAt(s + offset);
        return new Vec3(p.x(), p.y(), p.z());
    }

    /** Arc length of (x, y, z) on the smoothed run under the cart, or null to fall back to vanilla. */
    @Nullable
    private static Double arcLength(AbstractMinecart cart, double x, double y, double z) {
        if (!cart.level().isClientSide || ClientRuns.isEmpty()) return null;
        CartRide ride = (CartRide) cart;
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
