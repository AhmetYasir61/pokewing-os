package com.pokewing.swm.config;

import org.bukkit.Location;

/**
 * A single music zone: a world (optionally narrowed down to a sphere around a
 * point, e.g. the spawn area) plus the Spotify context that should be playing
 * while a player is inside it.
 */
public final class MusicZone {

    /** Where the audio for a zone comes from. */
    public enum Kind {
        /** A Spotify link: browser embed, or the Web API for linked players. */
        SPOTIFY,
        /** A direct audio file served by this plugin or another web server. */
        AUDIO
    }

    private final Kind kind;
    private final String audioUrl;
    private final String id;
    private final String displayName;
    private final String worldName;
    private final boolean regionEnabled;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final double radius;
    private final boolean ignoreY;
    private final String rawUrl;
    private final String contextUri;
    private final String contextType;
    private final boolean loop;
    private final boolean shuffle;
    private final int volume;
    private final int priority;

    public MusicZone(Kind kind, String audioUrl, String id, String displayName, String worldName,
                     boolean regionEnabled, double centerX, double centerY, double centerZ,
                     double radius, boolean ignoreY,
                     String rawUrl, String contextUri, String contextType,
                     boolean loop, boolean shuffle, int volume, int priority) {
        this.kind = kind;
        this.audioUrl = audioUrl;
        this.id = id;
        this.displayName = displayName;
        this.worldName = worldName;
        this.regionEnabled = regionEnabled;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.ignoreY = ignoreY;
        this.rawUrl = rawUrl;
        this.contextUri = contextUri;
        this.contextType = contextType;
        this.loop = loop;
        this.shuffle = shuffle;
        this.volume = volume;
        this.priority = priority;
    }

    /** True when the given location falls inside this zone. */
    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getName().equalsIgnoreCase(worldName)) {
            return false;
        }
        if (!regionEnabled) {
            return true;
        }
        double dx = location.getX() - centerX;
        double dz = location.getZ() - centerZ;
        double distanceSq = dx * dx + dz * dz;
        if (!ignoreY) {
            double dy = location.getY() - centerY;
            distanceSq += dy * dy;
        }
        return distanceSq <= radius * radius;
    }

    public Kind kind() {
        return kind;
    }

    public boolean isAudio() {
        return kind == Kind.AUDIO;
    }

    /**
     * URL the browser player loads for an {@link Kind#AUDIO} zone: either
     * absolute, or a {@code /music/...} path served by this plugin.
     */
    public String audioUrl() {
        return audioUrl;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String worldName() {
        return worldName;
    }

    public boolean regionEnabled() {
        return regionEnabled;
    }

    public double radius() {
        return radius;
    }

    public String rawUrl() {
        return rawUrl;
    }

    /** Canonical {@code spotify:type:id} URI used by both the Web API and the embed player. */
    public String contextUri() {
        return contextUri;
    }

    /** One of {@code track}, {@code album}, {@code playlist}, {@code artist}, {@code episode}, {@code show}. */
    public String contextType() {
        return contextType;
    }

    public boolean loop() {
        return loop;
    }

    public boolean shuffle() {
        return shuffle;
    }

    public int volume() {
        return volume;
    }

    /** Higher priority wins when several zones overlap. */
    public int priority() {
        return priority;
    }
}
