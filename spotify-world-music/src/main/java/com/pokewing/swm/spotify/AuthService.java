package com.pokewing.swm.spotify;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Owns the OAuth handshake and hands out fresh access tokens.
 *
 * <p>{@link #accessToken(UUID)} performs network I/O when the cached token has
 * expired, so it must be called from an async task.
 */
public final class AuthService {

    private static final long STATE_TTL_MS = 10 * 60 * 1000L;

    private final SpotifyClient client;
    private final LinkStore links;
    private final Logger log;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, PendingLink> pending = new ConcurrentHashMap<>();

    public AuthService(SpotifyClient client, LinkStore links, Logger log) {
        this.client = client;
        this.links = links;
        this.log = log;
    }

    /** Creates a one-shot authorize URL bound to this player. */
    public String beginLink(UUID uuid, String playerName) {
        purgeExpired();
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pending.put(state, new PendingLink(uuid, playerName, System.currentTimeMillis()));
        return client.authorizeUrl(state);
    }

    /**
     * Finishes the handshake started by {@link #beginLink}. Performs blocking
     * I/O; the web server calls it on its own thread pool.
     */
    public CallbackResult completeLink(String code, String state) {
        purgeExpired();
        if (state == null || code == null) {
            return new CallbackResult(false, null, "Eksik parametre.");
        }
        PendingLink link = pending.remove(state);
        if (link == null) {
            return new CallbackResult(false, null,
                    "Baglanti istegi bulunamadi veya suresi doldu. Oyunda /swm link komutunu tekrar calistir.");
        }
        SpotifyClient.TokenResult token = client.exchangeCode(code);
        if (!token.ok()) {
            log.warning("Spotify token exchange failed for " + link.playerName + ": " + token.error());
            return new CallbackResult(false, null, "Spotify token alinamadi: " + token.error());
        }
        if (token.refreshToken() == null) {
            return new CallbackResult(false, null,
                    "Spotify refresh token dondurmedi. Uygulama ayarlarini kontrol et.");
        }
        String spotifyName = client.currentUserName(token.accessToken());
        links.link(link.uuid, token.refreshToken(), spotifyName);
        links.putAccessToken(link.uuid, token.accessToken(), token.expiresIn());
        return new CallbackResult(true, link.uuid,
                spotifyName == null ? link.playerName : spotifyName);
    }

    /**
     * A usable access token for the player, refreshing it when needed.
     *
     * @return {@code null} when the player is not linked or the refresh failed
     */
    public String accessToken(UUID uuid) {
        String cached = links.validAccessToken(uuid);
        if (cached != null) {
            return cached;
        }
        String refreshToken = links.refreshToken(uuid);
        if (refreshToken == null) {
            return null;
        }
        SpotifyClient.TokenResult result = client.refresh(refreshToken);
        if (!result.ok()) {
            log.warning("Spotify token refresh failed for " + uuid + ": " + result.error());
            return null;
        }
        if (result.refreshToken() != null && !result.refreshToken().equals(refreshToken)) {
            links.link(uuid, result.refreshToken(), links.spotifyName(uuid));
        }
        links.putAccessToken(uuid, result.accessToken(), result.expiresIn());
        return result.accessToken();
    }

    private void purgeExpired() {
        long cutoff = System.currentTimeMillis() - STATE_TTL_MS;
        pending.entrySet().removeIf(entry -> entry.getValue().createdAt < cutoff);
    }

    /** Outcome of the {@code /callback} request. */
    public record CallbackResult(boolean ok, UUID uuid, String message) {
    }

    private record PendingLink(UUID uuid, String playerName, long createdAt) {
    }
}
