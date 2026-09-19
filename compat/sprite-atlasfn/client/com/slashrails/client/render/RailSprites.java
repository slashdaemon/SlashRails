package com.slashrails.client.render;

import com.slashrails.Ids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;

/** Block-atlas sprites the curved track uses (MC ≤ 1.21.8: {@code Minecraft#getTextureAtlas}). */
public final class RailSprites {

    private RailSprites() {
    }

    /** The straight rail texture. The plain rail's corner states use the corner texture; a curve always wants this one. */
    public static TextureAtlasSprite plainRail() {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(Ids.minecraft("block/rail"));
    }
}
