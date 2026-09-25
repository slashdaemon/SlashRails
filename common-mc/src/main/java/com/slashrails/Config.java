package com.slashrails;

import com.slashrails.platform.Platform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * {@code config/slashrails.properties}. Server keys apply on the server; client keys on the client.
 * Missing keys are written back with their defaults.
 */
public final class Config {

    /** Server: longest run the Track Smoother will smooth, in rails. */
    public static int maxRunLength = 512;
    /** Server: only operators may use the Track Smoother. */
    public static boolean opOnlyTool = false;
    /** Client: draw the smoothed centre line over every run (debugging aid). */
    public static boolean debugOverlay = false;
    /** Client: preview the run the Track Smoother would smooth while holding it. */
    public static boolean toolPreview = true;
    /** Server, server-only builds: let clients without SlashRails join (they see vanilla rails). */
    public static boolean allowVanillaClients = true;
    /** Server, server-only builds: send smoothed-run carts' positions every N ticks (1-3; vanilla: 3). */
    public static int cartSyncTicks = 1;

    private Config() {
    }

    static void load() {
        Path file = Platform.get().configDir().resolve("slashrails.properties");
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                p.load(r);
            } catch (IOException e) {
                SlashRails.LOG.warn("Could not read {}, using defaults", file, e);
            }
        }
        maxRunLength = Math.max(3, Math.min(4096, intOf(p, "maxRunLength", maxRunLength)));
        opOnlyTool = boolOf(p, "opOnlyTool", opOnlyTool);
        debugOverlay = boolOf(p, "debugOverlay", debugOverlay);
        toolPreview = boolOf(p, "toolPreview", toolPreview);
        boolean serverOnly = Platform.get().supportsVanillaClients();
        if (serverOnly) {
            allowVanillaClients = boolOf(p, "allowVanillaClients", allowVanillaClients);
            cartSyncTicks = Math.max(1, Math.min(3, intOf(p, "cartSyncTicks", cartSyncTicks)));
        }

        p.setProperty("maxRunLength", Integer.toString(maxRunLength));
        p.setProperty("opOnlyTool", Boolean.toString(opOnlyTool));
        p.setProperty("debugOverlay", Boolean.toString(debugOverlay));
        p.setProperty("toolPreview", Boolean.toString(toolPreview));
        if (serverOnly) {
            p.setProperty("allowVanillaClients", Boolean.toString(allowVanillaClients));
            p.setProperty("cartSyncTicks", Integer.toString(cartSyncTicks));
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                p.store(w, "SlashRails. maxRunLength/opOnlyTool/allowVanillaClients/cartSyncTicks: server. debugOverlay/toolPreview: client.");
            }
        } catch (IOException e) {
            SlashRails.LOG.warn("Could not write {}", file, e);
        }
    }

    private static int intOf(Properties p, String key, int def) {
        try {
            return Integer.parseInt(p.getProperty(key, Integer.toString(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean boolOf(Properties p, String key, boolean def) {
        return Boolean.parseBoolean(p.getProperty(key, Boolean.toString(def)).trim());
    }
}
