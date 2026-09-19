package com.slashrails.run;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Saved-data glue (MC 1.21.5+: {@code SavedDataType} + Codec). The codec wraps the same compound
 * the older versions write, so the file on disk has the same shape on every version.
 */
abstract class RunStore extends SavedData {

    private static final Codec<SmoothRunRegistry> CODEC =
            CompoundTag.CODEC.xmap(SmoothRunRegistry::readTag, r -> r.writeTag(new CompoundTag()));
    private static final SavedDataType<SmoothRunRegistry> TYPE = RunStoreType.create(CODEC);

    abstract CompoundTag writeTag(CompoundTag tag);

    static SmoothRunRegistry get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
