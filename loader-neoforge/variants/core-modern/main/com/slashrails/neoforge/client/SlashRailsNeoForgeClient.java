package com.slashrails.neoforge.client;

import com.slashrails.SlashRails;
import com.slashrails.client.ClientRuns;
import com.slashrails.client.VisualTest;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;


@Mod(value = SlashRails.MOD_ID, dist = Dist.CLIENT)
public final class SlashRailsNeoForgeClient {

    public SlashRailsNeoForgeClient(IEventBus modBus) {
        modBus.addListener(ModelEvent.ModifyBakingResult.class, RailModels::wrap);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> ClientRuns.clear());
        OverlayHook.register();
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> VisualTest.tick(Minecraft.getInstance()));
    }
}
