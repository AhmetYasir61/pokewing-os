package com.pokewing.swm.spotify;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists the Spotify refresh token of every linked player in {@code links.yml}
 * and keeps short-lived access tokens in memory only.
 */
public final class LinkStore {

    /** Refresh a bit before the real expiry so a request never races the clock. */
    private static final long EXPIRY_MARGIN_MS = 30_000L;

    private final File file;
    private final Logger log;
    private final Map<UUID, String> refreshTokens = new ConcurrentHashMap<>();
    private final Map<UUID, String> displayNames = new ConcurrentHashMap<>();
    private final Map<UUID, AccessToken> accessTokens = new ConcurrentHashMap<>();

    public LinkStore(File file, Logger log) {
        this.file = file;
        this.log = log;
    }

    public synchronized void load() {
        refreshTokens.clear();
        displayNames.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String token = yml.getString(key + ".refresh-token", "");
                if (token != null && !token.isEmpty()) {
                    refreshTokens.put(uuid, token);
                }
                String name = yml.getString(key + ".spotify-name", "");
                if (name != null && !name.isEmpty()) {
                    displayNames.put(uuid, name);
                }
            } catch (IllegalArgumentException ignored) {
                log.warning("links.yml contains a non-UUID key: " + key);
            }
        }
    }

    public synchronized void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, String> entry : refreshTokens.entrySet()) {
            String key = entry.getKey().toString();
            yml.set(key + ".refresh-token", entry.getValue());
            String name = displayNames.get(entry.getKey());
            if (name != null) {
                yml.set(key + ".spotify-name", name);
            }
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                log.warning("Could not create the plugin data folder for links.yml");
            }
            yml.save(file);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Could not write links.yml", ex);
        }
    }

    public boolean isLinked(UUID uuid) {
        return refreshTokens.containsKey(uuid);
    }

    public String refreshToken(UUID uuid) {
        return refreshTokens.get(uuid);
    }

    public String spotifyName(UUID uuid) {
        return displayNames.get(uuid);
    }

    public void link(UUID uuid, String refreshToken, String spotifyName) {
        refreshTokens.put(uuid, refreshToken);
        if (spotifyName != null && !spotifyName.isEmpty()) {
            displayNames.put(uuid, spotifyName);
        }
        save();
    }

    public void unlink(UUID uuid) {
        refreshTokens.remove(uuid);
        displayNames.remove(uuid);
        accessTokens.remove(uuid);
        save();
    }

    /** Cached access token, or {@code null} when missing or (nearly) expired. */
    public String validAccessToken(UUID uuid) {
        AccessToken token = accessTokens.get(uuid);
        if (token == null || token.expiresAtMs - EXPIRY_MARGIN_MS <= System.currentTimeMillis()) {
            return null;
        }
        return token.value;
    }

    public void putAccessToken(UUID uuid, String value, long expiresInSeconds) {
        accessTokens.put(uuid,
                new AccessToken(value, System.currentTimeMillis() + expiresInSeconds * 1000L));
    }

    public void clearAccessToken(UUID uuid) {
        accessTokens.remove(uuid);
    }

    public int linkedCount() {
        return refreshTokens.size();
    }

    private record AccessToken(String value, long expiresAtMs) {
    }
}
