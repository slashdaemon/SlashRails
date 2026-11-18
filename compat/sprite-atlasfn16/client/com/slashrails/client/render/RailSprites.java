package com.slashrails.client.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** Atlas UVs on the sprites the curved track uses (MC 1.20.1: sprite UVs in 0..16 pixels). */
public final class RailSprites {

    private RailSprites() {
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
