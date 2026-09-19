package com.slashrails.item;

import com.slashrails.SlashRails;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Item instances, created and registered by each loader through {@link #create()}. */
public final class ModItems {

    public static final ResourceLocation TRACK_SMOOTHER_ID = SlashRails.id("track_smoother");

    public static Item trackSmoother;

    private ModItems() {
    }

    public static Item create() {
        trackSmoother = new TrackSmootherItem(new Item.Properties().stacksTo(1));
        return trackSmoother;
    }
}
