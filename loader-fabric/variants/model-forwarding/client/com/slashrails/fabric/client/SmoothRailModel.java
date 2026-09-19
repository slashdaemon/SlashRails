package com.slashrails.fabric.client;

import com.slashrails.client.ClientRuns;
import com.slashrails.client.render.RailSprites;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * (Fabric API renderer 3.x-4.x, MC <= 1.21.3.) A rail model that, for a rail on a smoothed run, emits that rail's slice of the curved track
 * (Fabric Renderer API, so it is chunk geometry — batched with terrain and Sodium/Iris-safe).
 * Any other rail renders exactly as vanilla.
 */
final class SmoothRailModel extends ForwardingBakedModel {


    private final boolean plainRail;

    SmoothRailModel(BakedModel wrapped, boolean plainRail) {
        this.wrapped = wrapped;
        this.plainRail = plainRail;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos,
                               Supplier<RandomSource> randomSupplier, RenderContext context) {
        ClientRuns.Slot slot = ClientRuns.at(pos);
        if (slot == null) {
            super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
            return;
        }
        // The plain rail's corner states use the corner texture; a curve always wants the straight one.
        TextureAtlasSprite sprite = plainRail
                ? RailSprites.plainRail()
                : wrapped.getParticleIcon();
        FabricQuads.emit(context.getEmitter(), slot, sprite);
    }
}
