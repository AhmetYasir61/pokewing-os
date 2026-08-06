package com.pokewing.swm.web;

import com.google.gson.JsonObject;
import com.pokewing.swm.SpotifyWorldMusic;
import com.pokewing.swm.config.MusicZone;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.pokewing.swm.spotify.AuthService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Small embedded HTTP server: it serves the browser player page, an endpoint the
 * page polls to learn what should be playing, and the Spotify OAuth callback.
 *
 * <p>Handlers run on their own thread pool and therefore never touch the Bukkit
 * API - they only read the thread-safe snapshots kept by
 * {@link com.pokewing.swm.MusicService}.
 */
public final class WebServer {

    private final SpotifyWorldMusic plugin;
    private HttpServer server;
    private String playerPage;

    public WebServer(SpotifyWorldMusic plugin) {
        this.plugin = plugin;
    }

    public void start() throws IOException {
        playerPage = readResource("web/player.html");

        server = HttpServer.create(
                new InetSocketAddress(plugin.config().webBind(), plugin.config().webPort()), 0);
        server.setExecutor(Executors.newFixedThreadPool(4, namedThreads()));

        server.createContext("/", this::handlePlayerPage);
        server.createContext("/api/state", this::handleState);
        server.createContext("/api/report", this::handleReport);
        server.createContext("/callback", this::handleCallback);
        server.createContext("/health", exchange -> send(exchange, 200, "text/plain", "ok"));
        server.start();

        plugin.getLogger().info("Web player listening on "
                + plugin.config().webBind() + ":" + plugin.config().webPort()
                + " (public url: " + plugin.config().publicUrl() + ")");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    // ---------------------------------------------------------------- handlers

    private void handlePlayerPage(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!"/".equals(path) && !"/player".equals(path) && !"/index.html".equals(path)) {
            send(exchange, 404, "text/plain; charset=utf-8", "Bulunamadi");
            return;
        }
        String page = playerPage
                .replace("%%POLL_SECONDS%%", String.valueOf(plugin.config().webPollSeconds()))
                .replace("%%SERVER_NAME%%", escapeHtml(plugin.serverDisplayName()));
        send(exchange, 200, "text/html; charset=utf-8", page);
    }

    private void handleState(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        UUID uuid = plugin.webTokens().resolve(query.get("t"));

        JsonObject json = new JsonObject();
        if (uuid == null) {
            json.addProperty("ok", false);
            json.addProperty("reason", "unknown-token");
            sendJson(exchange, 200, json);
            return;
        }

        json.addProperty("ok", true);
        json.addProperty("online", plugin.music().isTracked(uuid));
        json.addProperty("version", plugin.music().version(uuid));
        json.addProperty("playerName", plugin.knownName(uuid));

        MusicZone zone = plugin.music().zoneOf(uuid);
        if (zone == null) {
            json.addProperty("playing", false);
            sendJson(exchange, 200, json);
            return;
        }
        json.addProperty("playing", true);
        json.addProperty("zone", zone.id());
        json.addProperty("zoneName", stripColors(zone.displayName()));
        json.addProperty("uri", zone.contextUri());
        json.addProperty("url", zone.rawUrl());
        json.addProperty("type", zone.contextType());
        json.addProperty("loop", zone.loop());
        json.addProperty("volume", zone.volume());
        json.addProperty("seekMs", plugin.music().syncPositionMs(zone));
        sendJson(exchange, 200, json);
    }

    private void handleReport(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        UUID uuid = plugin.webTokens().resolve(query.get("t"));
        JsonObject json = new JsonObject();
        if (uuid == null) {
            json.addProperty("ok", false);
            sendJson(exchange, 200, json);
            return;
        }
        String zoneId = query.get("zone");
        long duration = parseLong(query.get("duration"), 0L);
        plugin.music().reportDuration(zoneId, duration);
        json.addProperty("ok", true);
        sendJson(exchange, 200, json);
    }

    private void handleCallback(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String error = query.get("error");
        if (error != null) {
            sendResultPage(exchange, false, "Spotify izni verilmedi: " + error);
            return;
        }
        if (plugin.auth() == null) {
            sendResultPage(exchange, false, "Sunucuda Spotify API modu kapali.");
            return;
        }
        AuthService.CallbackResult result =
                plugin.auth().completeLink(query.get("code"), query.get("state"));
        if (!result.ok()) {
            sendResultPage(exchange, false, result.message());
            return;
        }
        plugin.onPlayerLinked(result.uuid(), result.message());
        sendResultPage(exchange, true,
                "Spotify hesabin (" + result.message() + ") baglandi. Oyuna donebilirsin.");
    }

    // ---------------------------------------------------------------- plumbing

    private void sendResultPage(HttpExchange exchange, boolean ok, String message) throws IOException {
        String accent = ok ? "#1DB954" : "#e2554f";
        String title = ok ? "Baglandi" : "Baglanamadi";
        String html = """
                <!doctype html>
                <html lang="tr"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s</title>
                <style>
                  body { margin:0; min-height:100vh; display:flex; align-items:center;
                         justify-content:center; background:#101014; color:#f4f4f5;
                         font-family: system-ui, -apple-system, "Segoe UI", sans-serif; }
                  .card { max-width:28rem; padding:2rem; border-radius:1rem; background:#18181c;
                          border:1px solid #2a2a31; text-align:center; }
                  h1 { margin:0 0 .75rem; font-size:1.35rem; color:%s; }
                  p { margin:0; line-height:1.6; color:#c9c9cf; }
                </style></head>
                <body><div class="card"><h1>%s</h1><p>%s</p></div></body></html>
                """.formatted(title, accent, title, escapeHtml(message));
        send(exchange, ok ? 200 : 400, "text/html; charset=utf-8", html);
    }

    private void sendJson(HttpExchange exchange, int status, JsonObject json) throws IOException {
        exchange.getResponseHeaders().add("Cache-Control", "no-store");
        send(exchange, status, "application/json; charset=utf-8", json.toString());
    }

    private void send(HttpExchange exchange, int status, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        } finally {
            exchange.close();
        }
    }

    /**
     * Reads a bundled page, preferring the copy inside the plugin data folder so
     * server owners can restyle it without rebuilding the jar.
     */
    private String readResource(String name) throws IOException {
        File override = new File(plugin.getDataFolder(), name);
        if (override.isFile()) {
            return Files.readString(override.toPath(), StandardCharsets.UTF_8);
        }
        try (InputStream in = plugin.getResource(name)) {
            if (in == null) {
                throw new IOException("Bundled resource missing: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> result = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return result;
        }
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            result.put(key, value);
        }
        return result;
    }

    private static long parseLong(String value, long fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String stripColors(String text) {
        return text == null ? "" : text.replaceAll("[&§][0-9a-fk-orA-FK-OR]", "");
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private ThreadFactory namedThreads() {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, "SpotifyWorldMusic-http-" + counter.getAndIncrement());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((t, error) ->
                    plugin.getLogger().log(Level.WARNING, "Web server thread failed", error));
            return thread;
        };
    }
}
