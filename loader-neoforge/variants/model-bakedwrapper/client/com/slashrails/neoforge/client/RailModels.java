package com.slashrails.neoforge.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Map;

/** Wraps every rail block-state model: smoothed rails draw their slice of the curve instead. */
final class RailModels {

    private RailModels() {
    }

    static void wrap(ModelEvent.ModifyBakingResult event) {
        for (Map.Entry<ModelResourceLocation, BakedModel> e : event.getModels().entrySet()) {
            ModelResourceLocation id = e.getKey();
            if (ModelResourceLocation.INVENTORY_VARIANT.equals(id.variant())) continue;
            Block block = BuiltInRegistries.BLOCK.get(id.id());
            if (block instanceof BaseRailBlock) {
                e.setValue(new SmoothRailModel(e.getValue(), block == Blocks.RAIL));
            }
        }
    }
}
