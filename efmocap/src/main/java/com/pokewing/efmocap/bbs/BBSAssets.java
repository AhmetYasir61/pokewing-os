package com.pokewing.efmocap.bbs;

import com.pokewing.efmocap.EFMocap;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;

/**
 * Installs EFMocap content into BBS's own asset tree, the way Emoticons does.
 *
 * <p>BBS scans {@code config/bbs/assets/models} and turns every folder there
 * into a category in its form picker — that's where "Modeller (emoticons)" and
 * "Modeller (player)" come from. So instead of asking anyone to copy files by
 * hand, everything under {@code config/efmocap/bbs-models} is mirrored into
 * {@code config/bbs/assets/models/efmocap} at startup, giving EFMocap its own
 * category next to theirs.</p>
 *
 * <p>Epic Fight animations exported with {@code /efmocap bbsexport} land in the
 * same place, so a model installed here arrives with its animations already
 * beside it — which is what BBS's loaders expect.</p>
 */
public final class BBSAssets {
    private BBSAssets() {}

    /** Where BBS keeps the models it offers in the form picker. */
    public static Path bbsModelsDir() {
        return FMLPaths.CONFIGDIR.get().resolve("bbs").resolve("assets").resolve("models");
    }

    /** Our category inside BBS. */
    public static Path installedDir() {
        return bbsModelsDir().resolve("efmocap");
    }

    /**
     * The actor model shipped in our jar under
     * {@code assets/bbs/assets/models/efmocap/actor}. BBS reads the geometry
     * from there; this is the writable overlay beside it, which is where its
     * animations have to live.
     */
    public static Path actorDir() {
        return installedDir().resolve("actor");
    }

    /** Drop models here; they get mirrored into BBS on the next launch. */
    public static Path sourceDir() {
        return FMLPaths.CONFIGDIR.get().resolve("efmocap").resolve("bbs-models");
    }

    /** Same folder, created first — for the editor's "open folder" button. */
    public static Path sourceDirEnsured() {
        Path p = sourceDir();
        try { Files.createDirectories(p); } catch (Throwable ignored) {}
        return p;
    }

    /**
     * Mirror the source folder into BBS's models tree.
     *
     * <p>Copies only what changed, and never deletes: BBS writes into its own
     * folder too (its editor saves there), so a mirror that pruned would eat
     * work done inside BBS.</p>
     *
     * @return how many files were written, or -1 when BBS isn't installed
     */
    public static int install() {
        if (!BBSBridge.isLoaded()) return -1;

        Path src = sourceDir();
        Path dst = installedDir();
        int[] written = {0};
        try {
            Files.createDirectories(src);
            Files.createDirectories(dst);

            Files.walkFileTree(src, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    if (!isAsset(file)) return FileVisitResult.CONTINUE;
                    Path target = dst.resolve(src.relativize(file).toString());
                    if (Files.exists(target)
                            && Files.getLastModifiedTime(target).toMillis()
                               >= attrs.lastModifiedTime().toMillis()) {
                        return FileVisitResult.CONTINUE;
                    }
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES);
                    written[0]++;
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Throwable t) {
            EFMocap.LOG.warn("[efmocap] could not install models into BBS", t);
            return written[0];
        }

        EFMocap.LOG.info("[efmocap] installed {} file(s) into {}", written[0], dst);
        return written[0];
    }

    /** Only the file types BBS's model loaders read. */
    private static boolean isAsset(Path p) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".json") || n.endsWith(".png") || n.endsWith(".obj")
                || n.endsWith(".mtl");
    }
}
