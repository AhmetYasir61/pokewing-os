package com.pokewing.swm;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Reacts to the moments a player can change zone. The repeating tracker task in
 * {@link MusicService} still catches plain walking; these events only make the
 * obvious transitions instant.
 */
public final class PlayerListener implements Listener {

    private final SpotifyWorldMusic plugin;

    public PlayerListener(SpotifyWorldMusic plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        plugin.rememberName(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        plugin.webTokens().tokenFor(event.getPlayer().getUniqueId());
        plugin.music().update(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.music().pauseApiOffline(event.getPlayer().getUniqueId());
        plugin.music().forget(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        plugin.music().update(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        // The player is only at the destination after the event resolves.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                plugin.music().update(event.getPlayer(), false);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                plugin.music().update(event.getPlayer(), false);
            }
        });
    }
}
