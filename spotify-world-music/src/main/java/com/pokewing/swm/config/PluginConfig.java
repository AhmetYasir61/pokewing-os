package com.pokewing.swm.config;

import com.pokewing.swm.spotify.SpotifyUri;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/** Typed view over {@code config.yml}. Rebuilt from scratch on every reload. */
public final class PluginConfig {

    /** How the plugin delivers music to players. */
    public enum Mode {
        /** Browser player only (works for everyone, free accounts included). */
        WEB,
        /** Spotify Web API only (controls the player's own Spotify app, Premium). */
        API,
        /** Both at once: linked players get the Web API, everyone else the browser player. */
        BOTH
    }

    private final Mode mode;

    private final boolean webEnabled;
    private final String webBind;
    private final int webPort;
    private final String webPublicUrl;
    private final boolean webSyncPosition;
    private final int webPollSeconds;

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final boolean pauseOnLeave;
    private final boolean applyVolume;

    private final int checkIntervalTicks;
    private final boolean announceOnEnter;
    private final boolean bossBar;
    private final boolean actionBar;

    private final String messagePrefix;
    private final List<MusicZone> zones;

    private PluginConfig(Builder b) {
        this.mode = b.mode;
        this.webEnabled = b.webEnabled;
        this.webBind = b.webBind;
        this.webPort = b.webPort;
        this.webPublicUrl = b.webPublicUrl;
        this.webSyncPosition = b.webSyncPosition;
        this.webPollSeconds = b.webPollSeconds;
        this.clientId = b.clientId;
        this.clientSecret = b.clientSecret;
        this.redirectUri = b.redirectUri;
        this.pauseOnLeave = b.pauseOnLeave;
        this.applyVolume = b.applyVolume;
        this.checkIntervalTicks = b.checkIntervalTicks;
        this.announceOnEnter = b.announceOnEnter;
        this.bossBar = b.bossBar;
        this.actionBar = b.actionBar;
        this.messagePrefix = b.messagePrefix;
        this.zones = Collections.unmodifiableList(b.zones);
    }

