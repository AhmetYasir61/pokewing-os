package com.pokewing.swm.spotify;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Thin wrapper over the parts of the Spotify Web API this plugin needs.
 *
 * <p>Every method here performs blocking network I/O and must therefore be
 * called from an async task, never from the server main thread.
 */
public final class SpotifyClient {

    private static final String ACCOUNTS = "https://accounts.spotify.com";
    private static final String API = "https://api.spotify.com/v1";
    private static final String SCOPES =
            "user-read-playback-state user-modify-playback-state user-read-private";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public SpotifyClient(String clientId, String clientSecret, String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    /** URL the player opens to grant the server control over their Spotify app. */
    public String authorizeUrl(String state) {
        return ACCOUNTS + "/authorize"
                + "?client_id=" + enc(clientId)
                + "&response_type=code"
                + "&redirect_uri=" + enc(redirectUri)
                + "&scope=" + enc(SCOPES)
                + "&state=" + enc(state)
                + "&show_dialog=false";
    }

    public TokenResult exchangeCode(String code) {
        String body = "grant_type=authorization_code"
                + "&code=" + enc(code)
                + "&redirect_uri=" + enc(redirectUri);
        return requestToken(body);
    }

    public TokenResult refresh(String refreshToken) {
        String body = "grant_type=refresh_token&refresh_token=" + enc(refreshToken);
        return requestToken(body);
    }

    private TokenResult requestToken(String form) {
        String basic = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create(ACCOUNTS + "/api/token"))
                .header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return TokenResult.failure(describe(response.statusCode(), response.body()));
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String accessToken = json.has("access_token") ? json.get("access_token").getAsString() : null;
            if (accessToken == null) {
                return TokenResult.failure("Spotify returned no access_token");
            }
            String newRefresh = json.has("refresh_token") ? json.get("refresh_token").getAsString() : null;
            long expiresIn = json.has("expires_in") ? json.get("expires_in").getAsLong() : 3600L;
            return TokenResult.success(accessToken, newRefresh, expiresIn);
        } catch (Exception ex) {
            return TokenResult.failure(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    /** Display name of the authenticated Spotify account, or {@code null}. */
    public String currentUserName(String accessToken) {
        ApiResult result = call("GET", API + "/me", accessToken, null);
        if (!result.ok() || result.body() == null || result.body().isEmpty()) {
            return null;
        }
        try {
            JsonObject json = JsonParser.parseString(result.body()).getAsJsonObject();
            if (json.has("display_name") && !json.get("display_name").isJsonNull()) {
                return json.get("display_name").getAsString();
            }
            return json.has("id") ? json.get("id").getAsString() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Starts (or restarts) playback of the given Spotify URI on the player's
     * active device.
     *
     * @param positionMs where to start inside the first item, for zone-wide sync
     */
    public ApiResult play(String accessToken, SpotifyUri uri, long positionMs) {
        JsonObject body = new JsonObject();
        if (uri.isContext()) {
            body.addProperty("context_uri", uri.uri());
        } else {
            var uris = new com.google.gson.JsonArray();
            uris.add(uri.uri());
            body.add("uris", uris);
        }
        if (positionMs > 0) {
            body.addProperty("position_ms", positionMs);
        }
        return call("PUT", API + "/me/player/play", accessToken, body.toString());
    }

    public ApiResult pause(String accessToken) {
        return call("PUT", API + "/me/player/pause", accessToken, "");
    }

    /** @param state one of {@code track}, {@code context}, {@code off} */
    public ApiResult setRepeat(String accessToken, String state) {
        return call("PUT", API + "/me/player/repeat?state=" + enc(state), accessToken, "");
    }

    public ApiResult setShuffle(String accessToken, boolean shuffle) {
        return call("PUT", API + "/me/player/shuffle?state=" + shuffle, accessToken, "");
    }

    public ApiResult setVolume(String accessToken, int volumePercent) {
        int clamped = Math.max(0, Math.min(100, volumePercent));
        return call("PUT", API + "/me/player/volume?volume_percent=" + clamped, accessToken, "");
    }

    /** True when the account currently has a device Spotify can push audio to. */
    public boolean hasActiveDevice(String accessToken) {
        ApiResult result = call("GET", API + "/me/player/devices", accessToken, null);
        if (!result.ok() || result.body() == null || result.body().isEmpty()) {
            return false;
        }
        try {
            JsonObject json = JsonParser.parseString(result.body()).getAsJsonObject();
            if (!json.has("devices")) {
                return false;
            }
            return json.getAsJsonArray("devices").size() > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private ApiResult call(String method, String url, String accessToken, String body) {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .timeout(Duration.ofSeconds(15))
                .method(method, publisher);
        if (body != null && !body.isEmpty()) {
            builder.header("Content-Type", "application/json");
        }
        try {
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status / 100 == 2) {
                return new ApiResult(true, status, response.body(), null);
            }
            return new ApiResult(false, status, response.body(), describe(status, response.body()));
        } catch (Exception ex) {
            return new ApiResult(false, -1, null, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private static String describe(int status, String body) {
        String detail = extractMessage(body);
        return switch (status) {
            case 401 -> "Spotify token gecersiz veya suresi dolmus (401)";
            case 403 -> "Spotify bu islemi reddetti - hesap Premium olmayabilir (403)"
                    + (detail == null ? "" : ": " + detail);
            case 404 -> "Aktif Spotify cihazi bulunamadi (404) - Spotify uygulamasini acip bir sey calin";
            case 429 -> "Spotify rate limit (429)";
            default -> "Spotify API hatasi (" + status + ")" + (detail == null ? "" : ": " + detail);
        };
    }

    private static String extractMessage(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("error")) {
                var error = json.get("error");
                if (error.isJsonObject() && error.getAsJsonObject().has("message")) {
                    return error.getAsJsonObject().get("message").getAsString();
                }
                if (error.isJsonPrimitive()) {
                    return error.getAsString();
                }
            }
            if (json.has("error_description")) {
                return json.get("error_description").getAsString();
            }
        } catch (Exception ignored) {
            // Not JSON - fall through and report only the status code.
        }
        return null;
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Result of a token endpoint call. */
    public record TokenResult(boolean ok, String accessToken, String refreshToken,
                              long expiresIn, String error) {
        static TokenResult success(String accessToken, String refreshToken, long expiresIn) {
            return new TokenResult(true, accessToken, refreshToken, expiresIn, null);
        }

        static TokenResult failure(String error) {
            return new TokenResult(false, null, null, 0L, error);
        }
    }

    /** Result of a generic Web API call. */
    public record ApiResult(boolean ok, int status, String body, String error) {
        public boolean unauthorized() {
            return status == 401;
        }

        public boolean noActiveDevice() {
            return status == 404;
        }
    }
}
