package com.pokewing.pokeface.face;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pokewing.pokeface.PokeFace;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Saved characters: a named {@link FaceProfile} each, stored one JSON file per
 * character under {@code config/pokeface/characters/}.
 *
 * <p>Files are keyed by a generated id rather than the name, so renaming is a
 * field edit instead of a file move and two characters may share a name. The
 * whole set is held in memory — a character is a few kilobytes, so even a very
 * long list costs little, and searching stays instant.
 */
public final class CharacterLibrary {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final class Character {
        public String id = UUID.randomUUID().toString();
        public String name = "Character";
        public FaceProfile profile = new FaceProfile();

        public Character() {
        }

        public Character(String name, FaceProfile profile) {
            this.name = name;
            this.profile = profile;
        }
    }

    private static final List<Character> CHARACTERS = new ArrayList<>();
    private static Path directory;

    private CharacterLibrary() {
    }

    public static void setDirectory(Path dir) {
        directory = dir;
    }

    public static Path directory() {
        return directory;
    }

    public static List<Character> all() {
        return CHARACTERS;
    }

    /** Case-insensitive name search; a blank query returns everything. */
    public static List<Character> search(String query) {
        if (query == null || query.isBlank()) {
            return List.copyOf(CHARACTERS);
        }
        String needle = query.toLowerCase(Locale.ROOT).trim();
        List<Character> out = new ArrayList<>();
        for (Character character : CHARACTERS) {
            if (character.name.toLowerCase(Locale.ROOT).contains(needle)) {
                out.add(character);
            }
        }
        return out;
    }

    public static void load() {
        CHARACTERS.clear();
        if (directory == null) {
            return;
        }
        try {
            Files.createDirectories(directory);
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not create {}: {}", directory, e.toString());
            return;
        }
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(p -> {
                try {
                    Character character = GSON.fromJson(Files.readString(p), Character.class);
                    if (character != null && character.profile != null) {
                        CHARACTERS.add(character);
                    }
                } catch (Exception e) {
                    PokeFace.LOGGER.warn("PokeFace: skipping character {}: {}",
                            p.getFileName(), e.toString());
                }
            });
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not list {}: {}", directory, e.toString());
        }
        CHARACTERS.sort(Comparator.comparing(c -> c.name.toLowerCase(Locale.ROOT)));
        PokeFace.LOGGER.info("PokeFace: {} character(s) loaded", CHARACTERS.size());
    }

    public static Character create(String name, FaceProfile profile) {
        Character character = new Character(name, profile.copy());
        CHARACTERS.add(character);
        save(character);
        return character;
    }

    public static void save(Character character) {
        if (directory == null) {
            return;
        }
        try {
            Files.createDirectories(directory);
            Files.writeString(directory.resolve(character.id + ".json"), GSON.toJson(character));
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not save character {}: {}", character.name, e.toString());
        }
    }

    public static void delete(Character character) {
        CHARACTERS.remove(character);
        if (directory == null) {
            return;
        }
        try {
            Files.deleteIfExists(directory.resolve(character.id + ".json"));
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not delete character {}: {}", character.name, e.toString());
        }
    }
}
