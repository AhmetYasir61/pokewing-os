package com.pokewing.swm.config;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Direct audio files (mp3, ogg, ...) as an alternative to a Spotify link.
 *
 * <p>A zone may point either at a file the plugin serves itself - dropped into
 * {@code plugins/SpotifyWorldMusic/music/} and written as {@code muzik.mp3} in
 * the config - or at an absolute {@code http(s)} URL that already serves the raw
 * audio bytes.
 */
public final class AudioSource {

    /** Web path prefix the embedded server exposes the music folder under. */
    public static final String WEB_PREFIX = "/music/";
    /** Folder inside the plugin data directory the files are read from. */
    public static final String FOLDER = "music";

    private static final Set<String> EXTENSIONS =
            Set.of("mp3", "ogg", "oga", "wav", "m4a", "aac", "flac", "opus", "webm");

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "mp3", "audio/mpeg",
            "ogg", "audio/ogg",
            "oga", "audio/ogg",
            "opus", "audio/ogg",
            "wav", "audio/wav",
            "m4a", "audio/mp4",
            "aac", "audio/aac",
            "flac", "audio/flac",
            "webm", "audio/webm");

    private AudioSource() {
    }

    /**
     * Turns the {@code url} field of a zone into something the browser player can
     * load.
     *
     * @return an absolute URL, or a {@code /music/...} path served by this plugin;
     *         {@code null} when the value is not a usable audio reference
     */
    public static String resolve(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }

        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            // Only accept links that actually point at an audio file: anything
            // else is a web page and would play nothing.
            return hasAudioExtension(stripQuery(value)) ? value : null;
        }

        String relative = sanitizeRelative(value);
        if (relative == null || !hasAudioExtension(relative)) {
            return null;
        }
        return WEB_PREFIX + encodePath(relative);
    }

    /**
     * Validates a path taken from a request or config value: no absolute paths,
     * no traversal, no backslashes.
     *
     * @return the cleaned relative path, or {@code null} when it is not safe
     */
    public static String sanitizeRelative(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replace('\\', '/');
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.isEmpty() || value.contains("..") || value.contains(":")) {
            return null;
        }
        for (String segment : value.split("/")) {
            if (segment.isEmpty() || ".".equals(segment)) {
                return null;
            }
        }
        return value;
    }

    public static boolean hasAudioExtension(String path) {
        String extension = extension(path);
        return extension != null && EXTENSIONS.contains(extension);
    }

    public static String contentType(String path) {
        String extension = extension(path);
        if (extension == null) {
            return "application/octet-stream";
        }
        return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    private static String extension(String path) {
        if (path == null) {
            return null;
        }
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) {
            return null;
        }
        return path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripQuery(String url) {
        int cut = url.indexOf('?');
        return cut < 0 ? url : url.substring(0, cut);
    }

    /** Percent-encodes each path segment, leaving the separators intact. */
    private static String encodePath(String relative) {
        StringBuilder out = new StringBuilder();
        String[] segments = relative.split("/");
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                out.append('/');
            }
            out.append(java.net.URLEncoder.encode(segments[i], java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20"));
        }
        return out.toString();
    }
}
