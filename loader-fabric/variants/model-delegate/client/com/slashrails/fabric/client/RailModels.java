package com.slashrails.fabric.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Wraps every rail block-state model: smoothed rails draw their slice of the curve instead. */
final class RailModels {

    private RailModels() {
    }

    static void register() {
        ModelLoadingPlugin.register(plugin -> plugin.modifyBlockModelAfterBake().register((model, context) -> {
            Block block = BuiltInRegistries.BLOCK.getValue(context.id().id());
            if (!(block instanceof BaseRailBlock)) return model;
            return new SmoothRailModel(model, block == Blocks.RAIL);
        }));
    }
}
