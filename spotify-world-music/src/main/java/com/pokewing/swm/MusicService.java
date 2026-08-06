package com.pokewing.swm;

import com.pokewing.swm.config.MusicZone;
import com.pokewing.swm.config.PluginConfig;
import com.pokewing.swm.spotify.LinkStore;
import com.pokewing.swm.spotify.SpotifyClient;
import com.pokewing.swm.spotify.SpotifyUri;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of which zone every player is standing in and reacts to changes:
 * updates the boss bar, pushes playback to linked Spotify accounts and keeps a
 * per-zone clock so browser listeners in the same zone stay roughly in sync.
 */
public final class MusicService {

    private final SpotifyWorldMusic plugin;

    /** Zone id the player is currently being served, {@code ""} when none. */
    private final Map<UUID, String> currentZone = new ConcurrentHashMap<>();
    /** Bumped on every zone change so the browser player notices instantly. */
    private final Map<UUID, Long> stateVersion = new ConcurrentHashMap<>();
    /** Zone id -> wall clock time the zone's music session started. */
    private final Map<String, Long> zoneStartedAt = new ConcurrentHashMap<>();
    /** Zone id -> duration of the playing item in ms, reported by browser players. */
    private final Map<String, Long> zoneDuration = new ConcurrentHashMap<>();
    /** Players already told that their Spotify app has no active device. */
    private final Map<UUID, Long> deviceWarnedAt = new ConcurrentHashMap<>();
    private final Map<String, BossBar> bossBars = new HashMap<>();

    private BukkitTask trackerTask;

