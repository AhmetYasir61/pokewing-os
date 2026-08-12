package com.pokewing.pokeface.market;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pokewing.pokeface.PokeFace;

import net.minecraft.client.Minecraft;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Proves who the player is to the store, without a password and without asking
 * them to sign in.
 *
 * <p>Entitlements have to outlive the config folder: a cosmetic someone paid for
 * should still be theirs after a reinstall, a wiped world, or a new PC. Tying
 * them to the account's UUID does that — names change, UUIDs do not — but the
 * mod cannot simply claim a UUID, or anyone could type someone else's and
 * collect their purchases.
 *
 * <p>So it uses the same handshake a server does: the client tells Mojang "I am
 * joining session X", the store asks Mojang "who joined session X", and Mojang
 * answers with the verified profile. The store never sees a password or a token
 * belonging to the account, and the mod never has to store one.
 */
public final class MarketAuth {

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static String sessionToken = "";

    private MarketAuth() {
    }

    /** The store session for this launch, empty until {@link #signIn} lands. */
    public static String token() {
        if (!sessionToken.isEmpty()) {
            return sessionToken;
        }
        // A remembered token saves the handshake; it is only a convenience,
        // since sign-in is silent anyway.
        return MarketConfig.get().token();
    }

    public static boolean isSignedIn() {
        return !token().isEmpty();
    }

    /**
     * Signs in silently. Safe to call whenever the market opens: it is a no-op
     * once a session exists, and a failure just means the catalog stays public
     * and downloads stay unavailable.
     */
    public static CompletableFuture<Boolean> signIn() {
        if (isSignedIn()) {
            return CompletableFuture.completedFuture(true);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getUser() == null || mc.getGameProfile() == null) {
            return CompletableFuture.completedFuture(false);
        }
        MarketConfig config = MarketConfig.get();
        String base = baseUrl(config.catalogUrl);
        if (base.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        String serverId = new BigInteger(130, new SecureRandom()).toString(16);
        try {
            // Tells Mojang this session exists. The store verifies it from its
            // side; nothing secret leaves this method.
            mc.getMinecraftSessionService().joinServer(mc.getGameProfile(),
                    mc.getUser().getAccessToken(), serverId);
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not start the store handshake ({}). "
                    + "Purchases will not be available this session.", e.toString());
            return CompletableFuture.completedFuture(false);
        }

        JsonObject body = new JsonObject();
        body.addProperty("username", mc.getGameProfile().getName());
        body.addProperty("serverId", serverId);

        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/session"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        PokeFace.LOGGER.warn("PokeFace: store refused the sign-in ({})",
                                response.statusCode());
                        return false;
                    }
                    JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
                    String token = json != null && json.has("token")
                            ? json.get("token").getAsString() : "";
                    if (token.isEmpty()) {
                        return false;
                    }
                    sessionToken = token;
                    // Only written to disk if the player asked to be remembered;
                    // either way the next launch can just sign in again.
                    MarketConfig.get().setToken(token);
                    return true;
                })
                .exceptionally(t -> {
                    PokeFace.LOGGER.warn("PokeFace: store sign-in failed: {}", t.toString());
                    return false;
                });
    }

    /** Drops the session held for this launch as well as any saved one. */
    public static void signOut() {
        sessionToken = "";
        MarketConfig.get().forgetSession();
    }

    /** Derives the store root from the catalog URL, so one setting drives both. */
    static String baseUrl(String catalogUrl) {
        try {
            URI uri = URI.create(catalogUrl);
            String port = uri.getPort() > 0 ? ":" + uri.getPort() : "";
            return uri.getScheme() + "://" + uri.getHost() + port;
        } catch (Exception e) {
            return "";
        }
    }
}
