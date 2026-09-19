package com.slashrails.ride;

import com.slashrails.core.Pt;
import com.slashrails.core.SmoothCurve;
import com.slashrails.platform.Platform;
import com.slashrails.run.SmoothRun;
import com.slashrails.run.SmoothRunRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

/**
 * Moves a vanilla minecart along a smoothed run's curve for one server tick.
 *
 * <p>Mirrors vanilla {@code moveAlongTrack}: rider push, unpowered-rail braking, per-tick movement
 * capped at the cart's max rail speed (capped on magnitude, so there is no diagonal speed surge),
 * natural slowdown, powered-rail boost and kick-start, and the rail-pass hooks — but position and
 * velocity follow the curve's tangent instead of snapping to each block's rail line. The rail that
 * owns the cart's arc length supplies powered/detector/activator behaviour, so every rail in the
 * run is visited in order.
 */
public final class CurveRide {

    /** How far either side of the last known arc length to search when re-projecting. */
    private static final double SEARCH_WINDOW = 2.5;
    /** A cart further than this from the curve is not riding it. */
    private static final double MAX_OFFSET = 1.25;

    private CurveRide() {
    }

    /** The cart is over a rail block. Returns true if the curve handled this tick. */
    public static boolean stepOnRail(AbstractMinecart cart, BlockPos railPos) {
        if (!(cart.level() instanceof ServerLevel level)) return false;
        CartRide ride = (CartRide) cart;
        SmoothRunRegistry registry = SmoothRunRegistry.get(level);
        if (registry.isEmpty()) {
            if (ride.slashrails$runId() != 0) ride.slashrails$clear();
            return false;
        }
        SmoothRun here = registry.runAt(railPos);
        SmoothRun run = here;
        if (run == null && ride.slashrails$runId() != 0) {
            run = registry.byId(ride.slashrails$runId());
        }
        if (run == null) {
            ride.slashrails$clear();
            return false;
        }
        if (run.id() != ride.slashrails$runId()) {
            ride.slashrails$set(run.id(), Double.NaN, ride.slashrails$sign());
        }
        return step(cart, level, run);
    }

    /** The cart is over a block with no rail. Returns true if it is still riding a curve. */
    public static boolean stepOffRail(AbstractMinecart cart) {
        if (!(cart.level() instanceof ServerLevel level)) return false;
        CartRide ride = (CartRide) cart;
        if (ride.slashrails$runId() == 0) return false;
        SmoothRun run = SmoothRunRegistry.get(level).byId(ride.slashrails$runId());
        if (run == null || !step(cart, level, run)) {
            ride.slashrails$clear();
            return false;
        }
        Carts.setOnRails(cart, true);
        return true;
    }

