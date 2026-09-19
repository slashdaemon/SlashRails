package com.slashrails.fabric.client;

import com.slashrails.client.ClientRuns;
import com.slashrails.client.render.TrackMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * A rail model that, for a rail on a smoothed run, emits that rail's slice of the curved track
 * (Fabric Renderer API, so it is chunk geometry — batched with terrain and Sodium/Iris-safe).
 * Any other rail renders exactly as vanilla.
 */
final class SmoothRailModel extends ForwardingBakedModel {

    private static final ResourceLocation PLAIN_RAIL = ResourceLocation.withDefaultNamespace("block/rail");

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
                ? Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(PLAIN_RAIL)
                : wrapped.getParticleIcon();
        QuadEmitter e = context.getEmitter();
        for (TrackMesh.Quad q : TrackMesh.forRail(slot.run(), slot.index())) {
            emit(e, q, sprite, false);
            emit(e, q, sprite, true);
        }
    }

    private static void emit(QuadEmitter e, TrackMesh.Quad q, TextureAtlasSprite sprite, boolean underside) {
        for (int k = 0; k < 4; k++) {
            int i = underside ? 3 - k : k;
            e.pos(k, q.x()[i], q.y()[i], q.z()[i]);
            e.uv(k, sprite.getU(q.u()[i]), sprite.getV(q.v()[i]));
            e.color(k, -1);
        }
        e.nominalFace(underside ? Direction.DOWN : Direction.UP);
        e.cullFace(null);
        e.emit();
    }
}
