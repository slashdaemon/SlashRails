package com.slashrails.neoforge.client;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Map;

/** Wraps every rail block-state model: smoothed rails draw their slice of the curve instead. */
final class RailModels {

    private RailModels() {
    }

    static void wrap(ModelEvent.ModifyBakingResult event) {
        for (Map.Entry<BlockState, BlockStateModel> e : event.getBakingResult().blockStateModels().entrySet()) {
            Block block = e.getKey().getBlock();
            if (block instanceof BaseRailBlock) {
                e.setValue(new SmoothRailModel(e.getValue(), block == Blocks.RAIL));
            }
        }
    }
}
