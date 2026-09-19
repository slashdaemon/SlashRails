package com.slashrails.neoforge;

import com.slashrails.SlashRails;
import com.slashrails.command.SlashRailsCommand;
import com.slashrails.item.ModItems;
import com.slashrails.platform.Platform;
import com.slashrails.run.RunService;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** NeoForge 20.2 / 20.4 entrypoint (tick events with phases, no client-only @Mod classes yet). */
@Mod(SlashRails.MOD_ID)
public final class SlashRailsNeoForge {

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, SlashRails.MOD_ID);

    static {
        ITEMS.register(ModItems.TRACK_SMOOTHER_ID.getPath(), ModItems::create);
    }

    public SlashRailsNeoForge(IEventBus modBus) {
        Platform.set(new NeoForgePlatform());
        SlashRails.init();

        ITEMS.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) e.accept(ModItems.trackSmoother);
        });
        NeoForgeNet.register(modBus);

        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> snapshot(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> snapshot(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> snapshot(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) SlashRails.serverTick(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> SlashRailsCommand.register(e.getDispatcher()));

        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.slashrails.neoforge.client.SlashRailsNeoForgeClient.init(modBus);
        }
    }

    private static void snapshot(Player player) {
        if (player instanceof ServerPlayer sp) RunService.sendSnapshot(sp);
    }
}
