package com.pokewing.swm.spotify;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns whatever the server owner pasted into {@code config.yml} into a
 * canonical {@code spotify:type:id} URI.
 *
 * <p>Accepted inputs:
 * <ul>
 *   <li>{@code https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abc}</li>
 *   <li>{@code https://open.spotify.com/intl-tr/track/4cOdK2wGLETKBW3PvgPWqT}</li>
 *   <li>{@code spotify:album:1DFixLWuPkv3KT3TnV35m3}</li>
 * </ul>
 */
public final class SpotifyUri {

    private static final Set<String> TYPES =
            Set.of("track", "album", "playlist", "artist", "episode", "show");

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(?:https?://)?(?:open|play)\\.spotify\\.com/(?:[a-zA-Z0-9-]+/)?"
                    + "(track|album|playlist|artist|episode|show)/([A-Za-z0-9]+)");

    private static final Pattern URI_PATTERN =
            Pattern.compile("^spotify:(track|album|playlist|artist|episode|show):([A-Za-z0-9]+)$");

    private final String type;
    private final String id;

    private SpotifyUri(String type, String id) {
        this.type = type;
        this.id = id;
    }

    /**
     * @return the parsed reference, or {@code null} when the input is not a
     *         Spotify link this plugin can play.
     */
    public static SpotifyUri parse(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        Matcher uri = URI_PATTERN.matcher(trimmed);
        if (uri.matches()) {
            return new SpotifyUri(uri.group(1).toLowerCase(Locale.ROOT), uri.group(2));
        }

        Matcher url = URL_PATTERN.matcher(trimmed);
        if (url.find()) {
            return new SpotifyUri(url.group(1).toLowerCase(Locale.ROOT), url.group(2));
        }
        return null;
    }

    public static boolean isKnownType(String type) {
        return type != null && TYPES.contains(type.toLowerCase(Locale.ROOT));
    }

    public String type() {
        return type;
    }

    public String id() {
        return id;
    }

    /** {@code spotify:playlist:37i9dQZF1DXcBWIGoYBM5M} */
    public String uri() {
        return "spotify:" + type + ":" + id;
    }

    /** Public web link, handy for chat messages. */
    public String openUrl() {
        return "https://open.spotify.com/" + type + "/" + id;
    }

    /**
     * A single track cannot be used as a Web API {@code context_uri}; it has to
     * be sent through the {@code uris} array instead.
     */
    public boolean isContext() {
        return !"track".equals(type) && !"episode".equals(type);
    }

    @Override
    public String toString() {
        return uri();
    }
}
