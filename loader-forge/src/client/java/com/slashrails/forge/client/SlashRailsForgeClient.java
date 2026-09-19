package com.slashrails.forge.client;

import com.slashrails.client.ClientRuns;
import com.slashrails.client.VisualTest;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Client-side wiring; only reached from the entrypoint's Dist.CLIENT branch. */
public final class SlashRailsForgeClient {

    private SlashRailsForgeClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((ModelEvent.ModifyBakingResult e) -> RailModels.wrap(e));
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientRuns.clear());
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) VisualTest.tick(Minecraft.getInstance());
        });
        OverlayHook.register();
    }
}
