package com.pokewing.efmocap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small persisted knobs for scene behaviour. */
public final class Settings {
    private Settings() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Ticks a corpse lies there before it's down to bones (20 ticks = 1s). */
    public static int decayTicks = 600;
    /** How far you can reach to shoulder a body, in blocks. */
    public static double carryRange = 3.5;

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("efmocap").resolve("settings.json");
    }

    public static void save() {
        try {
            Files.createDirectories(file().getParent());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("decayTicks", decayTicks);
            m.put("carryRange", carryRange);
            try (Writer w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
                GSON.toJson(m, w);
            }
        } catch (Exception e) {
            EFMocap.LOG.warn("[efmocap] failed to save settings", e);
        }
    }

    public static void load() {
        try {
            if (!Files.exists(file())) return;
            try (Reader r = Files.newBufferedReader(file(), StandardCharsets.UTF_8)) {
                Map<String, Object> m = GSON.fromJson(r,
                        new TypeToken<Map<String, Object>>() {}.getType());
                if (m == null) return;
                if (m.get("decayTicks") instanceof Number n) decayTicks = n.intValue();
                if (m.get("carryRange") instanceof Number n) carryRange = n.doubleValue();
            }
        } catch (Exception e) {
            EFMocap.LOG.warn("[efmocap] failed to load settings", e);
        }
    }
}
