package com.slashrails.command;

import com.slashrails.SlashRails;
import com.slashrails.ride.CartRide;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Measures a ride objectively: the cart's heading (from its movement between ticks), the largest
 * heading change in a single tick, speed, and ticks spent on a smoothed curve vs. derailed.
 */
public final class RideProbe {

    private static final class Session {
        final int total;
        int ticks;
        Vec3 last;
        double lastHeading = Double.NaN;
        double maxTurn;
        double sumTurn;
        int turnSamples;
        double maxSpeed;
        int onCurve;
        int offRails;

        Session(int total) {
            this.total = total;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private RideProbe() {
    }

    static void start(ServerPlayer player, int ticks) {
        SESSIONS.put(player.getUUID(), new Session(ticks));
    }

    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) return;
        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> e = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
            Session s = e.getValue();
            if (player == null || !(player.getVehicle() instanceof AbstractMinecart cart)) {
                if (player != null) report(player, s, "stopped: left the cart");
                it.remove();
                continue;
            }
            Vec3 pos = cart.position();
            if (s.last != null) {
                double dx = pos.x - s.last.x, dz = pos.z - s.last.z;
                double speed = Math.sqrt(dx * dx + dz * dz);
                s.maxSpeed = Math.max(s.maxSpeed, speed);
                if (speed > 0.02) {
                    double heading = Math.toDegrees(Math.atan2(dz, dx));
                    if (!Double.isNaN(s.lastHeading)) {
                        double d = Math.abs(heading - s.lastHeading) % 360;
                        if (d > 180) d = 360 - d;
                        s.maxTurn = Math.max(s.maxTurn, d);
                        s.sumTurn += d;
                        s.turnSamples++;
                    }
                    s.lastHeading = heading;
                }
            }
            s.last = pos;
            if (((CartRide) cart).slashrails$runId() != 0) s.onCurve++;
            if (!com.slashrails.ride.Carts.isOnRails(cart)) s.offRails++;
            if (++s.ticks >= s.total) {
                report(player, s, "done");
                it.remove();
            }
        }
    }

    private static void report(ServerPlayer player, Session s, String why) {
        String text = String.format("Probe %s after %d ticks: max turn %.1f deg/tick, mean %.2f deg/tick, "
                        + "top speed %.1f m/s, %d ticks on a smoothed curve, %d ticks off rails",
                why, s.ticks, s.maxTurn, s.turnSamples == 0 ? 0 : s.sumTurn / s.turnSamples,
                s.maxSpeed * 20, s.onCurve, s.offRails);
        SlashRails.LOG.info("[probe] {}: {}", player.getScoreboardName(), text);
        player.sendSystemMessage(Component.literal(text));
    }
}
