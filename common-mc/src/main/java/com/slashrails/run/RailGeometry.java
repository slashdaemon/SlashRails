package com.slashrails.run;

import com.slashrails.core.Dir;
import com.slashrails.core.RailNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Translates vanilla rail block states into the core's exit model. */
public final class RailGeometry {

    private RailGeometry() {
    }

    public static boolean isRail(BlockState state) {
        return state.getBlock() instanceof BaseRailBlock;
    }

    @Nullable
    public static RailShape shape(BlockState state) {
        if (!(state.getBlock() instanceof BaseRailBlock rail)) return null;
        return state.getValue(rail.getShapeProperty());
    }

    /** The two exits of a flat shape, or {@code null} for an ascending one. */
    @Nullable
    public static Dir[] exits(RailShape shape) {
        return switch (shape) {
            case NORTH_SOUTH -> new Dir[]{Dir.NORTH, Dir.SOUTH};
            case EAST_WEST -> new Dir[]{Dir.EAST, Dir.WEST};
            case SOUTH_EAST -> new Dir[]{Dir.SOUTH, Dir.EAST};
            case SOUTH_WEST -> new Dir[]{Dir.SOUTH, Dir.WEST};
            case NORTH_WEST -> new Dir[]{Dir.NORTH, Dir.WEST};
            case NORTH_EAST -> new Dir[]{Dir.NORTH, Dir.EAST};
            default -> null;
        };
    }

    public static BlockPos step(BlockPos pos, Dir d) {
        return pos.offset(d.dx, 0, d.dz);
    }

    /** Core rail nodes for an ordered list of flat rail positions. */
    public static List<RailNode> nodes(BlockGetter level, List<BlockPos> rails) {
        return rails.stream().map(p -> {
            RailShape shape = shape(level.getBlockState(p));
            Dir[] ex = shape == null ? null : exits(shape);
            if (ex == null) throw new IllegalStateException("not a flat rail at " + p);
            return new RailNode(p.getX(), p.getY(), p.getZ(), ex[0], ex[1]);
        }).toList();
    }

    /** Whether the block at {@code node}'s position is still a flat rail with the same two exits. */
    public static boolean matches(BlockState state, RailNode node) {
        RailShape shape = shape(state);
        Dir[] ex = shape == null ? null : exits(shape);
        return ex != null && node.hasExit(ex[0]) && node.hasExit(ex[1]);
    }

    public static BlockPos pos(RailNode node) {
        return new BlockPos(node.x(), node.y(), node.z());
    }
}
