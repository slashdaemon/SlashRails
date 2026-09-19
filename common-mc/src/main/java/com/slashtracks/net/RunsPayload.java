package com.slashtracks.net;

import com.slashtracks.SlashTracks;
import com.slashtracks.core.Dir;
import com.slashtracks.core.RailNode;
import com.slashtracks.run.SmoothRun;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client: smoothed runs in the player's current dimension. {@code replace} means "this is
 * the full set" (join, dimension change); otherwise the runs are additions.
 */
public record RunsPayload(boolean replace, List<SmoothRun> runs) implements CustomPacketPayload {

    public static final Type<RunsPayload> TYPE = new Type<>(SlashTracks.id("runs"));

    public static final StreamCodec<FriendlyByteBuf, RunsPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.replace);
                buf.writeVarInt(p.runs.size());
                for (SmoothRun run : p.runs) writeRun(buf, run);
            },
            buf -> {
                boolean replace = buf.readBoolean();
                int count = buf.readVarInt();
                List<SmoothRun> runs = new ArrayList<>(count);
                for (int i = 0; i < count; i++) runs.add(readRun(buf));
                return new RunsPayload(replace, runs);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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
