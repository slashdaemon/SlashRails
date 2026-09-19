package com.slashrails;

import net.minecraft.resources.ResourceLocation;

/** Resource ids (MC 1.21+: factory methods). */
public final class Ids {

    private Ids() {
    }

    public static ResourceLocation of(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static ResourceLocation minecraft(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
