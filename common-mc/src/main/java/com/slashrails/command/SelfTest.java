package com.slashrails.command;

import com.slashrails.SlashRails;
import com.slashrails.core.Dir;
import com.slashrails.core.RailNode;
import com.slashrails.mixin.AbstractMinecartAccessor;
import com.slashrails.ride.CartRide;
import com.slashrails.run.RunService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Unattended end-to-end check, runnable from a dedicated server console: for each fixture, ride a
 * loaded cart over the vanilla track, then over the smoothed track, and compare. A smoothed ride
 * passes when the cart never leaves the rails, reaches the far end (or laps a loop), stays within
 * the fixture's turn limit per tick, and trips the detector rail's lamp.
 */
public final class SelfTest {

    /**
     * @param hopper ride an empty hopper cart instead of a ridden minecart (lighter, 0.96 friction:
     *               it coasts to a stop, so it only has to stay on the curve, not finish)
     */
    private record Case(String name, List<Dir> steps, boolean closed, double turnLimit, boolean trench, boolean hopper,
                        boolean ramp, boolean reverse) {
        Case(String name, List<Dir> steps, boolean closed, double turnLimit) {
            this(name, steps, closed, turnLimit, false, false, false, false);
        }
    }

    private enum Phase { VANILLA, SMOOTH }

    private static final class Ride {
        AbstractMinecart cart;
        ArmorStand rider;
        Vec3 last;
        double lastHeading = Double.NaN;
        double maxTurn;
        String worstAt = "";
        double travelled;
        int ticks;
        int offRails;
        int onCurve;
        boolean lampLit;
        boolean reached;
    }

    private static final int TIMEOUT = 900;

    private static SelfTest running;

    private final CommandSourceStack source;
    private final ServerLevel level;
    private final BlockPos origin;
    private final List<Case> cases = new ArrayList<>();
    private final List<String> results = new ArrayList<>();
    private int caseIndex;
    private Phase phase;
    private TrackBuilder.Fixture fixture;
    private Ride ride;
    private double vanillaTurn;
    private int passed;
    private final List<long[]> forced = new ArrayList<>();

    private SelfTest(CommandSourceStack source) {
        this.source = source;
        this.level = source.getLevel();
        this.origin = BlockPos.containing(source.getPosition()).offset(6, 0, 6);
        cases.add(new Case("staircase 1:2", TestTracks.staircase(2, 6), false, 5));
        cases.add(new Case("staircase 1:4", TestTracks.staircase(4, 6), false, 5));
        cases.add(new Case("staircase 1:8", TestTracks.staircase(8, 4), false, 5));
        cases.add(new Case("diagonal", TestTracks.diagonal(10), false, 8));
        cases.add(new Case("arc r=16", TestTracks.arc(16), false, 6));
        cases.add(new Case("s-curve r=10", TestTracks.sCurve(10), false, 8));
        cases.add(new Case("loop r=10", TestTracks.loop(10), true, 8));
        cases.add(new Case("90-degree corner", TestTracks.corner(), false, Double.NaN));
        // In the walled middle the cart must keep to the vanilla line: it may turn sharply there but
        // must not scrape the walls, stall or derail.
        cases.add(new Case("staircase 1:3 in a trench", TestTracks.staircase(3, 8), false, Double.NaN, true, false, false, false));
        cases.add(new Case("hopper cart, staircase 1:2", TestTracks.staircase(2, 6), false, 5, false, true, false, false));
        cases.add(new Case("entering from vanilla track up a ramp", TestTracks.staircase(3, 6), false, 5, false, false, true, false));
        cases.add(new Case("staircase 1:3 ridden backwards", TestTracks.staircase(3, 6), false, 5, false, false, false, true));
    }

    static boolean start(CommandSourceStack source) {
        if (running != null) return false;
        running = new SelfTest(source);
        running.begin(Phase.VANILLA);
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (running == null) return;
        if (running.breakCheckTicks > 0) {
            running.breakCheckStep();
            return;
        }
        running.step();
    }

    // ---- breaking a smoothed rail reverts its run ------------------------------------------

    private boolean breakChecked;
    private int breakCheckTicks;
    private BlockPos breakFirst;

    private void startBreakCheck() {
        fixture = TrackBuilder.build(level, origin, TestTracks.staircase(3, 4), false);
        forceChunks(true);
        RailNode first = fixture.nodes().get(0);
        breakFirst = new BlockPos(first.x(), first.y(), first.z());
        RunService.smooth(level, breakFirst);
        RailNode mid = fixture.nodes().get(fixture.nodes().size() / 2);
        level.destroyBlock(new BlockPos(mid.x(), mid.y(), mid.z()), false);
        breakCheckTicks = 3; // the revert happens at the end of the next server tick
    }

