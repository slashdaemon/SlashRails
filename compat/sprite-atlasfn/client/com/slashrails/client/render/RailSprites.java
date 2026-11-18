package com.slashrails.client.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** Atlas UVs on the sprites the curved track uses (MC 1.20.2+: atlas UVs from 0..1 fractions). */
public final class RailSprites {

    private RailSprites() {
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
