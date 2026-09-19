package com.slashrails.run;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Typed NBT reads with defaults (MC <= 1.21.4: the getters return plain values). */
final class Nbt {

    private Nbt() {
    }

    static int getInt(CompoundTag t, String key) {
        return t.getInt(key);
    }

    static boolean getBoolean(CompoundTag t, String key) {
        return t.getBoolean(key);
    }

    static long[] getLongArray(CompoundTag t, String key) {
        return t.getLongArray(key);
    }

    static byte[] getByteArray(CompoundTag t, String key) {
        return t.getByteArray(key);
    }

    static ListTag getCompoundList(CompoundTag t, String key) {
        return t.getList(key, Tag.TAG_COMPOUND);
    }

    static CompoundTag compoundAt(ListTag list, int i) {
        return list.getCompound(i);
    }
}
