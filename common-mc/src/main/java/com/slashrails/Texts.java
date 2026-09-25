package com.slashrails;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Translatable text with the English string as fallback, so a client without SlashRails' lang (a
 * vanilla client that has no server resource pack) reads English instead of raw keys.
 */
public final class Texts {

    private static final Map<String, String> EN = load();

    private Texts() {
    }

    public static MutableComponent tr(String key, Object... args) {
        return Component.translatableWithFallback(key, EN.get(key), args);
    }

    private static Map<String, String> load() {
        Map<String, String> out = new HashMap<>();
        try (InputStream in = Texts.class.getResourceAsStream("/assets/slashrails/lang/en_us.json")) {
            if (in == null) return out;
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : json.entrySet()) out.put(e.getKey(), e.getValue().getAsString());
        } catch (Exception e) {
            SlashRails.LOG.warn("Could not read the English lang file; clients without it will see keys", e);
        }
        return out;
    }
}
