package com.pokewing.pokeface.market;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pokewing.pokeface.PokeFace;

import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Where the market points and how the player wants to check out.
 *
 * <p>The store is a URL rather than something baked in, so a creator can point
 * the mod at their own Tebex store (or any endpoint that serves the same JSON)
 * without a new build, and a server can ship a config that points at theirs.
 */
public final class MarketConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * The official store, baked in so nobody has to type it. It stays editable
     * because a server or a creator may run their own listing service, but the
     * default has to work out of the box.
     */
    public static final String DEFAULT_CATALOG = "https://pokewing.com/api/cosmetic.json";
    public static final String DEFAULT_PUBLISH = "https://pokewing.com/api/publish";

    /** Catalog endpoint: returns a JSON array of {@link MarketItem}. */
    public String catalogUrl = DEFAULT_CATALOG;
    /** Where "publish" uploads a packaged cosmetic. */
    public String publishUrl = DEFAULT_PUBLISH;
    /** Open checkout in the system browser (safest) rather than in-game. */
    public boolean useSystemBrowser = true;
    /**
     * Remember the store session between launches. Off by default: the less that
     * is kept on disk, the less there is to lose.
     */
    public boolean rememberSession = false;
    /** The store's session token, encrypted at rest. Never a payment detail. */
    public String encryptedToken = "";

    private static MarketConfig instance;

    public static Path directory() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve(PokeFace.MOD_ID);
    }

    private static Path file() {
        return directory().resolve("market.json");
    }

    public static MarketConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static MarketConfig load() {
        try {
            if (Files.exists(file())) {
                MarketConfig config = GSON.fromJson(Files.readString(file()), MarketConfig.class);
                if (config != null) {
                    // An older config, or one a user blanked, still lands on the
                    // official store rather than on an empty market.
                    if (config.catalogUrl == null || config.catalogUrl.isBlank()) {
                        config.catalogUrl = DEFAULT_CATALOG;
                    }
                    if (config.publishUrl == null || config.publishUrl.isBlank()) {
                        config.publishUrl = DEFAULT_PUBLISH;
                    }
                    return config;
                }
            }
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not read market config ({}), using defaults.",
                    e.toString());
        }
        return new MarketConfig();
    }

    public void save() {
        try {
            Files.createDirectories(directory());
            Files.writeString(file(), GSON.toJson(this));
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not save market config: {}", e.toString());
        }
    }

    /** @return the store session token in plain text, or "" when not kept */
    public String token() {
        return this.rememberSession ? SecureStore.decrypt(directory(), this.encryptedToken) : "";
    }

    public void setToken(String token) {
        if (!this.rememberSession || token == null || token.isEmpty()) {
            this.encryptedToken = "";
        } else {
            this.encryptedToken = SecureStore.encrypt(directory(), token);
        }
        save();
    }

    /** Forgets the session and destroys the key it was encrypted with. */
    public void forgetSession() {
        this.encryptedToken = "";
        SecureStore.wipe(directory());
        save();
    }
}
