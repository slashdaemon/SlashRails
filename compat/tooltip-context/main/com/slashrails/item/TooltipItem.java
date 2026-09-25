package com.slashrails.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** An item with one translated tooltip line (MC 1.20.5 - 1.21.4: {@code TooltipContext} + list). */
abstract class TooltipItem extends Item {

    private final String tooltipKey;

    TooltipItem(Properties properties, String tooltipKey) {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(com.slashrails.Texts.tr(tooltipKey));
    }
}
