package com.slashrails;

import net.minecraft.resources.ResourceLocation;

/** Resource ids (MC ≤ 1.20.6: constructors). */
public final class Ids {

    private Ids() {
    }

    public static ResourceLocation of(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation minecraft(String path) {
        return new ResourceLocation("minecraft", path);
    }
}
