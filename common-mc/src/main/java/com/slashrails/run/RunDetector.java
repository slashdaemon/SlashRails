package com.slashrails.run;

import com.slashrails.core.Dir;
import com.slashrails.core.RailNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Walks the connected flat rails around a clicked rail. The walk follows rail exits and stops at a
 * dead end, a rail that doesn't connect back, a slope (smoothing is flat-only), a rail that already
 * belongs to another smoothed run, an unloaded chunk, or the length cap.
 */
public final class RunDetector {

    public enum Failure {
        NOT_A_RAIL, SLOPED, TOO_SHORT, NOTHING_TO_SMOOTH
    }

    /** Either a run or the reason there isn't one. */
    public record Result(List<RailNode> nodes, boolean closed, boolean truncated, Failure failure) {
        public boolean ok() {
            return failure == null;
        }
    }

    private RunDetector() {
    }

    public static Result detect(Level level, BlockPos start, int maxRails, Predicate<BlockPos> taken) {
        BlockState state = level.getBlockState(start);
        RailShape shape = RailGeometry.shape(state);
        if (shape == null) return fail(Failure.NOT_A_RAIL);
        Dir[] exits = RailGeometry.exits(shape);
        if (exits == null) return fail(Failure.SLOPED);

        Set<BlockPos> visited = new HashSet<>();
        visited.add(start);
        List<BlockPos> forward = new ArrayList<>();
        Walk f = walk(level, start, start, exits[1], maxRails - 1, visited, taken, forward);
        List<BlockPos> order = new ArrayList<>();
        boolean closed = f == Walk.CLOSED;
        boolean truncated = f == Walk.CAPPED;
        if (closed) {
            order.add(start);
            order.addAll(forward);
        } else {
            List<BlockPos> backward = new ArrayList<>();
            Walk b = walk(level, start, start, exits[0], maxRails - 1 - forward.size(), visited, taken, backward);
            truncated |= b == Walk.CAPPED;
            Collections.reverse(backward);
            order.addAll(backward);
            order.add(start);
            order.addAll(forward);
        }

        if (order.size() < (closed ? 4 : 3)) return fail(Failure.TOO_SHORT);
        List<RailNode> nodes = RailGeometry.nodes(level, order);
        if (nodes.stream().noneMatch(RailNode::isCorner)) return fail(Failure.NOTHING_TO_SMOOTH);
        return new Result(nodes, closed, truncated, null);
    }

    private enum Walk { END, CLOSED, CAPPED }

    private static Walk walk(Level level, BlockPos origin, BlockPos from, Dir out, int budget, Set<BlockPos> visited,
                             Predicate<BlockPos> taken, List<BlockPos> into) {
        BlockPos cur = from;
        Dir dir = out;
        while (true) {
            BlockPos next = RailGeometry.step(cur, dir);
            if (next.equals(origin) && into.size() >= 3) {
                Dir[] originExits = RailGeometry.exits(RailGeometry.shape(level.getBlockState(origin)));
                Dir back = dir.opposite();
                return originExits[0] == back || originExits[1] == back ? Walk.CLOSED : Walk.END;
            }
            if (visited.contains(next)) return Walk.END;
            if (into.size() >= budget) return Walk.CAPPED;
            if (!level.isLoaded(next) || taken.test(next)) return Walk.END;
            RailShape shape = RailGeometry.shape(level.getBlockState(next));
            Dir[] ex = shape == null ? null : RailGeometry.exits(shape);
            if (ex == null) return Walk.END;
            Dir back = dir.opposite();
            if (ex[0] != back && ex[1] != back) return Walk.END;
            visited.add(next);
            into.add(next);
            cur = next;
            dir = ex[0] == back ? ex[1] : ex[0];
        }
    }

    private static Result fail(Failure f) {
        return new Result(List.of(), false, false, f);
    }
}
