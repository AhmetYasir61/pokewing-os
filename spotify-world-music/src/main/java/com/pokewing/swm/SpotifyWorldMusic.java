package com.pokewing.swm;

import com.pokewing.swm.command.SwmCommand;
import com.pokewing.swm.config.AudioSource;
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
        retireStalePlayerPage();

        File musicFolder = new File(getDataFolder(), AudioSource.FOLDER);
        if (!musicFolder.isDirectory() && !musicFolder.mkdirs()) {
            getLogger().warning("Could not create the music/ folder; audio zones that reference a "
                    + "local file will not play.");
        }

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
            getLogger().info("Spotify Web API mode is ON (redirect: " + config.redirectUri() + ").");
        } else {
            this.spotify = null;
            this.auth = null;
            getLogger().info("Spotify Web API mode is OFF: " + config.apiDisabledReason());
        }
        String redirectProblem = config.redirectUriProblem();
        if (redirectProblem != null) {
            getLogger().warning(redirectProblem);
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

    /**
     * Earlier versions copied {@code web/player.html} into the data folder and
     * preferred that copy, which meant a plugin update never reached the
     * browser. Move any such copy aside so the bundled page wins again; the file
     * is kept as {@code .bak} in case it was customised.
     */
    private void retireStalePlayerPage() {
        File stale = new File(getDataFolder(), "web/player.html");
        if (!stale.isFile()) {
            return;
        }
        File backup = new File(getDataFolder(), "web/player.html.bak");
        if (backup.exists() && !backup.delete()) {
            getLogger().warning("Could not replace " + backup.getPath());
        }
        if (stale.renameTo(backup)) {
            getLogger().warning("An old copy of web/player.html was shadowing the bundled player "
                    + "page and has been moved to web/player.html.bak. The page from the jar is "
                    + "used now. To keep your own version, rename it to web/player.custom.html.");
        } else {
            getLogger().severe("web/player.html could not be moved aside - it is an outdated copy "
                    + "that will keep overriding the bundled player page. Delete it manually.");
        }
    }

    /** Plugin version shown on the player page, so a stale tab is recognisable. */
    public String versionLabel() {
        // getDescription() rather than Paper's getPluginMeta() so the jar keeps
        // working on plain Spigot.
        return getDescription().getVersion();
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

    /** False when the embedded HTTP listener could not be started. */
    public boolean webServerRunning() {
        return webServer != null;
    }

    /** True when a {@code web/player.custom.html} override is being served. */
    public boolean usingCustomPage() {
        return webServer != null && webServer.usingCustomPage();
    }
}
