package com.pokewing.efmocap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Characters plus their skin textures. Skins are plain PNGs dropped into
 * {@code config/efmocap/skins}; they're loaded lazily on the render thread and
 * registered as dynamic textures.
 */
public final class CharacterLibrary {
    public static final CharacterLibrary INSTANCE = new CharacterLibrary();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, Character> characters = new LinkedHashMap<>();
    private final Map<String, ResourceLocation> textures = new HashMap<>();
    /** Character used for the next recording. */
    public String active = "";

    private CharacterLibrary() {}

    public static Path root() { return FMLPaths.CONFIGDIR.get().resolve("efmocap"); }
    public static Path skinsDir() { return root().resolve("skins"); }
    public static Path attachmentsDir() { return root().resolve("attachments"); }
    private static Path file() { return root().resolve("characters.json"); }

    public List<String> names() { return new ArrayList<>(characters.keySet()); }
    public int count() { return characters.size(); }
    public Character get(String name) { return name == null ? null : characters.get(name); }
    public Character activeCharacter() { return get(active); }

    public Character create(String name) {
        Character c = new Character(name);
        characters.put(name, c);
        if (active.isEmpty()) active = name;
        save();
        return c;
    }

    public boolean remove(String name) {
        boolean ok = characters.remove(name) != null;
        if (ok) {
            if (active.equals(name)) active = characters.isEmpty() ? "" : names().get(0);
            save();
        }
        return ok;
    }

    public void put(Character c) {
        characters.put(c.name, c);
        save();
    }

    /** PNG files available to assign as skins. */
    public List<String> availableSkins() {
        List<String> out = new ArrayList<>();
        Path d = skinsDir();
        if (!Files.isDirectory(d)) return out;
        try (Stream<Path> s = Files.list(d)) {
            s.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".png"))
             .forEach(p -> out.add(p.getFileName().toString()));
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Texture for a skin file, loading and registering it on first use.
     * Returns null for the default skin (caller falls back to vanilla).
     * Must be called on the render thread.
     */
    public ResourceLocation texture(String pngName) {
        if (pngName == null || pngName.isEmpty()) return null;
        ResourceLocation cached = textures.get(pngName);
        if (cached != null) return cached;

        Path p = skinsDir().resolve(pngName);
        if (!Files.isRegularFile(p)) p = attachmentsDir().resolve(pngName);
        if (!Files.isRegularFile(p)) return null;

        try (InputStream in = Files.newInputStream(p)) {
            NativeImage img = NativeImage.read(in);
            DynamicTexture tex = new DynamicTexture(img);
            String safe = pngName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
            ResourceLocation rl = new ResourceLocation(EFMocap.MOD_ID, "skins/" + safe);
            Minecraft.getInstance().getTextureManager().register(rl, tex);
            textures.put(pngName, rl);
            EFMocap.LOG.info("[efmocap] loaded texture {}", pngName);
            return rl;
        } catch (Exception e) {
            EFMocap.LOG.warn("[efmocap] could not load texture {}", pngName, e);
            textures.put(pngName, null);
            return null;
        }
    }

    // --- persistence -----------------------------------------------------

    public void save() {
        try {
            Files.createDirectories(root());
            Files.createDirectories(skinsDir());
            Files.createDirectories(attachmentsDir());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("active", active);
            data.put("characters", new ArrayList<>(characters.values()));
            try (Writer w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
                GSON.toJson(data, w);
            }
        } catch (Exception e) {
            EFMocap.LOG.warn("[efmocap] failed to save characters", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void load() {
        try {
            Files.createDirectories(skinsDir());
            Files.createDirectories(attachmentsDir());
            if (!Files.exists(file())) return;
            try (Reader r = Files.newBufferedReader(file(), StandardCharsets.UTF_8)) {
                Map<String, Object> data = GSON.fromJson(r,
                        new TypeToken<Map<String, Object>>() {}.getType());
                if (data == null) return;
                Object act = data.get("active");
                if (act instanceof String s) active = s;
                Object list = data.get("characters");
                if (list != null) {
                    List<Character> cs = GSON.fromJson(GSON.toJson(list),
                            new TypeToken<List<Character>>() {}.getType());
                    characters.clear();
                    if (cs != null) for (Character c : cs) characters.put(c.name, c);
                }
            }
        } catch (Exception e) {
            EFMocap.LOG.warn("[efmocap] failed to load characters", e);
        }
    }
}
