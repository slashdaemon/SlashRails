package com.slashrails.run;

import com.slashrails.core.Dir;
import com.slashrails.core.RailNode;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every smoothed run in one dimension. Saved with the world as {@code data/slashrails_runs.dat};
 * how it plugs into the level's saved-data storage differs per Minecraft version ({@link RunStore}).
 */
public final class SmoothRunRegistry extends RunStore {

    static final String NAME = "slashrails_runs";

    private final Map<Integer, SmoothRun> runs = new LinkedHashMap<>();
    private final Long2IntOpenHashMap byPos = new Long2IntOpenHashMap();
    private int nextId = 1;

    public SmoothRunRegistry() {
        byPos.defaultReturnValue(0);
    }

    public static SmoothRunRegistry get(ServerLevel level) {
        return RunStore.get(level);
    }

    public boolean isEmpty() {
        return runs.isEmpty();
    }

    @Nullable
    public SmoothRun runAt(BlockPos pos) {
        int id = byPos.get(pos.asLong());
        return id == 0 ? null : runs.get(id);
    }

    public boolean contains(BlockPos pos) {
        return byPos.containsKey(pos.asLong());
    }

    @Nullable
    public SmoothRun byId(int id) {
        return runs.get(id);
    }

    public Collection<SmoothRun> all() {
        return Collections.unmodifiableCollection(runs.values());
    }

    public SmoothRun add(java.util.List<RailNode> nodes, boolean closed, boolean[] tight) {
        SmoothRun run = new SmoothRun(nextId++, nodes, closed, tight);
        put(run);
        setDirty();
        return run;
    }

    public void remove(SmoothRun run) {
        if (runs.remove(run.id()) == null) return;
        for (int i = 0; i < run.size(); i++) byPos.remove(run.rail(i).asLong());
        setDirty();
    }

    private void put(SmoothRun run) {
        runs.put(run.id(), run);
        for (int i = 0; i < run.size(); i++) byPos.put(run.rail(i).asLong(), run.id());
    }

    // ---- persistence ----------------------------------------------------------------------

    @Override
    CompoundTag writeTag(CompoundTag tag) {
        tag.putInt("NextId", nextId);
        ListTag list = new ListTag();
        for (SmoothRun run : runs.values()) {
            list.add(writeRun(run));
        }
        tag.put("Runs", list);
        return tag;
    }

    static SmoothRunRegistry readTag(CompoundTag tag) {
        SmoothRunRegistry r = new SmoothRunRegistry();
        r.nextId = Math.max(1, Nbt.getInt(tag, "NextId"));
        ListTag list = Nbt.getCompoundList(tag, "Runs");
        for (int i = 0; i < list.size(); i++) {
            SmoothRun run = readRun(Nbt.compoundAt(list, i));
            if (run != null) {
                r.put(run);
                r.nextId = Math.max(r.nextId, run.id() + 1);
            }
        }
        return r;
    }

    /** Rails are stored as packed positions plus one byte each: exits (a*4 + b), and 16 if tight. */
    static CompoundTag writeRun(SmoothRun run) {
        CompoundTag t = new CompoundTag();
        t.putInt("Id", run.id());
        t.putBoolean("Closed", run.closed());
        long[] pos = new long[run.size()];
        byte[] exits = new byte[run.size()];
        for (int i = 0; i < run.size(); i++) {
            RailNode n = run.nodes().get(i);
            pos[i] = run.rail(i).asLong();
            exits[i] = (byte) (n.a().ordinal() * 4 + n.b().ordinal() + (run.tight(i) ? 16 : 0));
        }
        t.putLongArray("Rails", pos);
        t.putByteArray("Exits", exits);
        return t;
    }

    @Nullable
    static SmoothRun readRun(CompoundTag t) {
        long[] pos = Nbt.getLongArray(t, "Rails");
        byte[] exits = Nbt.getByteArray(t, "Exits");
        if (pos.length == 0 || pos.length != exits.length) return null;
        java.util.List<RailNode> nodes = new ArrayList<>(pos.length);
        boolean[] tight = new boolean[pos.length];
        Dir[] dirs = Dir.values();
        for (int i = 0; i < pos.length; i++) {
            BlockPos p = BlockPos.of(pos[i]);
            nodes.add(new RailNode(p.getX(), p.getY(), p.getZ(), dirs[(exits[i] >> 2) & 3], dirs[exits[i] & 3]));
            tight[i] = (exits[i] & 16) != 0;
        }
        try {
            return new SmoothRun(Nbt.getInt(t, "Id"), nodes, Nbt.getBoolean(t, "Closed"), tight);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
