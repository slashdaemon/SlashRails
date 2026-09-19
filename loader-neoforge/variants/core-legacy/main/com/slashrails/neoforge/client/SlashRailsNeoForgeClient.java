package com.slashrails.neoforge.client;

import com.slashrails.client.ClientRuns;
import com.slashrails.client.VisualTest;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;

/** Client-side wiring; only reached from the entrypoint's Dist.CLIENT branch. */
public final class SlashRailsNeoForgeClient {

    private SlashRailsNeoForgeClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((ModelEvent.ModifyBakingResult e) -> RailModels.wrap(e));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientRuns.clear());
        NeoForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) VisualTest.tick(Minecraft.getInstance());
        });
        OverlayHook.register();
    }
}
