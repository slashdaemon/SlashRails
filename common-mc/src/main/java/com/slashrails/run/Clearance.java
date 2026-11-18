package com.slashrails.run;

import com.slashrails.core.RailNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Which rails have room for the cart to leave the vanilla line. A vanilla cart is 0.98 wide and
 * rides dead centre, so a wall, a tunnel or a lamp right beside the track is fine for vanilla but
 * would scrape a curve that swings sideways. A rail is "tight" when any block around it (at rail
 * and cart height, excluding the run's own rails) has a collision shape; tightness then spreads to
 * the neighbouring rails, since the cart body is longer than one block. A neighbour in an unloaded
 * chunk counts as blocked: smoothing never loads chunks.
 */
public final class Clearance {

    private Clearance() {
    }

    public static boolean[] compute(Level level, List<RailNode> nodes, boolean closed) {
        int n = nodes.size();
        Set<BlockPos> own = new HashSet<>();
        for (RailNode r : nodes) own.add(RailGeometry.pos(r));

        boolean[] blocked = new boolean[n];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < n; i++) {
            RailNode r = nodes.get(i);
            search:
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    for (int dy = 0; dy <= 1; dy++) {
                        p.set(r.x() + dx, r.y() + dy, r.z() + dz);
                        if (dy == 0 && own.contains(p)) continue;
                        // Never read into an unloaded chunk: getBlockState would load (or generate) it
                        // on the server thread. A run can end on a chunk edge, since RunDetector stops
                        // at unloaded chunks. Unknown counts as blocked: the curve keeps to the vanilla line.
                        if (!level.isLoaded(p)) {
                            blocked[i] = true;
                            break search;
                        }
                        BlockState s = level.getBlockState(p);
                        if (!s.getCollisionShape(level, p).isEmpty()) {
                            blocked[i] = true;
                            break search;
                        }
                    }
                }
            }
        }
        boolean[] tight = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (!blocked[i]) continue;
            for (int k = -1; k <= 1; k++) {
                int j = i + k;
                if (closed) j = (j + n) % n;
                if (j >= 0 && j < n) tight[j] = true;
            }
        }
        return tight;
    }
}
