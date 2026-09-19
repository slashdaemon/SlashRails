package com.slashrails.run;

import com.slashrails.core.CurveFitter;
import com.slashrails.core.RailNode;
import com.slashrails.core.SmoothCurve;
import net.minecraft.core.BlockPos;

import java.util.List;

/** One smoothed rail run: its rails in travel order and the curve fitted over them. */
public final class SmoothRun {

    private final int id;
    private final List<RailNode> nodes;
    private final boolean closed;
    private final BlockPos[] positions;
    private final boolean[] tight;
    private volatile SmoothCurve curve;

    /** @param tight per rail: no room beside the track, so the curve keeps to the vanilla line there */
    public SmoothRun(int id, List<RailNode> nodes, boolean closed, boolean[] tight) {
        if (tight.length != nodes.size()) throw new IllegalArgumentException("tight[] length");
        this.id = id;
        this.nodes = List.copyOf(nodes);
        this.closed = closed;
        this.tight = tight.clone();
        this.positions = nodes.stream().map(RailGeometry::pos).toArray(BlockPos[]::new);
    }

    public int id() {
        return id;
    }

    public List<RailNode> nodes() {
        return nodes;
    }

    public boolean closed() {
        return closed;
    }

    public int size() {
        return positions.length;
    }

    public boolean tight(int index) {
        return tight[index];
    }

    public int tightCount() {
        int c = 0;
        for (boolean t : tight) if (t) c++;
        return c;
    }

    public BlockPos rail(int index) {
        return positions[index];
    }

    /** Fitted lazily; identical on server and client because the fit is deterministic. */
    public SmoothCurve curve() {
        SmoothCurve c = curve;
        if (c == null) {
            synchronized (this) {
                c = curve;
                if (c == null) {
                    c = CurveFitter.fit(nodes, closed, tight);
                    curve = c;
                }
            }
        }
        return c;
    }
}