    private void breakCheckStep() {
        if (--breakCheckTicks > 0) return;
        boolean reverted = com.slashrails.run.SmoothRunRegistry.get(level).runAt(breakFirst) == null;
        String line = (reverted ? "PASS" : "FAIL") + " breaking a smoothed rail reverts its run";
        if (reverted) passed++;
        results.add(line);
        say(line);
        TrackBuilder.clear(level, fixture.minX() - 3, fixture.minZ() - 3, fixture.maxX() + 3, fixture.maxZ() + 3, fixture.y());
        forceChunks(false);
        int total = cases.size() + 1;
        say((passed == total ? "SELFTEST PASSED " : "SELFTEST FAILED ") + passed + "/" + total);
        running = null;
    }

    // ---- state machine --------------------------------------------------------------------

    private Case current() {
        return cases.get(caseIndex);
    }

    private void begin(Phase p) {
        phase = p;
        Case c = current();
        fixture = TrackBuilder.build(level, origin, c.steps(), c.closed(), c.trench(), c.ramp());
        if (p == Phase.VANILLA) forceChunks(true);
        if (p == Phase.SMOOTH) {
            RailNode n = fixture.nodes().get(0);
            RunService.smooth(level, new BlockPos(n.x(), n.y(), n.z()));
        }
        ride = new Ride();
        List<RailNode> ns = fixture.nodes();
        RailNode first = c.reverse() ? ns.get(ns.size() - 2) : ns.get(c.closed() ? 0 : 1);
        double sx = first.x() + 0.5, sy = first.y() + 0.0625, sz = first.z() + 0.5;
        if (fixture.rampStart() != null) {
            sx = fixture.rampStart().getX() + 0.5;
            sy = fixture.rampStart().getY() + 0.0625;
            sz = fixture.rampStart().getZ() + 0.5;
        }
        if (p == Phase.SMOOTH && fixture.rampStart() == null) {
            // Put the cart on the curve, as a cart arriving along the track would be.
            com.slashrails.run.SmoothRun run = com.slashrails.run.SmoothRunRegistry.get(level)
                    .runAt(new BlockPos(first.x(), first.y(), first.z()));
            if (run != null) {
                com.slashrails.core.SmoothCurve curve = run.curve();
                com.slashrails.core.Pt on = curve.pointAt(curve.project(sx, sy, sz, Double.NaN, 0));
                sx = on.x();
                sz = on.z();
            }
        }
        AbstractMinecart cart = c.hopper()
                ? new MinecartHopper(level, sx, sy, sz)
                : new Minecart(level, sx, sy, sz);
        level.addFreshEntity(cart);
        ArmorStand rider = null;
        if (!c.hopper()) {
            rider = new ArmorStand(level, cart.getX(), cart.getY(), cart.getZ());
            level.addFreshEntity(rider);
            rider.startRiding(cart, true);
        }
        Dir d = fixture.startDir();
        if (c.reverse()) {
            RailNode last = ns.get(ns.size() - 1), prev = ns.get(ns.size() - 2);
            d = Dir.of(prev.x() - last.x(), prev.z() - last.z());
        }
        cart.setDeltaMovement(d.dx * 0.4, 0, d.dz * 0.4);
        ride.cart = cart;
        ride.rider = rider;
        ride.last = cart.position();
    }

