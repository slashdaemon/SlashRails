package com.slashrails.client.render;

import com.slashrails.Ids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;

/** Block-atlas sprites the curved track uses (MC 1.21.9+: {@code AtlasManager}). */
public final class RailSprites {

    private RailSprites() {
    }

    /** The straight rail texture. The plain rail's corner states use the corner texture; a curve always wants this one. */
    public static TextureAtlasSprite plainRail() {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(Ids.minecraft("block/rail"));
    }

    /** Atlas U for a 0..1 position across the sprite. */
    public static float u(TextureAtlasSprite sprite, float f) {
        return sprite.getU(f);
    }

    /** Atlas V for a 0..1 position along the sprite. */
    public static float v(TextureAtlasSprite sprite, float f) {
        return sprite.getV(f);
    }
}
