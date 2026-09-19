package com.slashrails.neoforge.client;

import com.slashrails.client.ClientRuns;
import com.slashrails.client.render.RailSprites;
import com.slashrails.client.render.TrackMesh;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * (NeoForge 26.1+, MC 26.x.) A rail block-state model that, for a rail on a smoothed run, returns that
 * rail's slice of the curved track as chunk geometry. The chunk compiler hands it the position, so
 * no model data is needed. Any other rail renders exactly as vanilla.
 */
final class SmoothRailModel extends DelegateBlockStateModel {

    private final boolean plainRail;

    SmoothRailModel(BlockStateModel original, boolean plainRail) {
        super(original);
        this.plainRail = plainRail;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        ClientRuns.Slot slot = ClientRuns.at(pos);
        if (slot == null) {
            super.collectParts(level, pos, state, random, parts);
            return;
        }
        Material.Baked particle = delegate.particleMaterial();
        Material.Baked material = plainRail ? new Material.Baked(RailSprites.plainRail(), false) : particle;
        TextureAtlasSprite sprite = material.sprite();
        List<TrackMesh.Quad> mesh = TrackMesh.forRail(slot.run(), slot.index());
        List<BakedQuad> quads = new ArrayList<>(mesh.size() * 2);
        for (TrackMesh.Quad q : mesh) {
            quads.add(bake(q, material, sprite, false));
            quads.add(bake(q, material, sprite, true));
        }
        parts.add(new Part(quads, particle, delegate.materialFlags()));
    }

    /** The curved slice: unculled quads only. */
    private record Part(List<BakedQuad> quads, Material.Baked particleMaterial, int materialFlags) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction side) {
            return side == null ? quads : List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }
    }

    private static BakedQuad bake(TrackMesh.Quad q, Material.Baked material, TextureAtlasSprite sprite, boolean underside) {
        QuadBakingVertexConsumer b = new QuadBakingVertexConsumer();
        b.setSprite(material);
        b.setDirection(underside ? Direction.DOWN : Direction.UP);
        b.setTintIndex(-1);
        b.setShade(true);
        float ny = underside ? -1 : 1;
        for (int k = 0; k < 4; k++) {
            int i = underside ? 3 - k : k;
            b.addVertex(q.x()[i], q.y()[i], q.z()[i])
                    .setColor(255, 255, 255, 255)
                    .setUv(RailSprites.u(sprite, q.u()[i]), RailSprites.v(sprite, q.v()[i]))
                    .setUv2(0, 0)
                    .setNormal(0, ny, 0);
        }
        return b.bakeQuad();
    }
}
