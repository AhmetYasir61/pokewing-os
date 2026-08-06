package com.pokewing.swm;

import com.pokewing.swm.command.SwmCommand;
import com.pokewing.swm.config.PluginConfig;
import com.pokewing.swm.spotify.AuthService;
import com.pokewing.swm.spotify.LinkStore;
import com.pokewing.swm.spotify.SpotifyClient;
import com.pokewing.swm.web.WebServer;
import com.pokewing.swm.web.WebTokens;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Plays a Spotify link in configured worlds/regions (spawn, hub, nether, ...)
 * and keeps it looping, either through a browser player page every player can
 * open or through the Spotify Web API for players who linked their account.
 */
public final class SpotifyWorldMusic extends JavaPlugin {

    private PluginConfig config;
    private Messages messages;
    private LinkStore links;
    private WebTokens webTokens;
    private MusicService music;

    private SpotifyClient spotify;
    private AuthService auth;
    private WebServer webServer;

    private final Map<UUID, String> knownNames = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("web/player.html", false);

        this.messages = new Messages(this);
        this.links = new LinkStore(new File(getDataFolder(), "links.yml"), getLogger());
        this.links.load();
        this.webTokens = new WebTokens(loadOrCreateSecret());
        this.music = new MusicService(this);

        reloadEverything(true);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        PluginCommand command = getCommand("swm");
        if (command != null) {
            SwmCommand executor = new SwmCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().severe("Command 'swm' is missing from plugin.yml - commands disabled.");
        }

        getLogger().info("Enabled with " + config.zones().size() + " music zone(s), mode "
                + config.mode() + ".");
    }

    @Override
    public void onDisable() {
        if (music != null) {
            music.stop();
        }
        if (webServer != null) {
            webServer.stop();
            webServer = null;
        }
        if (links != null) {
            links.save();
        }
    }

    /**
     * (Re)reads config.yml and restarts the pieces that depend on it.
     *
     * @param initial true on startup, where there is nothing to tear down yet
     */
    public void reloadEverything(boolean initial) {
        if (!initial) {
            reloadConfig();
        }
        this.config = PluginConfig.load(getConfig(), getLogger());

        if (config.apiUsable()) {
            this.spotify = new SpotifyClient(config.clientId(), config.clientSecret(),
                    config.redirectUri());
            this.auth = new AuthService(spotify, links, getLogger());
        } else {
            this.spotify = null;
            this.auth = null;
            if (config.mode() != PluginConfig.Mode.WEB) {
                getLogger().info("Spotify API credentials are incomplete - running with the "
                        + "browser player only. Fill in spotify.client-id / client-secret / "
                        + "redirect-uri in config.yml to enable /swm link.");
            }
        }

        if (webServer != null) {
            webServer.stop();
            webServer = null;
        }
        if (config.webUsable() || config.apiUsable()) {
            // The OAuth callback needs the same listener, so the server also comes
            // up when only the API mode is configured.
            try {
                webServer = new WebServer(this);
                webServer.start();
            } catch (IOException ex) {
                webServer = null;
                getLogger().log(Level.SEVERE, "Could not bind the web player to "
                        + config.webBind() + ":" + config.webPort()
                        + " - browser player and /swm link are unavailable.", ex);
            }
        }

        music.reset();
        music.start();
        for (Player player : Bukkit.getOnlinePlayers()) {
            knownNames.put(player.getUniqueId(), player.getName());
            webTokens.tokenFor(player.getUniqueId());
            music.update(player, true);
        }
    }

    private String loadOrCreateSecret() {
        File file = new File(getDataFolder(), "data.yml");
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        String secret = data.getString("web-secret", "");
        if (secret == null || secret.isEmpty()) {
            secret = WebTokens.newSecret();
            data.set("web-secret", secret);
            try {
                if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
                    getLogger().warning("Could not create the plugin data folder.");
                }
                data.save(file);
            } catch (IOException ex) {
                getLogger().log(Level.WARNING,
                        "Could not persist data.yml - browser player links will change on restart.",
                        ex);
            }
        }
        return secret;
    }

    /** Personal browser player URL for a player. */
    public String webPlayerUrl(Player player) {
        String token = webTokens.tokenFor(player.getUniqueId());
        return config.publicUrl() + "/?t=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    /** Called by the web server once an OAuth callback succeeded. */
    public void onPlayerLinked(UUID uuid, String spotifyName) {
        Bukkit.getScheduler().runTask(this, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                messages.send(player, "&aSpotify hesabin baglandi&7: &f" + spotifyName);
                music.update(player, true);
            }
        });
    }

    public void rememberName(UUID uuid, String name) {
        knownNames.put(uuid, name);
    }

    /** Last known player name; safe to call off the main thread. */
    public String knownName(UUID uuid) {
        return knownNames.getOrDefault(uuid, "Oyuncu");
    }

    public String serverDisplayName() {
        String motd = Bukkit.getMotd();
        if (motd == null || motd.isBlank()) {
            return "Minecraft sunucusu";
        }
        return Messages.color(motd).replaceAll("§[0-9a-fk-orA-FK-OR]", "").split("\n")[0].trim();
    }

    public PluginConfig config() {
        return config;
    }

    public Messages messages() {
        return messages;
    }

    public LinkStore links() {
        return links;
    }

    public WebTokens webTokens() {
        return webTokens;
    }

    public MusicService music() {
        return music;
    }

    /** {@code null} when the Spotify Web API is not configured. */
    public SpotifyClient spotify() {
        return spotify;
    }

    /** {@code null} when the Spotify Web API is not configured. */
    public AuthService auth() {
        return auth;
    }
}
