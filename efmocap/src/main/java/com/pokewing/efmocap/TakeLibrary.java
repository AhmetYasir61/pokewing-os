package com.pokewing.efmocap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Named takes, persisted to {@code config/efmocap/takes/<name>.json} so a scene
 * survives a restart and can be re-layered across sessions.
 */
public final class TakeLibrary {
    public static final TakeLibrary INSTANCE = new TakeLibrary();
    private static final Gson GSON = new GsonBuilder().create();

    private final Map<String, MocapRecording> takes = new LinkedHashMap<>();

    private TakeLibrary() {}

    public static Path dir() {
        return FMLPaths.CONFIGDIR.get().resolve("efmocap").resolve("takes");
    }

    public List<String> names() { return new ArrayList<>(takes.keySet()); }
    public int count() { return takes.size(); }
    public MocapRecording get(String name) { return takes.get(name); }
    public java.util.Collection<MocapRecording> all() { return takes.values(); }

    /** Next free sequential name: take1, take2, ... */
    public String nextName() {
        int i = 1;
        while (takes.containsKey("take" + i)) i++;
        return "take" + i;
    }

    public void add(MocapRecording rec) {
        if (rec == null || rec.isEmpty()) return;
        takes.put(rec.name, rec);
        save(rec);
    }

    public boolean remove(String name) {
        MocapRecording r = takes.remove(name);
        if (r == null) return false;
        try { Files.deleteIfExists(dir().resolve(name + ".json")); } catch (Exception ignored) {}
        return true;
    }

    public void clear() {
        for (String n : names()) remove(n);
    }

    public void save(MocapRecording rec) {
        try {
            Files.createDirectories(dir());
            Path p = dir().resolve(rec.name + ".json");
            try (Writer w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
                GSON.toJson(rec, w);
            }
            EFMocap.LOG.info("[efmocap] saved take '{}' ({} frames)", rec.name, rec.length());
        } catch (Exception e) {
            EFMocap.LOG.warn("[efmocap] failed to save take {}", rec.name, e);
        }
    }

    /** Load every take from disk (called once on world join). */
    public int loadAll() {
        int n = 0;
        Path d = dir();
        if (!Files.isDirectory(d)) return 0;
        try (Stream<Path> s = Files.list(d)) {
            for (Path p : s.filter(x -> x.toString().endsWith(".json")).toList()) {
                try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                    MocapRecording rec = GSON.fromJson(r, MocapRecording.class);
                    if (rec != null && rec.name != null && !rec.isEmpty()) {
                        takes.put(rec.name, rec);
                        n++;
                    }
                } catch (Exception e) {
                    EFMocap.LOG.warn("[efmocap] bad take file {}", p, e);
                }
            }
        } catch (Exception e) {
            EFMocap.LOG.warn("[efmocap] failed to list takes", e);
        }
        return n;
    }
}
