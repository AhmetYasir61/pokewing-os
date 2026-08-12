package com.pokewing.pokeface.client;

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
 * Skins a character can wear, read from {@code config/pokeface/skins/}.
 *
 * <p>Drop a 64x64 PNG in, hit reload, pick it — no account change, no upload,
 * and it applies to whoever is cast as that character rather than to the
 * account, which is the point when a scene has several characters played by one
 * person.
 *
 * <p>The raw image is kept alongside the registered texture because
 * {@link SkinOverride} needs the pixels to bake the painted face into, and
 * reading them back off the GPU would be a pointless round trip when they were
 * loaded from a file two lines earlier.
 */
public final class SkinLibrary {

    private record Entry(ResourceLocation location, NativeImage image) {
    }

    private static final Map<String, Entry> ENTRIES = new HashMap<>();
    private static List<String> names = List.of();

    private SkinLibrary() {
    }

    public static Path directory() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve(PokeFace.MOD_ID).resolve("skins");
    }

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
            files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted()
                    .forEach(p -> {
                        String name = p.getFileName().toString();
                        if (load(name, p)) {
                            found.add(name);
                        }
                    });
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not list {}: {}", dir, e.toString());
        }
        names = List.copyOf(found);
        PokeFace.LOGGER.info("PokeFace: {} skin(s) available", names.size());
    }

    private static boolean load(String name, Path png) {
        try (InputStream in = Files.newInputStream(png)) {
            NativeImage image = NativeImage.read(in);
            ResourceLocation location = new ResourceLocation(PokeFace.MOD_ID,
                    "skin/" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_"));
            NativeImage forTexture = new NativeImage(image.getWidth(), image.getHeight(), false);
            forTexture.copyFrom(image);
            Minecraft.getInstance().getTextureManager()
                    .register(location, new DynamicTexture(forTexture));
            ENTRIES.put(name, new Entry(location, image));
            return true;
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not read skin {}: {}", name, e.toString());
            return false;
        }
    }

    public static List<String> names() {
        return names;
    }

    public static ResourceLocation location(String name) {
        Entry entry = ENTRIES.get(name);
        return entry == null ? null : entry.location();
    }

    /** The untouched pixels, for baking the painted face into. */
    public static NativeImage image(String name) {
        Entry entry = ENTRIES.get(name);
        return entry == null ? null : entry.image();
    }

    public static boolean has(String name) {
        return ENTRIES.containsKey(name);
    }

    /** Cycles to the next skin, wrapping through "" for the account's own. */
    public static String next(String current) {
        List<String> list = names();
        if (list.isEmpty()) {
            return "";
        }
        int index = list.indexOf(current) + 1;
        return index >= list.size() ? "" : list.get(index);
    }
}
