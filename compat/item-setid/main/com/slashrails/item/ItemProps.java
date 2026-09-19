package com.slashrails.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Item properties for a new item (MC 1.21.2+: every item must be constructed with its id). */
final class ItemProps {

    private ItemProps() {
    }

    static Item.Properties of(ResourceLocation id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }
}
