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
}
