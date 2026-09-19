package com.slashrails.forge.client;

import com.slashrails.Ids;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.event.ModelEvent;

import java.util.Map;

/** Wraps every rail block-state model: smoothed rails draw their slice of the curve instead. */
final class RailModels {

    private RailModels() {
    }

    static void wrap(ModelEvent.ModifyBakingResult event) {
        for (Map.Entry<ResourceLocation, BakedModel> e : event.getModels().entrySet()) {
            // Block-state models are ModelResourceLocations with a variant; "inventory" is the item model.
            if (!(e.getKey() instanceof ModelResourceLocation id) || "inventory".equals(id.getVariant())) continue;
            Block block = BuiltInRegistries.BLOCK.get(Ids.of(id.getNamespace(), id.getPath()));
            if (block instanceof BaseRailBlock) {
                e.setValue(new SmoothRailModel(e.getValue(), block == Blocks.RAIL));
            }
        }
    }
}
