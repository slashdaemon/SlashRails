package com.slashrails.client.render;

import com.slashrails.Ids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;

/** Block-atlas sprites the curved track uses (MC 1.20.1: {@code Minecraft#getTextureAtlas}; sprite UVs in 0..16 pixels). */
public final class RailSprites {

    private RailSprites() {
    }

    /** The straight rail texture. The plain rail's corner states use the corner texture; a curve always wants this one. */
    public static TextureAtlasSprite plainRail() {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(Ids.minecraft("block/rail"));
    }

    /** Atlas U for a 0..1 position across the sprite. */
    public static float u(TextureAtlasSprite sprite, float f) {
        return sprite.getU(f * 16);
    }

    /** Atlas V for a 0..1 position along the sprite. */
    public static float v(TextureAtlasSprite sprite, float f) {
        return sprite.getV(f * 16);
    }
}
