package com.slashrails.fabric;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Spike-only: packets sent to all players, by type, since the last reset. */
public final class SpikeNetStat {

    private static final Map<String, AtomicLong> COUNTS = new ConcurrentHashMap<>();
    private static long since = System.nanoTime();

    private SpikeNetStat() {
    }

    public static void count(Packet<?> packet) {
        if (packet instanceof ClientboundBundlePacket bundle) {
            for (Packet<?> p : bundle.subPackets()) count(p);
            return;
        }
        COUNTS.computeIfAbsent(packet.type().id().getPath(), k -> new AtomicLong()).incrementAndGet();
    }

    static void reset() {
        COUNTS.clear();
        since = System.nanoTime();
    }

    static String show() {
        double secs = (System.nanoTime() - since) / 1e9;
        StringBuilder sb = new StringBuilder(String.format(java.util.Locale.ROOT, "%.1fs:", secs));
        new TreeMap<>(COUNTS).forEach((k, v) -> {
            if (k.contains("entity") || k.contains("move") || k.contains("teleport") || k.contains("motion"))
                sb.append(String.format(java.util.Locale.ROOT, " %s=%.2f/s", k, v.get() / secs));
        });
        return sb.toString();
    }
}
