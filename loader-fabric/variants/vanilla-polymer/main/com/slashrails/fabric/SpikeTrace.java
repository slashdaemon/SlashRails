package com.slashrails.fabric;

import com.slashrails.SlashRails;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Spike-only: records every overworld minecart's server position each tick, then the rails under
 * the traced path, to {@code slashrails-trace.csv} in the server directory. scripts/spike_client_model.py
 * replays it through the vanilla client's interpolation and cart renderer.
 */
final class SpikeTrace {

    private static PrintWriter out;
    private static int ticksLeft;
    private static int tick;
    /** Rails seen around traced carts, captured while they exist (the self-test removes its fixtures). */
    private static final java.util.Map<BlockPos, String> rails = new java.util.HashMap<>();

    private SpikeTrace() {
    }

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(SpikeTrace::tick);
    }

    static String start(MinecraftServer server, int ticks) {
        stop(server);
        try {
            Path file = server.getServerDirectory().resolve("slashrails-trace.csv");
            out = new PrintWriter(Files.newBufferedWriter(file));
        } catch (IOException e) {
            return "trace failed: " + e;
        }
        out.println("kind,tick,id,x,y,z,yaw,shape,curve");
        ticksLeft = ticks;
        tick = 0;
        rails.clear();
        return "tracing " + ticks + " ticks";
    }

    private static void tick(MinecraftServer server) {
        if (out == null) return;
        ServerLevel level = server.overworld();
        List<? extends AbstractMinecart> carts = level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(AbstractMinecart.class), c -> true);
        for (AbstractMinecart c : carts) {
            out.printf(Locale.ROOT, "cart,%d,%d,%.6f,%.6f,%.6f,%.3f,,%d%n", tick, c.getId(), c.getX(), c.getY(), c.getZ(), c.getYRot(),
                    ((com.slashrails.ride.CartRide) c).slashrails$runId() != 0 ? 1 : 0);
            BlockPos b = c.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(b.getX() - 2, b.getY() - 2, b.getZ() - 2, b.getX() + 2, b.getY() + 1, b.getZ() + 2)) {
                BlockState s = level.getBlockState(p);
                if (BaseRailBlock.isRail(s)) {
                    RailShape shape = s.getValue(((BaseRailBlock) s.getBlock()).getShapeProperty());
                    rails.putIfAbsent(p.immutable(), shape.getSerializedName());
                }
            }
        }
        tick++;
        if (--ticksLeft <= 0) stop(server);
    }

    private static void stop(MinecraftServer server) {
        if (out == null) return;
        rails.forEach((p, shape) -> out.printf(Locale.ROOT, "rail,,,%d,%d,%d,,%s,%n", p.getX(), p.getY(), p.getZ(), shape));
        out.close();
        out = null;
        SlashRails.LOG.info("[spike] trace written");
    }
}
