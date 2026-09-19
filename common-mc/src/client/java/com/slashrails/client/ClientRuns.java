package com.slashrails.client;

import com.slashrails.client.render.TrackMesh;
import com.slashrails.net.RemoveRunPayload;
import com.slashrails.net.RunsPayload;
import com.slashrails.run.SmoothRun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The client's copy of the smoothed runs in the current dimension. Read off-thread by chunk meshing,
 * so it is a concurrent map of immutable entries.
 */
public final class ClientRuns {

    /** A rail position's run and its index along that run. */
    public record Slot(SmoothRun run, int index) {
    }

    private static final Map<Integer, SmoothRun> RUNS = new ConcurrentHashMap<>();
    private static final Map<Long, Slot> SLOTS = new ConcurrentHashMap<>();

    private ClientRuns() {
    }

    @Nullable
    public static Slot at(BlockPos pos) {
        return SLOTS.isEmpty() ? null : SLOTS.get(pos.asLong());
    }

    @Nullable
    public static Slot at(long pos) {
        return SLOTS.isEmpty() ? null : SLOTS.get(pos);
    }

    @Nullable
    public static SmoothRun byId(int id) {
        return RUNS.get(id);
    }

    public static boolean isEmpty() {
        return RUNS.isEmpty();
    }

    public static Collection<SmoothRun> all() {
        return RUNS.values();
    }

    // ---- network (main thread) ------------------------------------------------------------

    public static void handle(RunsPayload payload) {
        if (payload.replace()) {
            for (SmoothRun old : RUNS.values()) removeInternal(old);
        }
        for (SmoothRun run : payload.runs()) {
            SmoothRun old = RUNS.get(run.id());
            if (old != null) removeInternal(old);
            RUNS.put(run.id(), run);
            for (int i = 0; i < run.size(); i++) SLOTS.put(run.rail(i).asLong(), new Slot(run, i));
            TrackMesh.forget(run.id());
            markDirty(run);
        }
    }

    public static void handle(RemoveRunPayload payload) {
        SmoothRun run = RUNS.get(payload.runId());
        if (run != null) removeInternal(run);
    }

    public static void clear() {
        RUNS.clear();
        SLOTS.clear();
        TrackMesh.forgetAll();
    }

    private static void removeInternal(SmoothRun run) {
        RUNS.remove(run.id());
        for (int i = 0; i < run.size(); i++) {
            Slot s = SLOTS.get(run.rail(i).asLong());
            if (s != null && s.run().id() == run.id()) SLOTS.remove(run.rail(i).asLong());
        }
        TrackMesh.forget(run.id());
        markDirty(run);
    }

    /** Re-mesh every chunk section holding one of the run's rails (vanilla rail models come back or go). */
    private static void markDirty(SmoothRun run) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        for (int i = 0; i < run.size(); i++) {
            BlockPos p = run.rail(i);
            level.setSectionDirtyWithNeighbors(p.getX() >> 4, p.getY() >> 4, p.getZ() >> 4);
        }
    }
}
