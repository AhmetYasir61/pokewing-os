package com.pokewing.swm.web;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player tokens for the browser player URL.
 *
 * <p>The token is an HMAC of the player UUID under a server-side secret, so it
 * stays the same across restarts (bookmarkable link) while remaining
 * unguessable, and it never exposes the raw UUID in the URL.
 */
public final class WebTokens {

    private final byte[] secret;
    private final Map<UUID, String> byUuid = new ConcurrentHashMap<>();
    private final Map<String, UUID> byToken = new ConcurrentHashMap<>();

    public WebTokens(String secretBase64) {
        this.secret = Base64.getDecoder().decode(secretBase64);
    }

    /** Generates a fresh secret for {@code data.yml}. */
    public static String newSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public String tokenFor(UUID uuid) {
        return byUuid.computeIfAbsent(uuid, id -> {
            String token = hmac(id.toString());
            byToken.put(token, id);
            return token;
        });
    }

    /** @return the player behind a token, or {@code null} if it was never issued */
    public UUID resolve(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return byToken.get(token);
    }

    private String hmac(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] digest = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 22);
        } catch (Exception ex) {
            throw new IllegalStateException("HmacSHA256 is unavailable", ex);
        }
    }
}