    public static PluginConfig load(FileConfiguration cfg, Logger log) {
        Builder b = new Builder();

        String rawMode = cfg.getString("mode", "BOTH");
        try {
            b.mode = Mode.valueOf(rawMode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warning("Unknown mode '" + rawMode + "' in config.yml, falling back to BOTH.");
            b.mode = Mode.BOTH;
        }

        b.webEnabled = cfg.getBoolean("web.enabled", true);
        b.webBind = cfg.getString("web.bind", "0.0.0.0");
        b.webPort = cfg.getInt("web.port", 8787);
        b.webPublicUrl = stripTrailingSlash(cfg.getString("web.public-url", ""));
        b.webSyncPosition = cfg.getBoolean("web.sync-position", true);
        b.webPollSeconds = Math.max(1, cfg.getInt("web.poll-seconds", 3));

        b.clientId = cfg.getString("spotify.client-id", "").trim();
        b.clientSecret = cfg.getString("spotify.client-secret", "").trim();
        b.redirectUri = cfg.getString("spotify.redirect-uri", "").trim();
        b.pauseOnLeave = cfg.getBoolean("spotify.pause-on-leave", true);
        b.applyVolume = cfg.getBoolean("spotify.apply-volume", true);

        b.checkIntervalTicks = Math.max(5, cfg.getInt("tracking.check-interval-ticks", 20));
        b.announceOnEnter = cfg.getBoolean("tracking.announce-on-enter", true);
        b.bossBar = cfg.getBoolean("tracking.boss-bar", true);
        b.actionBar = cfg.getBoolean("tracking.action-bar", false);

        b.messagePrefix = cfg.getString("messages.prefix", "&8[&aMusic&8] &r");

        ConfigurationSection zonesSection = cfg.getConfigurationSection("zones");
        if (zonesSection == null) {
            log.warning("config.yml has no 'zones' section - no world will play music.");
        } else {
            for (String id : zonesSection.getKeys(false)) {
                ConfigurationSection z = zonesSection.getConfigurationSection(id);
                if (z == null) {
                    continue;
                }
                MusicZone zone = readZone(id, z, log);
                if (zone != null) {
                    b.zones.add(zone);
                }
            }
        }
        // Highest priority first, and a region zone always beats a whole-world zone
        // of equal priority so "spawn inside world" works without extra config.
        b.zones.sort((x, y) -> {
            int byPriority = Integer.compare(y.priority(), x.priority());
            if (byPriority != 0) {
                return byPriority;
            }
            return Boolean.compare(y.regionEnabled(), x.regionEnabled());
        });

        return new PluginConfig(b);
    }

    private static MusicZone readZone(String id, ConfigurationSection z, Logger log) {
        if (!z.getBoolean("enabled", true)) {
            return null;
        }
        String world = z.getString("world", "");
        if (world == null || world.isBlank()) {
            log.warning("Zone '" + id + "' has no 'world' set - skipped.");
            return null;
        }
        String url = z.getString("url", "");
        SpotifyUri parsed = SpotifyUri.parse(url);
        String audioUrl = parsed == null ? AudioSource.resolve(url) : null;
        if (parsed == null && audioUrl == null) {
            log.warning("Zone '" + id + "' has an unusable url ('" + url + "') - skipped. "
                    + "Expected a Spotify link (https://open.spotify.com/playlist/<id>), an audio "
                    + "file placed in the plugin's music/ folder (muzik.mp3), or a direct "
                    + "http(s) link to an audio file.");
            return null;
        }

        boolean regionEnabled = z.getBoolean("region.enabled", false);
        double cx = z.getDouble("region.center.x", 0.0);
        double cy = z.getDouble("region.center.y", 64.0);
        double cz = z.getDouble("region.center.z", 0.0);
        double radius = z.getDouble("region.radius", 100.0);
        boolean ignoreY = z.getBoolean("region.ignore-y", true);
        if (regionEnabled && radius <= 0) {
            log.warning("Zone '" + id + "' has region.radius <= 0 - region check disabled.");
            regionEnabled = false;
        }

        return new MusicZone(
                parsed != null ? MusicZone.Kind.SPOTIFY : MusicZone.Kind.AUDIO,
                audioUrl,
                id,
                z.getString("display-name", id),
                world,
                regionEnabled, cx, cy, cz, radius, ignoreY,
                parsed != null ? parsed.openUrl() : url.trim(),
                parsed != null ? parsed.uri() : null,
                parsed != null ? parsed.type() : "audio",
                z.getBoolean("loop", true),
                z.getBoolean("shuffle", false),
                clamp(z.getInt("volume", 60), 0, 100),
                z.getInt("priority", 0));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String stripTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    /** First zone (highest priority) that contains the location, or {@code null}. */
    public MusicZone zoneAt(Location location) {
        for (MusicZone zone : zones) {
            if (zone.contains(location)) {
                return zone;
            }
        }
        return null;
    }

    public MusicZone zoneById(String id) {
        for (MusicZone zone : zones) {
            if (zone.id().equalsIgnoreCase(id)) {
                return zone;
            }
        }
        return null;
    }

    public boolean apiUsable() {
        return (mode == Mode.API || mode == Mode.BOTH)
                && !clientId.isEmpty() && !clientSecret.isEmpty() && !redirectUri.isEmpty();
    }

    /**
     * Why the Spotify Web API mode is off, in words a server owner can act on.
     *
     * @return {@code null} when the API mode is configured
     */
    public String apiDisabledReason() {
        if (mode == Mode.WEB) {
            return "config.yml icinde 'mode: WEB' ayarli. API icin 'API' veya 'BOTH' yaz.";
        }
        List<String> missing = new ArrayList<>();
        if (clientId.isEmpty()) {
            missing.add("spotify.client-id");
        }
        if (clientSecret.isEmpty()) {
            missing.add("spotify.client-secret");
        }
        if (redirectUri.isEmpty()) {
            missing.add("spotify.redirect-uri");
        }
        if (!missing.isEmpty()) {
            return "config.yml icinde su alan(lar) bos: " + String.join(", ", missing)
                    + ". Dosyayi kaydettikten sonra /swm reload calistir.";
        }
        return null;
    }

    /**
     * Spotify only accepts HTTPS redirect URIs, or an explicit loopback literal
     * ({@code http://127.0.0.1:PORT} / {@code http://[::1]:PORT}); {@code localhost}
     * and plain HTTP public addresses are rejected when the app is saved.
     */
    public static boolean isAcceptedRedirect(String uri) {
        if (uri == null) {
            return false;
        }
        String value = uri.trim().toLowerCase(Locale.ROOT);
        return value.startsWith("https://")
                || value.startsWith("http://127.0.0.1")
                || value.startsWith("http://[::1]");
    }

    /**
     * Problem with {@code spotify.redirect-uri} that will make Spotify refuse the
     * login, or {@code null} when it looks usable.
     */
    public String redirectUriProblem() {
        if (redirectUri.isEmpty()) {
            return null;
        }
        if (!isAcceptedRedirect(redirectUri)) {
            return "spotify.redirect-uri '" + redirectUri + "' Spotify tarafindan kabul edilmez. "
                    + "Spotify yalnizca https:// adresleri veya acik loopback adresi "
                    + "(http://127.0.0.1:PORT) kabul ediyor; 'localhost' ve duz http:// "
                    + "genel adresler reddediliyor. API modu icin alan adi + HTTPS gerekiyor.";
        }
        if (!redirectUri.endsWith("/callback")) {
            return "spotify.redirect-uri '" + redirectUri + "' /callback ile bitmeli, "
                    + "cunku eklenti yalnizca o yolu dinliyor.";
        }
        return null;
    }

    public boolean webUsable() {
        return webEnabled && (mode == Mode.WEB || mode == Mode.BOTH);
    }

    /** Base URL players open in a browser; falls back to the bind address. */
    public String publicUrl() {
        if (!webPublicUrl.isEmpty()) {
            return webPublicUrl;
        }
        String host = "0.0.0.0".equals(webBind) ? "localhost" : webBind;
        return "http://" + host + ":" + webPort;
    }

    public Mode mode() {
        return mode;
    }

    public boolean webEnabled() {
        return webEnabled;
    }

    public String webBind() {
        return webBind;
    }

    public int webPort() {
        return webPort;
    }

    public boolean webSyncPosition() {
        return webSyncPosition;
    }

    public int webPollSeconds() {
        return webPollSeconds;
    }

    public String clientId() {
        return clientId;
    }

    public String clientSecret() {
        return clientSecret;
    }

    public String redirectUri() {
        return redirectUri;
    }

    public boolean pauseOnLeave() {
        return pauseOnLeave;
    }

    public boolean applyVolume() {
        return applyVolume;
    }

    public int checkIntervalTicks() {
        return checkIntervalTicks;
    }

    public boolean announceOnEnter() {
        return announceOnEnter;
    }

    public boolean bossBar() {
        return bossBar;
    }

    public boolean actionBar() {
        return actionBar;
    }

    public String messagePrefix() {
        return messagePrefix;
    }

    public List<MusicZone> zones() {
        return zones;
    }

    private static final class Builder {
        Mode mode = Mode.BOTH;
        boolean webEnabled = true;
        String webBind = "0.0.0.0";
        int webPort = 8787;
        String webPublicUrl = "";
        boolean webSyncPosition = true;
        int webPollSeconds = 3;
        String clientId = "";
        String clientSecret = "";
        String redirectUri = "";
        boolean pauseOnLeave = true;
        boolean applyVolume = true;
        int checkIntervalTicks = 20;
        boolean announceOnEnter = true;
        boolean bossBar = true;
        boolean actionBar = false;
        String messagePrefix = "&8[&aMusic&8] &r";
        final List<MusicZone> zones = new ArrayList<>();
    }
}
