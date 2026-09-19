package com.slashrails.neoforge;

import com.slashrails.SlashRails;
import com.slashrails.command.SlashRailsCommand;
import com.slashrails.item.ModItems;
import com.slashrails.net.RemoveRunPayload;
import com.slashrails.net.RunsPayload;
import com.slashrails.platform.Platform;
import com.slashrails.run.RunService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(SlashRails.MOD_ID)
public final class SlashRailsNeoForge {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SlashRails.MOD_ID);

    static {
        ITEMS.register(ModItems.TRACK_SMOOTHER_ID.getPath(), ModItems::create);
    }

    public SlashRailsNeoForge(IEventBus modBus) {
        Platform.set(new NeoForgePlatform());
        SlashRails.init();

        ITEMS.register(modBus);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, e -> {
            if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) e.accept(ModItems.trackSmoother);
        });
        modBus.addListener(RegisterPayloadHandlersEvent.class, SlashRailsNeoForge::registerPayloads);

        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> snapshot(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, e -> snapshot(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, e -> snapshot(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, e -> SlashRails.serverTick(e.getServer()));
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e -> SlashRailsCommand.register(e.getDispatcher()));
    }

    /** Required on both sides: the registrar is not optional, so clients without SlashRails are refused. */
    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(RunsPayload.TYPE, RunsPayload.CODEC, (payload, ctx) -> ClientPayloads.runs(payload));
        registrar.playToClient(RemoveRunPayload.TYPE, RemoveRunPayload.CODEC, (payload, ctx) -> ClientPayloads.remove(payload));
    }

    private static void snapshot(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer sp) RunService.sendSnapshot(sp);
    }
}
