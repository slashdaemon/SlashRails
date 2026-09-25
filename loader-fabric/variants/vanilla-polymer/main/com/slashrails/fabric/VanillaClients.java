package com.slashrails.fabric;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.slashrails.SlashRails;
import com.slashrails.item.ModItems;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Server-only mode (Polymer): clients without SlashRails may join. They see the Track Smoother as a
 * vanilla item carrying the mod's item model (served in the Polymer resource pack), never receive a
 * SlashRails registry entry or payload, and see the vanilla rails while carts follow the curve.
 */
final class VanillaClients {

    /** Send smoothed-run carts' positions every N ticks (vanilla minecarts: every 3). */
    static int cartSyncTicks = Integer.getInteger("slashrails.cartSyncTicks", 3);

    private VanillaClients() {
    }

    static void init() {
        PolymerItem.registerOverlay(ModItems.trackSmoother, new PolymerItem() {
            @Override
            public Item getPolymerItem(ItemStack stack, PacketContext context) {
                return Items.STICK;
            }
        });
        PolymerResourcePackUtils.addModAssets(SlashRails.MOD_ID);
        // Spike-only knob for measuring vanilla-client smoothness against update rate.
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, env) -> dispatcher.register(
                Commands.literal("slashrails-cartsync")
                        .requires(com.slashrails.Perms::isGamemaster)
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 3)).executes(c -> {
                            cartSyncTicks = IntegerArgumentType.getInteger(c, "ticks");
                            c.getSource().sendSuccess(() -> Component.literal("cartSyncTicks=" + cartSyncTicks), true);
                            return cartSyncTicks;
                        }))));
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, env) -> dispatcher.register(
                Commands.literal("slashrails-trace")
                        .requires(com.slashrails.Perms::isGamemaster)
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 72000)).executes(c -> {
                            String msg = SpikeTrace.start(c.getSource().getServer(), IntegerArgumentType.getInteger(c, "ticks"));
                            c.getSource().sendSuccess(() -> Component.literal(msg), true);
                            return 1;
                        }))));
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, env) -> dispatcher.register(
                Commands.literal("slashrails-netstat")
                        .requires(com.slashrails.Perms::isGamemaster)
                        .executes(c -> {
                            String msg = SpikeNetStat.show();
                            c.getSource().sendSuccess(() -> Component.literal(msg), false);
                            return 1;
                        })
                        .then(Commands.literal("reset").executes(c -> {
                            SpikeNetStat.reset();
                            c.getSource().sendSuccess(() -> Component.literal("reset"), false);
                            return 1;
                        }))));
        SpikeTrace.register();
        SlashRails.LOG.info("SlashRails server-only mode: clients without the mod may join (Polymer)");
    }

    static boolean requireClientMod() {
        return false;
    }

    /** A cart on a smoothed run moved this tick (server). */
    static void onCurveStep(AbstractMinecart cart) {
        // syncPosition makes ServerEntity send the position this tick (needsSync would add a motion packet).
        if (cartSyncTicks < 3 && cart.tickCount % cartSyncTicks == 0) cart.syncPosition = true;
    }
}
