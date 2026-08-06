package com.pokewing.swm;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Chat helper. Uses the BungeeCord chat components that both Spigot and Paper
 * expose, so the jar stays usable on either server.
 */
public final class Messages {

    private final SpotifyWorldMusic plugin;

    public Messages(SpotifyWorldMusic plugin) {
        this.plugin = plugin;
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private String prefix() {
        return color(plugin.config().messagePrefix());
    }

    public void send(CommandSender target, String message) {
        target.sendMessage(prefix() + color(message));
    }

    public void raw(CommandSender target, String message) {
        target.sendMessage(color(message));
    }

    /** Sends a clickable message that opens {@code url} in the player's browser. */
    public void sendLink(CommandSender target, String message, String url) {
        if (url == null || url.isEmpty()) {
            send(target, message);
            return;
        }
        if (!(target instanceof Player player)) {
            target.sendMessage(prefix() + color(message) + " " + ChatColor.GRAY + url);
            return;
        }
        TextComponent component = new TextComponent(prefix() + color(message));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(color("&7" + url))));
        player.spigot().sendMessage(component);
    }

    public void actionBar(Player player, String message) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new TextComponent(color(message)));
    }
}
