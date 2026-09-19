package com.slashrails.run;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Saved-data glue (MC 1.20.5 - 1.21.4: {@code SavedData.Factory} with a registry lookup). */
abstract class RunStore extends SavedData {

    private static final Factory<SmoothRunRegistry> FACTORY =
            new Factory<>(SmoothRunRegistry::new, (tag, registries) -> SmoothRunRegistry.readTag(tag), null);

    abstract CompoundTag writeTag(CompoundTag tag);

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return writeTag(tag);
    }

    static SmoothRunRegistry get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, SmoothRunRegistry.NAME);
    }
}
