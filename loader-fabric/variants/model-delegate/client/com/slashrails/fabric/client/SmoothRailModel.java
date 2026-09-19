package com.slashrails.fabric.client;

import com.slashrails.client.ClientRuns;
import com.slashrails.client.render.RailSprites;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.DelegateBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * (Fabric API renderer 5.x, MC 1.21.4.) A rail model that, for a rail on a smoothed run, emits that
 * rail's slice of the curved track as chunk geometry. Any other rail renders exactly as vanilla.
 */
final class SmoothRailModel extends DelegateBakedModel {


    private final boolean plainRail;

    SmoothRailModel(BakedModel wrapped, boolean plainRail) {
        super(wrapped);
        this.plainRail = plainRail;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockState state, BlockPos pos,
                               Supplier<RandomSource> randomSupplier, Predicate<Direction> cullTest) {
        ClientRuns.Slot slot = ClientRuns.at(pos);
        if (slot == null) {
            parent.emitBlockQuads(emitter, blockView, state, pos, randomSupplier, cullTest);
            return;
        }
        TextureAtlasSprite sprite = plainRail
                ? RailSprites.plainRail()
                : parent.getParticleIcon();
        FabricQuads.emit(emitter, slot, sprite);
    }
}
