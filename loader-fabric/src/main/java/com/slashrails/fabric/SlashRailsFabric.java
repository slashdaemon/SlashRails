package com.slashrails.fabric;

import com.slashrails.SlashRails;
import com.slashrails.command.SlashRailsCommand;
import com.slashrails.item.ModItems;
import com.slashrails.platform.Platform;
import com.slashrails.run.RunService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTabs;

public final class SlashRailsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Platform.set(new FabricPlatform());
        SlashRails.init();

        Registry.register(BuiltInRegistries.ITEM, ModItems.TRACK_SMOOTHER_ID, ModItems.create());
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(ModItems.trackSmoother));

        FabricNet.register();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!FabricNet.canSend(handler.player)) {
                // Plain text: a client without the mod has no translation for it.
                handler.disconnect(Component.literal("This server uses SlashRails. Install SlashRails "
                        + FabricPlatform.modVersion() + " on your client to join."));
                return;
            }
            RunService.sendSnapshot(handler.player);
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, from, to) -> RunService.sendSnapshot(player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> RunService.sendSnapshot(newPlayer));
        ServerTickEvents.END_SERVER_TICK.register(SlashRails::serverTick);
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, env) -> SlashRailsCommand.register(dispatcher));
    }
}
