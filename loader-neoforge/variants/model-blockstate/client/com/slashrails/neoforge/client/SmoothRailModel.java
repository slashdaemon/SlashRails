package com.slashrails.neoforge.client;

import com.slashrails.client.ClientRuns;
import com.slashrails.client.render.RailSprites;
import com.slashrails.client.render.TrackMesh;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * (NeoForge 21.5+, MC 1.21.5+.) A rail block-state model that, for a rail on a smoothed run, returns
 * that rail's slice of the curved track as chunk geometry. The chunk compiler hands it the position,
 * so no model data is needed. Any other rail renders exactly as vanilla.
 */
final class SmoothRailModel extends DelegateBlockStateModel {

    private final boolean plainRail;

    SmoothRailModel(BlockStateModel original, boolean plainRail) {
        super(original);
        this.plainRail = plainRail;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts) {
        ClientRuns.Slot slot = ClientRuns.at(pos);
        if (slot == null) {
            super.collectParts(level, pos, state, random, parts);
            return;
        }
        TextureAtlasSprite sprite = plainRail ? RailSprites.plainRail() : delegate.particleIcon();
        List<TrackMesh.Quad> mesh = TrackMesh.forRail(slot.run(), slot.index());
        List<BakedQuad> quads = new ArrayList<>(mesh.size() * 2);
        for (TrackMesh.Quad q : mesh) {
            quads.add(bake(q, sprite, false));
            quads.add(bake(q, sprite, true));
        }
        parts.add(new Part(quads, sprite));
    }

    /** The curved slice: unculled quads only. */
    private record Part(List<BakedQuad> quads, TextureAtlasSprite particleIcon) implements BlockModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction side) {
            return side == null ? quads : List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }
    }

    private static BakedQuad bake(TrackMesh.Quad q, TextureAtlasSprite sprite, boolean underside) {
        QuadBakingVertexConsumer b = new QuadBakingVertexConsumer();
        b.setSprite(sprite);
        b.setDirection(underside ? Direction.DOWN : Direction.UP);
        b.setTintIndex(-1);
        b.setShade(true);
        float ny = underside ? -1 : 1;
        for (int k = 0; k < 4; k++) {
            int i = underside ? 3 - k : k;
            b.addVertex(q.x()[i], q.y()[i], q.z()[i])
                    .setColor(255, 255, 255, 255)
                    .setUv(sprite.getU(q.u()[i]), sprite.getV(q.v()[i]))
                    .setUv2(0, 0)
                    .setNormal(0, ny, 0);
        }
        return b.bakeQuad();
    }
}
