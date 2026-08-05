package com.pokewing.efbbs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Standalone converter -- runs OUTSIDE Minecraft. It reads Epic Fight's bundled
 * animation JSON files (from the Epic Fight jar, or an unzipped folder) and
 * writes BBS-ready Bedrock animation JSON. Handy for batch-converting the built
 * in combat animations without launching the game.
 *
 * <p>Usage:</p>
 * <pre>
 *   java -cp gson.jar:efbbs.jar com.pokewing.efbbs.OfflineConverter \
 *        &lt;epicfight.jar | animations-folder&gt; &lt;output-folder&gt; [retarget.json]
 * </pre>
 *
 * <p>Note: for animations added by resource packs (e.g. "EF Plus / Pierced
 * Animations"), use the in-game {@code /efbbs exportall} command instead -- it
 * reads the live registry and therefore includes resource-pack overrides.</p>
 */
public final class OfflineConverter {
    private static final String ANIM_PREFIX = "assets/epicfight/animmodels/animations/";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: OfflineConverter <epicfight.jar|folder> <output-folder> [retarget.json]");
            return;
        }
        Path input = Paths.get(args[0]);
        Path outDir = Paths.get(args[1]);
        Files.createDirectories(outDir);
        RetargetConfig cfg = RetargetConfig.fromFileOrDefaults(
                args.length >= 3 ? Paths.get(args[2]) : null);

        int ok = 0, fail = 0;
        if (input.toString().endsWith(".jar") || input.toString().endsWith(".zip")) {
            try (ZipFile zip = new ZipFile(input.toFile())) {
                Enumeration<? extends ZipEntry> e = zip.entries();
                while (e.hasMoreElements()) {
                    ZipEntry entry = e.nextElement();
                    String n = entry.getName();
                    if (!n.startsWith(ANIM_PREFIX) || !n.endsWith(".json")) continue;
                    if (n.contains("/data/")) continue; // datapack sub-files
                    String name = n.substring(ANIM_PREFIX.length(), n.length() - 5).replace('/', '.');
                    try (InputStream is = zip.getInputStream(entry)) {
                        if (convertOne(name, is, cfg, outDir)) ok++; else fail++;
                    } catch (Exception ex) {
                        fail++;
                        System.err.println("skip " + name + ": " + ex.getMessage());
                    }
                }
            }
        } else {
            List<Path> files = new ArrayList<>();
            try (Stream<Path> s = Files.walk(input)) {
                s.filter(p -> p.toString().endsWith(".json"))
                 .filter(p -> !p.toString().contains(File_data()))
                 .forEach(files::add);
            }
            for (Path p : files) {
                String name = input.relativize(p).toString()
                        .replace('\\', '.').replace('/', '.');
                if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
                try (InputStream is = Files.newInputStream(p)) {
                    if (convertOne(name, is, cfg, outDir)) ok++; else fail++;
                } catch (Exception ex) {
                    fail++;
                    System.err.println("skip " + name + ": " + ex.getMessage());
                }
            }
        }
        System.out.println("Done. Converted " + ok + " animations (" + fail + " skipped) -> " + outDir);
    }

    private static String File_data() {
        return java.io.File.separator + "data" + java.io.File.separator;
    }

    private static boolean convertOne(String name, InputStream is, RetargetConfig cfg, Path outDir)
            throws Exception {
        JsonObject json;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            json = JsonParser.parseReader(r).getAsJsonObject();
        }
        if (!json.has("animation") && !json.has("joints")) return false;
        EpicFightAccess.Extracted ex = EpicFightAccess.extractFromJson(name, json);
        if (ex.joints.isEmpty()) return false;
        AnimationExporter.writeExtracted(ex, cfg, outDir);
        return true;
    }
}