    private static boolean step(AbstractMinecart cart, ServerLevel level, SmoothRun run) {
        CartRide ride = (CartRide) cart;
        SmoothCurve curve = run.curve();
        Vec3 pos = cart.position();
        double hint = ride.slashrails$s();
        double s = curve.project(pos.x, pos.y, pos.z, hint, Double.isNaN(hint) ? 0 : SEARCH_WINDOW);
        Pt on = curve.pointAt(s);
        if (Math.hypot(on.x() - pos.x, on.z() - pos.z) > MAX_OFFSET) {
            ride.slashrails$clear();
            return false;
        }

        cart.resetFallDistance();
        Pt t = curve.tangentAt(s);
        Vec3 v = cart.getDeltaMovement();
        double along = v.x * t.x() + v.z * t.z();
        int sign = along > 1e-9 ? 1 : along < -1e-9 ? -1 : ride.slashrails$sign();
        double speed = Math.min(2.0, Math.abs(along));

        BlockPos railPos = run.rail(curve.railIndexAt(s));
        BlockState rail = level.getBlockState(railPos);
        boolean powered = false, braking = false;
        if (rail.getBlock() instanceof PoweredRailBlock && !Platform.get().isActivatorRail(rail)) {
            powered = rail.getValue(PoweredRailBlock.POWERED);
            braking = !powered;
        }

        // Rider push: a player walking forward nudges a (nearly) stopped cart.
        Entity rider = cart.getFirstPassenger();
        if (rider instanceof Player) {
            Vec3 pv = rider.getDeltaMovement();
            if (pv.horizontalDistanceSqr() > 1.0E-4 && speed * speed < 0.01) {
                double pushed = sign * speed + (pv.x * t.x() + pv.z * t.z()) * 0.1;
                sign = pushed >= 0 ? 1 : -1;
                speed = Math.abs(pushed);
                braking = false;
            }
        }
        if (braking) {
            speed = speed < 0.03 ? 0 : speed * 0.5;
        }

        // Move along the curve. Vanilla scales rider-carrying carts to 3/4 per tick and caps each tick.
        double factor = cart.isVehicle() ? 0.75 : 1.0;
        double dist = Math.min(speed * factor, Platform.get().maxRailSpeed(cart));
        double s2 = s + sign * dist;
        boolean leaving = !curve.closed() && (s2 < 0 || s2 > curve.length());
        Pt target;
        Pt t2;
        if (leaving) {
            // Carry on past the open end along the vanilla rail's axis; vanilla takes over next tick.
            double end = s2 < 0 ? 0 : curve.length();
            Pt endPoint = curve.pointAt(end);
            Pt endTan = curve.tangentAt(end);
            double over = Math.abs(s2 - end);
            target = endPoint.add(endTan.scale(sign * over));
            t2 = endTan;
        } else {
            target = curve.pointAt(s2);
            t2 = curve.tangentAt(s2);
        }
        // Move with collision like vanilla does. A cart's box can already overlap the blocks beside a
        // corner (vanilla carts do too) and collision may nudge it sideways; that is not "blocked".
        // Blocked means the cart failed to make real progress along the curve.
        cart.move(MoverType.SELF, new Vec3(target.x() - pos.x, target.y() - pos.y, target.z() - pos.z));
        Vec3 after = cart.position();
        double progress = leaving ? dist
                : sign * curve.delta(s, curve.project(after.x, after.y, after.z, s, SEARCH_WINDOW));
        if (dist > 1e-3 && progress < dist * 0.5) {
            speed = 0; // ran into something
            s2 = curve.project(after.x, after.y, after.z, s, SEARCH_WINDOW);
            leaving = false;
            t2 = curve.tangentAt(s2);
            Pt back = curve.pointAt(s2);
            cart.setPos(back.x(), back.y(), back.z());
        } else {
            cart.setPos(target.x(), target.y(), target.z()); // snap onto the curve, as vanilla snaps onto its rail line
        }

        // Velocity along the new tangent, then vanilla's slowdown (virtual: furnace carts add push).
        cart.setDeltaMovement(t2.x() * sign * speed, 0, t2.z() * sign * speed);
        Carts.applyNaturalSlowdown(cart);
        Vec3 slowed = cart.getDeltaMovement();
        double a2 = slowed.x * t2.x() + slowed.z * t2.z();
        if (Math.abs(a2) > 1e-9) sign = a2 > 0 ? 1 : -1;
        speed = Math.abs(a2);

        if (!leaving) {
            BlockPos nowPos = run.rail(curve.railIndexAt(s2));
            BlockState now = level.getBlockState(nowPos);
            Platform.get().onMinecartPass(now, level, nowPos, cart);
            if (now.getBlock() instanceof PoweredRailBlock) {
                boolean railPowered = now.getValue(PoweredRailBlock.POWERED);
                if (Platform.get().isActivatorRail(now)) {
                    cart.activateMinecart(nowPos.getX(), nowPos.getY(), nowPos.getZ(), railPowered);
                } else if (railPowered) {
                    if (speed > 0.01) {
                        speed += 0.06;
                    } else {
                        int kick = kickStart(level, nowPos, now, t2);
                        if (kick != 0) {
                            sign = kick;
                            speed = 0.02;
                        }
                    }
                }
            }
        }
        cart.setDeltaMovement(t2.x() * sign * speed, 0, t2.z() * sign * speed);

        if (leaving) ride.slashrails$set(0, Double.NaN, sign);
        else ride.slashrails$set(run.id(), curve.normalize(s2), sign);
        return true;
    }

    /** Vanilla: a stopped cart on a powered rail is pushed away from a solid block at either end. */
    private static int kickStart(ServerLevel level, BlockPos pos, BlockState rail, Pt tangent) {
        RailShape shape = rail.getValue(PoweredRailBlock.SHAPE);
        int ax, az;
        if (shape == RailShape.EAST_WEST) {
            ax = 1;
            az = 0;
        } else if (shape == RailShape.NORTH_SOUTH) {
            ax = 0;
            az = 1;
        } else {
            return 0;
        }
        int dir;
        if (level.getBlockState(pos.offset(-ax, 0, -az)).isRedstoneConductor(level, pos.offset(-ax, 0, -az))) {
            dir = 1;
        } else if (level.getBlockState(pos.offset(ax, 0, az)).isRedstoneConductor(level, pos.offset(ax, 0, az))) {
            dir = -1;
        } else {
            return 0;
        }
        double along = (ax * tangent.x() + az * tangent.z()) * dir;
        return along >= 0 ? 1 : -1;
    }
}
