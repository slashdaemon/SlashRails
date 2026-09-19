package com.slashrails.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Item properties for a new item (MC ≤ 1.21.1: an item does not carry its own id). */
final class ItemProps {

    private ItemProps() {
    }

    static Item.Properties of(ResourceLocation id) {
        return new Item.Properties();
    }
}
