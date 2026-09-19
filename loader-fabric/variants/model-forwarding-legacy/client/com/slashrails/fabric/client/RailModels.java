package com.slashrails.fabric.client;

import com.slashrails.Ids;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Wraps every rail block-state model: smoothed rails draw their slice of the curve instead. */
final class RailModels {

    private RailModels() {
    }

    static void register() {
        ModelLoadingPlugin.register(plugin -> plugin.modifyModelAfterBake().register((model, context) -> {
            // Block-state models are ModelResourceLocations with a variant; "inventory" is the item model.
            if (model == null || !(context.id() instanceof ModelResourceLocation id) || "inventory".equals(id.getVariant())) return model;
            Block block = BuiltInRegistries.BLOCK.get(Ids.of(id.getNamespace(), id.getPath()));
            if (!(block instanceof BaseRailBlock)) return model;
            return new SmoothRailModel(model, block == Blocks.RAIL);
        }));
    }
}
