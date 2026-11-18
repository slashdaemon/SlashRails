package com.slashrails.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.jetbrains.annotations.Nullable;

/**
 * The texture a smoothed rail is drawn with: the one its straight shape uses. A rail's corner models
 * (and their particle sprite) use the curved texture, vanilla's plain rail and modded rails alike
 * (Railcraft's corner models take their particle from the corner texture), but a curve is drawn as
 * straight track bent along the path.
 */
public final class StraightRail {

    private StraightRail() {
    }

    /** The particle sprite of {@code state}'s north-south shape, or {@code fallback} if it has none. */
    public static TextureAtlasSprite sprite(@Nullable BlockState state, TextureAtlasSprite fallback) {
        BlockState straight = state(state);
        if (straight == null) return fallback;
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getBlockModelShaper().getParticleIcon(straight);
        return sprite != null ? sprite : fallback;
    }

    /** {@code state} with its rail shape set to north-south (other properties kept), or null if not a rail. */
    @Nullable
    static BlockState state(@Nullable BlockState state) {
        if (state == null || !(state.getBlock() instanceof BaseRailBlock rail)) return null;
        Property<RailShape> shape = rail.getShapeProperty();
        if (!state.hasProperty(shape) || !shape.getPossibleValues().contains(RailShape.NORTH_SOUTH)) return null;
        return state.setValue(shape, RailShape.NORTH_SOUTH);
    }
}
