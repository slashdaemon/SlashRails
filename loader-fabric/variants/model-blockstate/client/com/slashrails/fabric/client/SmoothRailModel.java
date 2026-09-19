package com.slashrails.fabric.client;

import com.slashrails.client.ClientRuns;
import com.slashrails.client.render.RailSprites;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * (Fabric API renderer 6.x+, MC 1.21.5+.) A rail block-state model that, for a rail on a smoothed
 * run, emits that rail's slice of the curved track as chunk geometry. Any other rail renders exactly
 * as vanilla.
 */
final class SmoothRailModel extends WrapperBlockStateModel {


    private final boolean plainRail;

    SmoothRailModel(BlockStateModel wrapped, boolean plainRail) {
        super(wrapped);
        this.plainRail = plainRail;
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos, BlockState state,
                          RandomSource random, Predicate<@Nullable Direction> cullTest) {
        ClientRuns.Slot slot = ClientRuns.at(pos);
        if (slot == null) {
            wrapped.emitQuads(emitter, blockView, pos, state, random, cullTest);
            return;
        }
        TextureAtlasSprite sprite = plainRail
                ? RailSprites.plainRail()
                : wrapped.particleIcon();
        FabricQuads.emit(emitter, slot, sprite);
    }

    /** Smoothed geometry depends on the position, so it must never be shared through a geometry cache. */
    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random) {
        return ClientRuns.at(pos) != null ? null : wrapped.createGeometryKey(blockView, pos, state, random);
    }
}
