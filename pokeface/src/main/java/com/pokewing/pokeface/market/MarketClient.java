package com.pokewing.pokeface.market;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pokewing.pokeface.PokeFace;
import com.pokewing.pokeface.model.ModelLibrary;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Talks to the storefront: lists what is for sale, and fetches what is owned.
 *
 * <p>Every request carries the store session token and nothing else. No payment
 * information passes through here — buying happens on the store's page (see
 * {@link Checkout}) and this only ever asks "what is for sale" and "may I have
 * the file I paid for".
 *
 * <p>All calls are off the render thread; callers get a future and update the
 * screen when it lands, so a slow store never freezes the game.
 */
public final class MarketClient {

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private MarketClient() {
    }

    public static CompletableFuture<List<MarketItem>> catalog() {
        MarketConfig config = MarketConfig.get();
        if (config.catalogUrl == null || config.catalogUrl.isBlank()) {
            return CompletableFuture.completedFuture(List.of());
        }
        HttpRequest request = authorised(HttpRequest.newBuilder(URI.create(config.catalogUrl))
                .timeout(Duration.ofSeconds(20)).GET()).build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        PokeFace.LOGGER.warn("PokeFace: store returned {} for the catalog",
                                response.statusCode());
                        return List.<MarketItem>of();
                    }
                    List<MarketItem> items = GSON.fromJson(response.body(),
                            new TypeToken<List<MarketItem>>() {}.getType());
                    return items == null ? List.<MarketItem>of() : markInstalled(items);
                })
                .exceptionally(t -> {
                    PokeFace.LOGGER.warn("PokeFace: could not reach the store: {}", t.toString());
                    return List.of();
                });
    }

    private static List<MarketItem> markInstalled(List<MarketItem> items) {
        List<String> installed = ModelLibrary.names();
        List<MarketItem> out = new ArrayList<>(items);
        for (MarketItem item : out) {
            item.installed = installed.contains(stripExtension(item.fileName))
                    || installed.contains(item.id);
        }
        return out;
    }

    /**
     * Downloads an owned cosmetic into the models folder.
     *
     * <p>A pack is a zip of the same {@code name.obj} + {@code name.png} pairing
     * the folder already uses, so an installed purchase and a hand-made model are
     * the same thing to the rest of the mod. Entries are written by their own
     * file name only, so a malicious archive cannot walk out of the folder.
     */
    public static CompletableFuture<Boolean> install(MarketItem item) {
        if (item.downloadUrl == null || item.downloadUrl.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }
        HttpRequest request = authorised(HttpRequest.newBuilder(URI.create(item.downloadUrl))
                .timeout(Duration.ofMinutes(2)).GET()).build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        PokeFace.LOGGER.warn("PokeFace: download refused ({}) for {} - is it paid for?",
                                response.statusCode(), item.name);
                        return false;
                    }
                    try (InputStream in = response.body()) {
                        Path dir = ModelLibrary.directory();
                        Files.createDirectories(dir);
                        if (item.fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")) {
                            unzipInto(in, dir);
                        } else {
                            Files.copy(in, dir.resolve(safeName(item.fileName)),
                                    StandardCopyOption.REPLACE_EXISTING);
                        }
                        return true;
                    } catch (Exception e) {
                        PokeFace.LOGGER.warn("PokeFace: could not install {}: {}", item.name, e.toString());
                        return false;
                    }
                })
                .exceptionally(t -> {
                    PokeFace.LOGGER.warn("PokeFace: download failed: {}", t.toString());
                    return false;
                });
    }

    private static void unzipInto(InputStream in, Path dir) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                Path target = dir.resolve(safeName(entry.getName()));
                Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /** Flattens any path in an archive entry to its file name. */
    private static String safeName(String name) {
        String cleaned = name.replace('\\', '/');
        int slash = cleaned.lastIndexOf('/');
        if (slash >= 0) {
            cleaned = cleaned.substring(slash + 1);
        }
        return cleaned.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private static String stripExtension(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot < 0 ? (fileName == null ? "" : fileName) : fileName.substring(0, dot);
    }

    /**
     * Publishes a local model to the configured endpoint, with the price the
     * creator set. What the store does with it — review, listing, payouts — is
     * the store's business; this only hands over the files and the asking price.
     */
    public static CompletableFuture<String> publish(String modelName, double price, String currency) {
        MarketConfig config = MarketConfig.get();
        if (config.publishUrl == null || config.publishUrl.isBlank()) {
            return CompletableFuture.completedFuture("no publish endpoint configured");
        }
        Path dir = ModelLibrary.directory();
        Path obj = dir.resolve(modelName + ".obj");
        Path png = dir.resolve(modelName + ".png");
        if (!Files.exists(obj) || !Files.exists(png)) {
            return CompletableFuture.completedFuture("model files not found");
        }
        try {
            Path bundle = Files.createTempFile("pokeface-", ".zip");
            try (var zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(bundle))) {
                writeEntry(zip, modelName + ".obj", Files.readAllBytes(obj));
                writeEntry(zip, modelName + ".png", Files.readAllBytes(png));
            }
            HttpRequest request = authorised(HttpRequest.newBuilder(URI.create(config.publishUrl))
                    .timeout(Duration.ofMinutes(2))
                    .header("Content-Type", "application/zip")
                    .header("X-PokeFace-Name", modelName)
                    .header("X-PokeFace-Price", String.valueOf(price))
                    .header("X-PokeFace-Currency", currency)
                    .POST(HttpRequest.BodyPublishers.ofFile(bundle))).build();

            return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> response.statusCode() / 100 == 2
                            ? "published" : "store said " + response.statusCode())
                    .exceptionally(t -> "failed: " + t.getMessage());
        } catch (Exception e) {
            return CompletableFuture.completedFuture("failed: " + e.getMessage());
        }
    }

    private static void writeEntry(java.util.zip.ZipOutputStream zip, String name, byte[] data)
            throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    private static HttpRequest.Builder authorised(HttpRequest.Builder builder) {
        String token = MarketConfig.get().token();
        if (!token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder.header("User-Agent", "PokeFace-Market");
    }
}
