package com.slashrails.client;

import com.slashrails.SlashRails;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/** Dev-only screenshots into {@code screenshots/} (MC <= 1.21.5). */
public final class Shots {

    private Shots() {
    }

    public static void grab(Minecraft mc, String fileName, String logTag) {
        Screenshot.grab(mc.gameDirectory, fileName, mc.getMainRenderTarget(),
                msg -> SlashRails.LOG.info("[{}] {}", logTag, msg.getString()));
    }
}
