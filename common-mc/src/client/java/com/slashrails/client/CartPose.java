package com.slashrails.client;

import com.slashrails.Config;
import com.slashrails.core.Pt;
import com.slashrails.core.SmoothCurve;
import com.slashrails.ride.CartRide;
import com.slashrails.ride.Carts;
import com.slashrails.run.SmoothRun;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
    /** Bodies up to this long (a vanilla minecart is 0.98) keep the tangent pose. */
    private static final double BOGIE_MIN_LENGTH = 1.2;

    private CartPose() {
    }

    /** The curve point for {@code getPos}, or null to let vanilla answer. */
    @Nullable
    public static Vec3 pos(AbstractMinecart cart, double x, double y, double z) {
        return offset(cart, x, y, z, 0);
    }

    /**
     * The curve point for {@code getPosOffs}, or null to let vanilla answer.
     *
     * <p>A minecart-sized body is centred on the curve and faces along its tangent. A longer modded
     * cart (a {@link Config#cartLength listed} body length L) sits like a two-bogie vehicle instead:
     * bogies on the curve at s +- b/2, the body along the chord between them and centred on its
     * midpoint. With b = L / sqrt 2 the middle (inside the bend) and the ends (outside it) are off the
     * curve by the same amount, L^2 / (16 R), half the overhang of a tangent body.
     */
    @Nullable
    public static Vec3 offset(AbstractMinecart cart, double x, double y, double z, double offset) {
        Double s = arcLength(cart, x, y, z);
        if (s == null) return null;
        SmoothCurve curve = ClientRuns.byId(((CartRide) cart).slashrails$runId()).curve();
        double length = Config.cartLength(BuiltInRegistries.ENTITY_TYPE.getKey(cart.getType()).toString());
        if (length <= BOGIE_MIN_LENGTH) {
            Pt p = curve.pointAt(s + offset);
            return new Vec3(p.x(), p.y(), p.z());
        }
        double half = length / Math.sqrt(2) / 2;
        Pt front = extended(curve, s + half), rear = extended(curve, s - half);
        double dx = front.x() - rear.x(), dy = front.y() - rear.y(), dz = front.z() - rear.z();
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double k = d > 1e-9 ? offset / d : 0;
        return new Vec3((front.x() + rear.x()) / 2 + dx * k, (front.y() + rear.y()) / 2 + dy * k,
                (front.z() + rear.z()) / 2 + dz * k);
    }

    /** The curve point at {@code s}, carried on along the end tangent past an open curve's ends. */
    private static Pt extended(SmoothCurve curve, double s) {
        if (curve.closed() || (s >= 0 && s <= curve.length())) return curve.pointAt(s);
        double end = s < 0 ? 0 : curve.length();
        return curve.pointAt(end).add(curve.tangentAt(end).scale(s - end));
    }

    /**
     * Client tick: a cart with its own rail physics (a modded locomotive) moves on the vanilla line on
     * the server, which keeps its engine, couplings and derail checks on real rails. Show it on the
     * curve instead: put the client-side entity at the curve point level with it, so its model, hitbox
     * and passengers sit on the drawn track. Progress along the track still follows the server.
     */
    public static void presentOnCurve(AbstractMinecart cart) {
        if (!Carts.ownsRailMovement(cart)) return; // the server already moves vanilla carts along the curve
        Vec3 at = cart.position();
        Double s = arcLength(cart, at.x, at.y, at.z);
        if (s == null) return;
        Pt p = ClientRuns.byId(((CartRide) cart).slashrails$runId()).curve().pointAt(s);
        cart.setPos(p.x(), at.y, p.z());
    }

    /** Arc length of (x, y, z) on the smoothed run under the cart, or null to fall back to vanilla. */
    @Nullable
    private static Double arcLength(AbstractMinecart cart, double x, double y, double z) {
        if (!cart.level().isClientSide() || ClientRuns.isEmpty()) return null;
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
