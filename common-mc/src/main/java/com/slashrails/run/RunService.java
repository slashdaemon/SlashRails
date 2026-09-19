package com.slashrails.run;

import com.slashrails.Config;
import com.slashrails.SlashRails;
import com.slashrails.core.RailNode;
import com.slashrails.net.RemoveRunPayload;
import com.slashrails.net.RunsPayload;
import com.slashrails.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Server-side lifecycle of smoothed runs: create, revert, invalidate, and sync to clients. */
public final class RunService {

    private record Change(ResourceKey<Level> level, long pos) {
    }

    private static final ConcurrentLinkedQueue<Change> CHANGES = new ConcurrentLinkedQueue<>();

    private RunService() {
    }

    // ---- player actions -------------------------------------------------------------------

    /** Smooth the run through {@code clicked}; returns the message for the player. */
    public static Component smooth(ServerLevel level, BlockPos clicked) {
        SmoothRunRegistry registry = SmoothRunRegistry.get(level);
        RunDetector.Result r = RunDetector.detect(level, clicked, Config.maxRunLength, registry::contains);
        if (!r.ok()) {
            return Component.translatable("slashrails.fail." + r.failure().name().toLowerCase(Locale.ROOT));
        }
        SmoothRun run;
        try {
            run = registry.add(r.nodes(), r.closed(), Clearance.compute(level, r.nodes(), r.closed()));
            run.curve();
        } catch (RuntimeException e) {
            SlashRails.LOG.error("Could not fit a curve over the run at {}", clicked, e);
            return Component.translatable("slashrails.fail.fit");
        }
        RunsPayload added = new RunsPayload(false, List.of(run));
        broadcast(level, p -> Platform.get().sendRuns(p, added));
        Component msg = Component.translatable(r.closed() ? "slashrails.smoothed_loop" : "slashrails.smoothed", run.size());
        int tight = run.tightCount();
        if (tight > 0) {
            msg = msg.copy().append(" ").append(Component.translatable("slashrails.tight", tight));
        }
        if (r.truncated()) {
            msg = msg.copy().append(" ").append(Component.translatable("slashrails.truncated", Config.maxRunLength));
        }
        return msg;
    }

    /** Revert the run containing {@code pos}; returns the number of rails reverted (0 if none). */
    public static int revert(ServerLevel level, BlockPos pos) {
        SmoothRunRegistry registry = SmoothRunRegistry.get(level);
        SmoothRun run = registry.runAt(pos);
        if (run == null) return 0;
        registry.remove(run);
        RemoveRunPayload removed = new RemoveRunPayload(run.id());
        broadcast(level, p -> Platform.get().sendRemove(p, removed));
        return run.size();
    }

    // ---- invalidation ---------------------------------------------------------------------

    /** From the chunk mixin: a block changed. Cheap unless the position is a smoothed rail. */
    public static void onBlockChanged(ServerLevel level, BlockPos pos) {
        SmoothRunRegistry registry = SmoothRunRegistry.get(level);
        if (!registry.isEmpty() && registry.contains(pos)) {
            CHANGES.add(new Change(level.dimension(), pos.asLong()));
        }
    }

    /** End of each server tick: drop runs whose rails were broken or re-shaped. */
    public static void tick(MinecraftServer server) {
        Change c;
        while ((c = CHANGES.poll()) != null) {
            ServerLevel level = server.getLevel(c.level());
            if (level == null) continue;
            BlockPos pos = BlockPos.of(c.pos());
            SmoothRunRegistry registry = SmoothRunRegistry.get(level);
            SmoothRun run = registry.runAt(pos);
            if (run == null) continue;
            BlockState state = level.getBlockState(pos);
            RailNode node = nodeAt(run, pos);
            if (node == null || !RailGeometry.matches(state, node)) {
                registry.remove(run);
                RemoveRunPayload removed = new RemoveRunPayload(run.id());
        broadcast(level, p -> Platform.get().sendRemove(p, removed));
            }
        }
    }

    private static RailNode nodeAt(SmoothRun run, BlockPos pos) {
        for (int i = 0; i < run.size(); i++) {
            if (run.rail(i).equals(pos)) return run.nodes().get(i);
        }
        return null;
    }

    // ---- sync -----------------------------------------------------------------------------

    /** Join, respawn or dimension change: send the full set for the player's dimension. */
    public static void sendSnapshot(ServerPlayer player) {
        if (!Platform.get().clientHasMod(player)) return;
        SmoothRunRegistry registry = SmoothRunRegistry.get((net.minecraft.server.level.ServerLevel) player.level());
        Platform.get().sendRuns(player, new RunsPayload(true, new ArrayList<>(registry.all())));
    }

    private static void broadcast(ServerLevel level, java.util.function.Consumer<ServerPlayer> send) {
        for (ServerPlayer p : level.players()) {
            if (Platform.get().clientHasMod(p)) send.accept(p);
        }
    }
}