    private void step() {
        Ride r = ride;
        AbstractMinecart cart = r.cart;
        r.ticks++;
        Vec3 pos = cart.position();
        double dx = pos.x - r.last.x, dz = pos.z - r.last.z;
        double moved = Math.sqrt(dx * dx + dz * dz);
        r.travelled += moved;
        if (moved > 0.02 && !r.reached && r.ticks > 2) { // first ticks: the cart settles from its spawn point
            double heading = Math.toDegrees(Math.atan2(dz, dx));
            if (!Double.isNaN(r.lastHeading)) {
                double turn = Math.abs(heading - r.lastHeading) % 360;
                if (turn > 180) turn = 360 - turn;
                if (turn > r.maxTurn) {
                    r.maxTurn = turn;
                    CartRide cr = (CartRide) cart;
                    r.worstAt = String.format(" at (%.2f, %.2f) tick %d run %d s=%.2f", pos.x, pos.z, r.ticks,
                            cr.slashrails$runId(), cr.slashrails$s());
                }
            }
            r.lastHeading = heading;
        }
        r.last = pos;
        if (((CartRide) cart).slashrails$runId() != 0) r.onCurve++;
        if (fixture.lamp() != null && level.getBlockState(fixture.lamp()).getValue(RedstoneLampBlock.LIT)) {
            r.lampLit = true;
        }

        Case c = current();
        List<RailNode> nodes = fixture.nodes();
        if (c.closed()) {
            if (r.travelled >= nodes.size() * 0.95) r.reached = true;
        } else {
            RailNode last = c.reverse() ? nodes.get(0) : nodes.get(nodes.size() - 1);
            RailNode prev = c.reverse() ? nodes.get(1) : nodes.get(nodes.size() - 2);
            // Past the middle of the last rail, heading out of the track.
            double ex = last.x() + 0.5 - (prev.x() + 0.5), ez = last.z() + 0.5 - (prev.z() + 0.5);
            double along = (pos.x - (last.x() + 0.5)) * ex + (pos.z - (last.z() + 0.5)) * ez;
            if (along >= 0 && Math.abs(pos.x - (last.x() + 0.5)) < 1.5 && Math.abs(pos.z - (last.z() + 0.5)) < 1.5) {
                r.reached = true;
            }
        }
        if (!r.reached && !((AbstractMinecartAccessor) cart).slashrails$isOnRails()) r.offRails++;

        boolean stalled = r.ticks > 40 && cart.getDeltaMovement().horizontalDistanceSqr() < 1e-6;
        if (r.reached || r.ticks >= TIMEOUT || stalled || !cart.isAlive()) finishPhase();
    }

    private void finishPhase() {
        Ride r = ride;
        r.cart.discard();
        if (r.rider != null) r.rider.discard();
        Case c = current();
        if (phase == Phase.VANILLA) {
            vanillaTurn = r.maxTurn;
            begin(Phase.SMOOTH);
            return;
        }

        boolean turnOk = Double.isNaN(c.turnLimit()) || r.maxTurn <= c.turnLimit();
        boolean lampOk = fixture.lamp() == null || r.lampLit;
        boolean finished = r.reached || c.hopper();
        if (c.hopper()) lampOk = true;
        boolean ok = finished && r.offRails == 0 && turnOk && lampOk && r.onCurve > 0;
        if (ok) passed++;
        String line = String.format("%s %s: vanilla max turn %.1f deg/tick -> smoothed %.1f deg/tick%s%s; "
                        + "%s, %d ticks off rails, %d ticks on curve, %s",
                ok ? "PASS" : "FAIL", c.name(), vanillaTurn, r.maxTurn, ok ? "" : r.worstAt,
                Double.isNaN(c.turnLimit()) ? "" : String.format(" (limit %.0f)", c.turnLimit()),
                r.reached ? (c.closed() ? "lapped" : "reached the end") : "DID NOT FINISH (" + r.ticks + " ticks)",
                r.offRails, r.onCurve,
                fixture.lamp() == null ? "no detector" : (r.lampLit ? "detector lamp lit" : "DETECTOR LAMP NEVER LIT"));
        results.add(line);
        say(line);

        RailNode first = fixture.nodes().get(0);
        RunService.revert(level, new BlockPos(first.x(), first.y(), first.z()));
        TrackBuilder.clear(level, fixture.minX() - 3, fixture.minZ() - 3, fixture.maxX() + 3, fixture.maxZ() + 3, fixture.y());
        forceChunks(false);

        caseIndex++;
        if (caseIndex < cases.size()) {
            begin(Phase.VANILLA);
        } else if (!breakChecked) {
            breakChecked = true;
            startBreakCheck();
        } else {
            String summary = (passed == cases.size() ? "SELFTEST PASSED " : "SELFTEST FAILED ") + passed + "/" + cases.size();
            say(summary);
            running = null;
        }
    }

    private void forceChunks(boolean force) {
        if (force) {
            forced.clear();
            for (int cx = (fixture.minX() - 4) >> 4; cx <= (fixture.maxX() + 4) >> 4; cx++) {
                for (int cz = (fixture.minZ() - 4) >> 4; cz <= (fixture.maxZ() + 4) >> 4; cz++) {
                    if (level.setChunkForced(cx, cz, true)) forced.add(new long[]{cx, cz});
                }
            }
        } else {
            for (long[] c : forced) level.setChunkForced((int) c[0], (int) c[1], false);
            forced.clear();
        }
    }

    private void say(String line) {
        SlashRails.LOG.info("[selftest] {}", line);
        source.sendSuccess(() -> Component.literal(line), true);
    }
}
