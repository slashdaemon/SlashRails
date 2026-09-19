package com.slashrails.run;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.saveddata.SavedDataType;

/** The {@code SavedDataType} id is a plain file name (MC 1.21.5 - 1.21.11). */
final class RunStoreType {

    private RunStoreType() {
    }

    static SavedDataType<SmoothRunRegistry> create(Codec<SmoothRunRegistry> codec) {
        return new SavedDataType<>(SmoothRunRegistry.NAME, SmoothRunRegistry::new, codec, null);
    }
}
