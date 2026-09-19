package com.slashrails.run;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/** Typed NBT reads with defaults (MC 1.21.5+: the getters return Optionals). */
final class Nbt {

    private Nbt() {
    }

    static int getInt(CompoundTag t, String key) {
        return t.getIntOr(key, 0);
    }

    static boolean getBoolean(CompoundTag t, String key) {
        return t.getBooleanOr(key, false);
    }

    static long[] getLongArray(CompoundTag t, String key) {
        return t.getLongArray(key).orElse(new long[0]);
    }

    static byte[] getByteArray(CompoundTag t, String key) {
        return t.getByteArray(key).orElse(new byte[0]);
    }

    static ListTag getCompoundList(CompoundTag t, String key) {
        return t.getListOrEmpty(key);
    }

    static CompoundTag compoundAt(ListTag list, int i) {
        return list.getCompoundOrEmpty(i);
    }
}
