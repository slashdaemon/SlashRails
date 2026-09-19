package com.slashrails.run;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The {@code SavedDataType} id is an Identifier (MC 26.1+). The default namespace keeps the file at
 * {@code data/slashrails_runs.dat}, where MC 1.21.11 and earlier wrote it.
 */
final class RunStoreType {

    private RunStoreType() {
    }

    static SavedDataType<SmoothRunRegistry> create(Codec<SmoothRunRegistry> codec) {
        return new SavedDataType<>(Identifier.withDefaultNamespace(SmoothRunRegistry.NAME), SmoothRunRegistry::new, codec, null);
    }
}