    public MusicService(SpotifyWorldMusic plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        int interval = plugin.config().checkIntervalTicks();
        trackerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    public void stop() {
        if (trackerTask != null) {
            trackerTask.cancel();
            trackerTask = null;
        }
        for (BossBar bar : bossBars.values()) {
            bar.removeAll();
        }
        bossBars.clear();
    }

    /** Drops all cached state; used by {@code /swm reload}. */
    public void reset() {
        stop();
        currentZone.clear();
        zoneStartedAt.clear();
        zoneDuration.clear();
        deviceWarnedAt.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            bump(player.getUniqueId());
        }
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player, false);
        }
    }

    /**
     * Re-evaluates the zone of a single player.
     *
     * @param force re-applies playback even when the zone did not change
     */
    public void update(Player player, boolean force) {
        PluginConfig config = plugin.config();
        MusicZone zone = config.zoneAt(player.getLocation());
        String newId = zone == null ? "" : zone.id();
        String oldId = currentZone.getOrDefault(player.getUniqueId(), "");

        if (newId.equals(oldId) && !force) {
            return;
        }
        currentZone.put(player.getUniqueId(), newId);
        bump(player.getUniqueId());

        if (zone == null) {
            clearBossBar(player);
            if (config.pauseOnLeave()) {
                pauseApi(player);
            }
            return;
        }

        zoneStartedAt.putIfAbsent(zone.id(), System.currentTimeMillis());
        showBossBar(player, zone);
        announce(player, zone);
        playApi(player, zone);
    }

    /** Called when a player leaves the server. */
    public void forget(Player player) {
        UUID uuid = player.getUniqueId();
        currentZone.remove(uuid);
        stateVersion.remove(uuid);
        deviceWarnedAt.remove(uuid);
        clearBossBar(player);
    }

    /** True while the player is online and being tracked. Safe off the main thread. */
    public boolean isTracked(UUID uuid) {
        return currentZone.containsKey(uuid);
    }

    public MusicZone zoneOf(UUID uuid) {
        String id = currentZone.get(uuid);
        if (id == null || id.isEmpty()) {
            return null;
        }
        return plugin.config().zoneById(id);
    }

    public long version(UUID uuid) {
        return stateVersion.getOrDefault(uuid, 0L);
    }

    private void bump(UUID uuid) {
        stateVersion.merge(uuid, 1L, Long::sum);
    }

    // ---------------------------------------------------------------- zone clock

    /** Browser players report how long the item they loaded is, for zone sync. */
    public void reportDuration(String zoneId, long durationMs) {
        if (zoneId == null || zoneId.isEmpty() || durationMs <= 0) {
            return;
        }
        zoneDuration.put(zoneId, durationMs);
        zoneStartedAt.putIfAbsent(zoneId, System.currentTimeMillis());
    }

    /**
     * Where a listener joining right now should start, so everybody in the zone
     * hears the same thing. Only meaningful for single tracks: for playlists and
     * albums the item boundaries are unknown to the server, so playback starts
     * from the top instead of guessing.
     */
    public long syncPositionMs(MusicZone zone) {
        if (zone == null || !plugin.config().webSyncPosition()) {
            return 0L;
        }
        if (!"track".equals(zone.contextType()) && !"episode".equals(zone.contextType())
                && !zone.isAudio()) {
            return 0L;
        }
        Long duration = zoneDuration.get(zone.id());
        Long startedAt = zoneStartedAt.get(zone.id());
        if (duration == null || startedAt == null || duration <= 0) {
            return 0L;
        }
        long elapsed = System.currentTimeMillis() - startedAt;
        if (elapsed < 0) {
            return 0L;
        }
        if (!zone.loop()) {
            return Math.min(elapsed, duration);
        }
        return elapsed % duration;
    }

    // ---------------------------------------------------------------- presentation

    private void announce(Player player, MusicZone zone) {
        PluginConfig config = plugin.config();
        if (config.actionBar()) {
            plugin.messages().actionBar(player, "&a♪ &f" + zone.displayName());
        }
        if (!config.announceOnEnter()) {
            return;
        }
        plugin.messages().send(player, "&7Su an caliyor: &a" + zone.displayName());
        if (config.webUsable()) {
            plugin.messages().sendLink(player,
                    "&7Muzigi duymak icin tarayici oynaticiyi ac: &b[TIKLA]",
                    plugin.webPlayerUrl(player));
        }
        if (config.apiUsable() && !plugin.links().isLinked(player.getUniqueId())) {
            plugin.messages().send(player,
                    "&7Spotify hesabini baglamak icin &e/swm link &7yazabilirsin.");
        }
    }

    private void showBossBar(Player player, MusicZone zone) {
        clearBossBar(player);
        if (!plugin.config().bossBar()) {
            return;
        }
        BossBar bar = bossBars.computeIfAbsent(zone.id(), id -> {
            BossBar created = Bukkit.createBossBar(
                    ChatColor.translateAlternateColorCodes('&', "&a♪ &f" + zone.displayName()),
                    BarColor.GREEN, BarStyle.SOLID);
            created.setProgress(1.0D);
            return created;
        });
        bar.setTitle(ChatColor.translateAlternateColorCodes('&', "&a♪ &f" + zone.displayName()));
        bar.addPlayer(player);
    }

    private void clearBossBar(Player player) {
        for (BossBar bar : bossBars.values()) {
            bar.removePlayer(player);
        }
    }

    // ---------------------------------------------------------------- Spotify Web API

    private void playApi(Player player, MusicZone zone) {
        SpotifyClient client = plugin.spotify();
        if (client == null || !plugin.links().isLinked(player.getUniqueId())) {
            return;
        }
        if (zone.isAudio()) {
            // A plain audio file cannot be pushed to Spotify; the browser player
            // handles this zone, so stop whatever Spotify was playing before.
            pauseApi(player);
            return;
        }
        UUID uuid = player.getUniqueId();
        SpotifyUri uri = SpotifyUri.parse(zone.contextUri());
        if (uri == null) {
            return;
        }
        long position = syncPositionMs(zone);

        runAsync(() -> {
            String token = plugin.auth().accessToken(uuid);
            if (token == null) {
                notifyPlayer(uuid, "&cSpotify baglantin yenilenemedi, &e/swm link &cile tekrar dene.");
                return;
            }
            SpotifyClient.ApiResult result = client.play(token, uri, position);
            if (result.unauthorized()) {
                plugin.links().clearAccessToken(uuid);
                token = plugin.auth().accessToken(uuid);
                if (token == null) {
                    notifyPlayer(uuid, "&cSpotify oturumun dustu, &e/swm link &cile tekrar bagla.");
                    return;
                }
                result = client.play(token, uri, position);
            }
            if (!result.ok()) {
                if (result.noActiveDevice()) {
                    warnNoDevice(uuid);
                } else {
                    notifyPlayer(uuid, "&c" + result.error());
                }
                return;
            }

            String repeat = zone.loop() ? (uri.isContext() ? "context" : "track") : "off";
            client.setRepeat(token, repeat);
            if (uri.isContext()) {
                client.setShuffle(token, zone.shuffle());
            }
            if (plugin.config().applyVolume()) {
                client.setVolume(token, zone.volume());
            }
        });
    }

    private void pauseApi(Player player) {
        SpotifyClient client = plugin.spotify();
        if (client == null || !plugin.links().isLinked(player.getUniqueId())) {
            return;
        }
        UUID uuid = player.getUniqueId();
        runAsync(() -> {
            String token = plugin.auth().accessToken(uuid);
            if (token != null) {
                client.pause(token);
            }
        });
    }

    /** Pauses playback for a player that is going offline (called on quit). */
    public void pauseApiOffline(UUID uuid) {
        SpotifyClient client = plugin.spotify();
        if (client == null || !plugin.config().pauseOnLeave() || !plugin.links().isLinked(uuid)) {
            return;
        }
        runAsync(() -> {
            String token = plugin.auth().accessToken(uuid);
            if (token != null) {
                client.pause(token);
            }
        });
    }

    private void warnNoDevice(UUID uuid) {
        long now = System.currentTimeMillis();
        Long last = deviceWarnedAt.get(uuid);
        if (last != null && now - last < 60_000L) {
            return;
        }
        deviceWarnedAt.put(uuid, now);
        notifyPlayer(uuid, "&eSpotify uygulaman kapali gorunuyor. Ac, herhangi bir sarkiyi "
                + "baslat ve tekrar bu bolgeye gir.");
    }

    private void notifyPlayer(UUID uuid, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.messages().send(player, message);
            }
        });
    }

    private void runAsync(Runnable task) {
        if (!plugin.isEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }
}
