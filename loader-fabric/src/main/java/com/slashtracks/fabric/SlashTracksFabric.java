package com.slashtracks.fabric;

import com.slashtracks.SlashTracks;
import com.slashtracks.command.SlashTracksCommand;
import com.slashtracks.item.ModItems;
import com.slashtracks.net.RemoveRunPayload;
import com.slashtracks.net.RunsPayload;
import com.slashtracks.platform.Platform;
import com.slashtracks.run.RunService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTabs;

public final class SlashTracksFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Platform.set(new FabricPlatform());
        SlashTracks.init();

        Registry.register(BuiltInRegistries.ITEM, ModItems.TRACK_SMOOTHER_ID, ModItems.create());
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(ModItems.trackSmoother));

        PayloadTypeRegistry.playS2C().register(RunsPayload.TYPE, RunsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RemoveRunPayload.TYPE, RemoveRunPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!ServerPlayNetworking.canSend(handler.player, RunsPayload.TYPE)) {
                // Plain text: a client without the mod has no translation for it.
                handler.disconnect(Component.literal("This server uses SlashTracks. Install SlashTracks "
                        + FabricPlatform.modVersion() + " on your client to join."));
                return;
            }
            RunService.sendSnapshot(handler.player);
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, from, to) -> RunService.sendSnapshot(player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> RunService.sendSnapshot(newPlayer));
        ServerTickEvents.END_SERVER_TICK.register(SlashTracks::serverTick);
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, env) -> SlashTracksCommand.register(dispatcher));
    }
}
