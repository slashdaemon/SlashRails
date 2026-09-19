package com.slashrails.command;

import com.slashrails.core.Dir;
import com.slashrails.core.RailNode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Lays test tracks: a floor, the rails in their exact shapes, a powered rail and a detector + lamp. */
final class TrackBuilder {

    /** What was built. {@code lamp} is next to the detector rail (null on loops). */
    record Fixture(List<RailNode> nodes, boolean closed, @Nullable BlockPos lamp, Dir startDir, int minX, int minZ,
                   int maxX, int maxZ, int y, @Nullable BlockPos rampStart) {
    }

    private TrackBuilder() {
    }

    static Fixture build(ServerLevel level, BlockPos origin, List<Dir> steps, boolean closed) {
        return build(level, origin, steps, closed, false);
    }

    static Fixture build(ServerLevel level, BlockPos origin, List<Dir> steps, boolean closed, boolean trench) {
        return build(level, origin, steps, closed, trench, false);
    }

    /**
     * @param trench wall in the middle third of the track on both sides (a 1-wide cutting)
     * @param ramp   lead in with vanilla rails one block lower and an ascending rail up to the first
     *               rail (the run stops at the slope, so a cart crosses from vanilla onto the curve)
     */
    static Fixture build(ServerLevel level, BlockPos origin, List<Dir> steps, boolean closed, boolean trench,
                         boolean ramp) {
        List<RailNode> nodes = closed ? closedNodes(origin, steps) : openNodes(origin, steps);
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (RailNode n : nodes) {
            minX = Math.min(minX, n.x());
            maxX = Math.max(maxX, n.x());
            minZ = Math.min(minZ, n.z());
            maxZ = Math.max(maxZ, n.z());
        }
        int y = origin.getY();
        clear(level, minX - 3, minZ - 3, maxX + 3, maxZ + 3, y);

        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        int poweredAt = -1, detectorAt = -1;
        if (!closed && nodes.size() >= 16) {
            poweredAt = 3;
            detectorAt = nodes.size() - 4;
        }
        BlockPos lamp = null;
        // Two passes: placing a rail re-shapes it from its neighbours; setting the shape on an
        // already-placed rail of the same block does not.
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < nodes.size(); i++) {
                RailNode n = nodes.get(i);
                BlockPos p = new BlockPos(n.x(), n.y(), n.z());
                RailShape shape = shapeOf(n.a(), n.b());
                BlockState state;
                if (i == poweredAt) {
                    level.setBlock(p.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), flags);
                    state = Blocks.POWERED_RAIL.defaultBlockState()
                            .setValue(PoweredRailBlock.SHAPE, shape).setValue(PoweredRailBlock.POWERED, true);
                } else if (i == detectorAt) {
                    state = Blocks.DETECTOR_RAIL.defaultBlockState()
                            .setValue(BaseRailBlock.WATERLOGGED, false).setValue(DetectorRailBlock.SHAPE, shape);
                    Dir side = n.a() == Dir.EAST || n.a() == Dir.WEST ? Dir.NORTH : Dir.EAST;
                    lamp = p.offset(side.dx, 0, side.dz);
                    level.setBlock(lamp, Blocks.REDSTONE_LAMP.defaultBlockState(), Block.UPDATE_ALL);
                } else {
                    state = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape);
                }
                level.setBlock(p, state, flags);
            }
        }
        if (trench) {
            java.util.Set<BlockPos> railSet = new java.util.HashSet<>();
            for (RailNode n : nodes) railSet.add(new BlockPos(n.x(), n.y(), n.z()));
            for (int i = nodes.size() / 3; i < nodes.size() * 2 / 3; i++) {
                RailNode n = nodes.get(i);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos w = new BlockPos(n.x() + dx, n.y(), n.z() + dz);
                        if (railSet.contains(w)) continue;
                        level.setBlock(w, Blocks.STONE_BRICKS.defaultBlockState(), flags);
                        level.setBlock(w.above(), Blocks.STONE_BRICKS.defaultBlockState(), flags);
                    }
                }
            }
        }
        Dir startDir = steps.get(0);
        BlockPos rampStart = null;
        if (ramp && !closed && startDir == Dir.EAST) {
            RailNode first = nodes.get(0);
            for (int x = first.x() - 8; x <= first.x() - 1; x++) {
                BlockPos lower = new BlockPos(x, y - 1, first.z());
                level.setBlock(lower.below(), Blocks.SMOOTH_STONE.defaultBlockState(), flags);
                RailShape shape = x == first.x() - 1 ? RailShape.ASCENDING_EAST : RailShape.EAST_WEST;
                level.setBlock(lower, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape), flags);
            }
            rampStart = new BlockPos(first.x() - 7, y - 1, first.z());
        }
        return new Fixture(nodes, closed, lamp, startDir, minX - (ramp ? 9 : 0), minZ, maxX, maxZ, y, rampStart);
    }

    static void clear(ServerLevel level, int x0, int z0, int x1, int z1, int y) {
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                level.setBlock(new BlockPos(x, y - 1, z), Blocks.SMOOTH_STONE.defaultBlockState(), flags);
                for (int dy = 0; dy < 4; dy++) {
                    level.setBlock(new BlockPos(x, y + dy, z), Blocks.AIR.defaultBlockState(), flags);
                }
            }
        }
    }

    static List<RailNode> openNodes(BlockPos origin, List<Dir> steps) {
        int n = steps.size() + 1;
        List<RailNode> out = new ArrayList<>(n);
        int x = origin.getX(), z = origin.getZ();
        for (int i = 0; i < n; i++) {
            Dir back = i > 0 ? steps.get(i - 1).opposite() : steps.get(0).opposite();
            Dir fwd = i < n - 1 ? steps.get(i) : steps.get(n - 2);
            out.add(new RailNode(x, origin.getY(), z, back, fwd));
            if (i < n - 1) {
                x += steps.get(i).dx;
                z += steps.get(i).dz;
            }
        }
        return out;
    }

    static List<RailNode> closedNodes(BlockPos origin, List<Dir> steps) {
        int n = steps.size();
        List<RailNode> out = new ArrayList<>(n);
        int x = origin.getX(), z = origin.getZ();
        for (int i = 0; i < n; i++) {
            out.add(new RailNode(x, origin.getY(), z, steps.get((i - 1 + n) % n).opposite(), steps.get(i)));
            x += steps.get(i).dx;
            z += steps.get(i).dz;
        }
        return out;
    }

    static RailShape shapeOf(Dir a, Dir b) {
        boolean n = a == Dir.NORTH || b == Dir.NORTH, s = a == Dir.SOUTH || b == Dir.SOUTH;
        boolean e = a == Dir.EAST || b == Dir.EAST, w = a == Dir.WEST || b == Dir.WEST;
        if (n && s) return RailShape.NORTH_SOUTH;
        if (e && w) return RailShape.EAST_WEST;
        if (s && e) return RailShape.SOUTH_EAST;
        if (s && w) return RailShape.SOUTH_WEST;
        if (n && w) return RailShape.NORTH_WEST;
        return RailShape.NORTH_EAST;
    }
}
