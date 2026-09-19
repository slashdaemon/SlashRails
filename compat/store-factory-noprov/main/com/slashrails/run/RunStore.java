package com.slashrails.run;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Saved-data glue (MC 1.20.2 - 1.20.4: {@code SavedData.Factory}, no registry lookup yet). */
abstract class RunStore extends SavedData {

    private static final Factory<SmoothRunRegistry> FACTORY =
            new Factory<>(SmoothRunRegistry::new, SmoothRunRegistry::readTag, null);

    abstract CompoundTag writeTag(CompoundTag tag);

    @Override
    public CompoundTag save(CompoundTag tag) {
        return writeTag(tag);
    }

    static SmoothRunRegistry get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, SmoothRunRegistry.NAME);
    }
}
