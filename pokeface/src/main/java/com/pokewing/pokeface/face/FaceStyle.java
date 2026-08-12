package com.pokewing.pokeface.face;

/**
 * Art style of the overlay. Each style is one row block in the expression atlas,
 * so adding a style is "draw another row" — no code change in the renderer.
 */
public enum FaceStyle {
    /** Follows the skin's own head pixels; only eyes/mouth are drawn on top. */
    DEFAULT("default", 0),
    /** Big eyes, small mouth. */
    ANIME("anime", 1),
    /** Rounded, thick outlines. */
    TOON("toon", 2),
    /** Hard angular brows, tiny pupils. */
    SHARP("sharp", 3),
    /** 1px minimal features, closest to vanilla Steve. */
    PIXEL("pixel", 4);

    private final String key;
    private final int row;

    FaceStyle(String key, int row) {
        this.key = key;
        this.row = row;
    }

    public String key() {
        return this.key;
    }

    public int row() {
        return this.row;
    }

    public String translationKey() {
        return "pokeface.style." + this.key;
    }

    public FaceStyle next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static FaceStyle byName(String name) {
        for (FaceStyle s : values()) {
            if (s.key.equalsIgnoreCase(name)) {
                return s;
            }
        }
        return DEFAULT;
    }
}
