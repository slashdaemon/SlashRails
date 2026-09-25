package com.slashrails.fabric;

import com.slashrails.Config;
import com.slashrails.SlashRails;
import com.slashrails.Texts;
import com.slashrails.item.ModItems;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

/**
 * Server-only builds (Polymer): clients without SlashRails may join. They see the Track Smoother as a
 * vanilla item carrying the mod's item model (served in Polymer's resource pack), never receive a
 * SlashRails registry entry or payload, and see the vanilla rails while carts follow the curve.
 * Clients with the mod keep the full experience.
 */
final class VanillaClients {

    static final boolean SUPPORTED = true;

    private VanillaClients() {
    }

    static void init() {
        // Every client gets the vanilla representation; modded clients also have the model locally.
        PolymerItem.registerOverlay(ModItems.trackSmoother, new PolymerItem() {
            @Override
            public Item getPolymerItem(ItemStack stack, PacketContext context) {
                return Items.STICK;
            }

            @Override
            public ItemStack getPolymerItemStack(ItemStack stack, TooltipFlag flag, PacketContext context, HolderLookup.Provider lookup) {
                ItemStack out = PolymerItem.super.getPolymerItemStack(stack, flag, context, lookup);
                // Polymer names the stack from the translation key; add the English fallback for clients without the pack.
                if (!out.has(DataComponents.CUSTOM_NAME)) out.set(DataComponents.ITEM_NAME, Texts.tr("item.slashrails.track_smoother"));
                return out;
            }

            @Override
            public Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
                // Without the server pack the mod's model is missing: keep the stick's own model.
                return PolymerResourcePackUtils.hasMainPack(context) ? PolymerItem.super.getPolymerItemModel(stack, context, lookup) : null;
            }

        });
        PolymerResourcePackUtils.addModAssets(SlashRails.MOD_ID);
        if (Config.allowVanillaClients) {
            // Without the pack a vanilla client would see a missing model and raw translation keys.
            PolymerResourcePackUtils.markAsRequired();
            SlashRails.LOG.info("SlashRails: clients without the mod may join (Polymer); cart updates every {} tick(s)", Config.cartSyncTicks);
        }
    }

    static boolean requireClientMod() {
        return !Config.allowVanillaClients;
    }

    /** A cart on a smoothed run moved this tick (server). Vanilla minecarts send positions every 3 ticks. */
    static void onCurveStep(AbstractMinecart cart) {
        // syncPosition makes ServerEntity send the position this tick (needsSync would add a motion packet).
        if (Config.cartSyncTicks < 3 && cart.tickCount % Config.cartSyncTicks == 0) cart.syncPosition = true;
    }
}
