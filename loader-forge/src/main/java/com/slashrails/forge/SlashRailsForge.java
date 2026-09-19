package com.slashrails.forge;

import com.slashrails.SlashRails;
import com.slashrails.command.SlashRailsCommand;
import com.slashrails.item.ModItems;
import com.slashrails.platform.Platform;
import com.slashrails.run.RunService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * MinecraftForge 1.20.1 entrypoint. Uses only Forge API up to 47.1.3, so the same jar also runs on
 * NeoForge 1.20.1 (47.1.x).
 */
@Mod(SlashRails.MOD_ID)
public final class SlashRailsForge {

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SlashRails.MOD_ID);
    private static final RegistryObject<Item> TRACK_SMOOTHER = ITEMS.register(ModItems.TRACK_SMOOTHER_ID.getPath(), ModItems::create);

    public SlashRailsForge() {
        Platform.set(new ForgePlatform());
        SlashRails.init();
        ForgeNet.register();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) e.accept(TRACK_SMOOTHER);
        });

        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> snapshot(e.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> snapshot(e.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> snapshot(e.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) SlashRails.serverTick(e.getServer());
        });
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> SlashRailsCommand.register(e.getDispatcher()));

        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.slashrails.forge.client.SlashRailsForgeClient.init(modBus);
        }
    }

    private static void snapshot(Player player) {
        if (player instanceof ServerPlayer sp) RunService.sendSnapshot(sp);
    }
}
