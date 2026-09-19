package com.slashrails.net;

import com.slashrails.core.Dir;
import com.slashrails.core.RailNode;
import com.slashrails.run.SmoothRun;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Wire format of the two server-to-client messages, shared by every networking API generation
 * (payload records on MC 1.20.5+, raw channel buffers before that).
 */
public final class RunWire {

    private RunWire() {
    }

    public static void writeRuns(FriendlyByteBuf buf, RunsPayload p) {
        buf.writeBoolean(p.replace());
        buf.writeVarInt(p.runs().size());
        for (SmoothRun run : p.runs()) writeRun(buf, run);
    }

    public static RunsPayload readRuns(FriendlyByteBuf buf) {
        boolean replace = buf.readBoolean();
        int count = buf.readVarInt();
        List<SmoothRun> runs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) runs.add(readRun(buf));
        return new RunsPayload(replace, runs);
    }

    public static void writeRemove(FriendlyByteBuf buf, RemoveRunPayload p) {
        buf.writeVarInt(p.id());
    }

    public static RemoveRunPayload readRemove(FriendlyByteBuf buf) {
        return new RemoveRunPayload(buf.readVarInt());
    }

    private static void writeRun(FriendlyByteBuf buf, SmoothRun run) {
        buf.writeVarInt(run.id());
        buf.writeBoolean(run.closed());
        buf.writeVarInt(run.size());
        for (int i = 0; i < run.size(); i++) {
            RailNode n = run.nodes().get(i);
            buf.writeLong(new BlockPos(n.x(), n.y(), n.z()).asLong());
            buf.writeByte(n.a().ordinal() * 4 + n.b().ordinal() + (run.tight(i) ? 16 : 0));
        }
    }

    private static SmoothRun readRun(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        boolean closed = buf.readBoolean();
        int size = buf.readVarInt();
        if (size > 1 << 16) throw new IllegalArgumentException("run too large: " + size);
        Dir[] dirs = Dir.values();
        List<RailNode> nodes = new ArrayList<>(size);
        boolean[] tight = new boolean[size];
        for (int i = 0; i < size; i++) {
            BlockPos p = BlockPos.of(buf.readLong());
            int e = buf.readByte();
            nodes.add(new RailNode(p.getX(), p.getY(), p.getZ(), dirs[(e >> 2) & 3], dirs[e & 3]));
            tight[i] = (e & 16) != 0;
        }
        return new SmoothRun(id, nodes, closed, tight);
    }
}
