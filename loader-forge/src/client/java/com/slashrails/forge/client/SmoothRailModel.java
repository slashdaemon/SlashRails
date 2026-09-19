package com.slashrails.forge.client;

import com.slashrails.client.ClientRuns;
import com.slashrails.client.render.RailSprites;
import com.slashrails.client.render.TrackMesh;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * (MC 1.20.x: buffered quad baker, vertex/endVertex chain.) A rail model that, for a rail on a smoothed run, returns that rail's slice of the curved track as
 * chunk geometry. The chunk compiler asks {@link #getModelData} for every block, which is where the
 * position-dependent slot is looked up. Any other rail renders exactly as vanilla.
 */
final class SmoothRailModel extends BakedModelWrapper<BakedModel> {

    private static final ModelProperty<ClientRuns.Slot> SLOT = new ModelProperty<>();

    private final boolean plainRail;

    SmoothRailModel(BakedModel original, boolean plainRail) {
        super(original);
        this.plainRail = plainRail;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        ClientRuns.Slot slot = ClientRuns.at(pos);
        if (slot == null) return super.getModelData(level, pos, state, modelData);
        return ModelData.builder().with(SLOT, slot).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData data, @Nullable RenderType renderType) {
        ClientRuns.Slot slot = data.get(SLOT);
        if (slot == null) return super.getQuads(state, side, rand, data, renderType);
        if (side != null) return List.of();
        // The plain rail's corner states use the corner texture; a curve always wants the straight one.
        TextureAtlasSprite sprite = plainRail
                ? RailSprites.plainRail()
                : originalModel.getParticleIcon();
        List<TrackMesh.Quad> mesh = TrackMesh.forRail(slot.run(), slot.index());
        List<BakedQuad> out = new ArrayList<>(mesh.size() * 2);
        for (TrackMesh.Quad q : mesh) {
            out.add(bake(q, sprite, false));
            out.add(bake(q, sprite, true));
        }
        return out;
    }

    private static BakedQuad bake(TrackMesh.Quad q, TextureAtlasSprite sprite, boolean underside) {
        QuadBakingVertexConsumer.Buffered b = new QuadBakingVertexConsumer.Buffered();
        b.setSprite(sprite);
        b.setDirection(underside ? Direction.DOWN : Direction.UP);
        b.setTintIndex(-1);
        b.setShade(true);
        b.setHasAmbientOcclusion(true);
        float ny = underside ? -1 : 1;
        for (int k = 0; k < 4; k++) {
            int i = underside ? 3 - k : k;
            b.vertex(q.x()[i], q.y()[i], q.z()[i])
                    .color(255, 255, 255, 255)
                    .uv(sprite.getU(q.u()[i]), sprite.getV(q.v()[i]))
                    .uv2(0, 0)
                    .normal(0, ny, 0)
                    .endVertex();
        }
        return b.getQuad();
    }
}
