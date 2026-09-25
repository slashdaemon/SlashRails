package com.slashrails.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** An item with one translated tooltip line (MC <= 1.20.4: {@code appendHoverText(stack, level, ...)}). */
abstract class TooltipItem extends Item {

    private final String tooltipKey;

    TooltipItem(Properties properties, String tooltipKey) {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(com.slashrails.Texts.tr(tooltipKey));
    }
}
