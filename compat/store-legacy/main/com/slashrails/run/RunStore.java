package com.slashrails.run;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Saved-data glue (MC 1.20.1: {@code computeIfAbsent(load, create, name)}, no registry lookup). */
abstract class RunStore extends SavedData {

    abstract CompoundTag writeTag(CompoundTag tag);

    @Override
    public CompoundTag save(CompoundTag tag) {
        return writeTag(tag);
    }

    static SmoothRunRegistry get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SmoothRunRegistry::readTag, SmoothRunRegistry::new, SmoothRunRegistry.NAME);
    }
}
