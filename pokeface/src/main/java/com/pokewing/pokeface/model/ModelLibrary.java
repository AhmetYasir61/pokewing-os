package com.pokewing.pokeface.model;

import com.mojang.blaze3d.platform.NativeImage;
import com.pokewing.pokeface.PokeFace;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads player-supplied OBJ models and their textures from
 * {@code config/pokeface/models/}.
 *
 * <p>A model is a pair of files sharing a name: {@code ears.obj} and
 * {@code ears.png}. Drop them in, hit reload, and the model is selectable — no
 * resource pack, no restart. Both Blockbench's OBJ export and Blender's produce
 * exactly this pairing.
 *
 * <p>Models are client-side assets: another player only sees your ears if they
 * have the same files, the same way a custom resource pack works. What travels
 * over the network is the name and the placement, never the geometry.
 */
public final class ModelLibrary {

    private record Entry(ObjModel model, ResourceLocation texture) {
    }

    private static final Map<String, Entry> ENTRIES = new HashMap<>();
    private static List<String> names = List.of();

    private ModelLibrary() {
    }

    public static Path directory() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve(PokeFace.MOD_ID).resolve("models");
    }

    /** Rescans the models directory. Safe to call at any time. */
    public static void reload() {
        ENTRIES.clear();
        List<String> found = new ArrayList<>();
        Path dir = directory();
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not create {}: {}", dir, e.toString());
            names = List.of();
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".obj"))
                    .sorted()
                    .forEach(p -> {
                        String name = stripExtension(p.getFileName().toString());
                        if (load(name, p)) {
                            found.add(name);
                        }
                    });
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not list {}: {}", dir, e.toString());
        }
        names = List.copyOf(found);
        PokeFace.LOGGER.info("PokeFace: {} attachment model(s) loaded from {}", names.size(), dir);
    }

    private static boolean load(String name, Path obj) {
        ObjModel model = ObjModel.load(obj);
        if (model == null) {
            return false;
        }
        ResourceLocation texture = loadTexture(name, obj.getParent().resolve(name + ".png"));
        if (texture == null) {
            PokeFace.LOGGER.warn("PokeFace: model {} has no {}.png next to it; skipping.", name, name);
            return false;
        }
        ENTRIES.put(name, new Entry(model, texture));
        return true;
    }

    private static ResourceLocation loadTexture(String name, Path png) {
        if (!Files.exists(png)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(png)) {
            NativeImage image = NativeImage.read(in);
            ResourceLocation location = new ResourceLocation(PokeFace.MOD_ID,
                    "attachment/" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_"));
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            return location;
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not read texture {}: {}", png.getFileName(), e.toString());
            return null;
        }
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }

    public static List<String> names() {
        return names;
    }

    public static boolean has(String name) {
        return ENTRIES.containsKey(name);
    }

    public static ObjModel model(String name) {
        Entry entry = ENTRIES.get(name);
        return entry == null ? null : entry.model();
    }

    public static ResourceLocation texture(String name) {
        Entry entry = ENTRIES.get(name);
        return entry == null ? null : entry.texture();
    }
}
