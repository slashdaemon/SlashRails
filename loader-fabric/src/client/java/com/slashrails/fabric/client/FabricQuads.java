package com.slashrails.fabric.client;

import com.slashrails.client.ClientRuns;
import com.slashrails.client.render.TrackMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/** Emits a smoothed rail's slice of curved track through the Fabric Renderer API. */
final class FabricQuads {

    private FabricQuads() {
    }

    /** Both faces of every quad of the slot's slice, textured with {@code sprite}. */
    static void emit(QuadEmitter e, ClientRuns.Slot slot, TextureAtlasSprite sprite) {
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
