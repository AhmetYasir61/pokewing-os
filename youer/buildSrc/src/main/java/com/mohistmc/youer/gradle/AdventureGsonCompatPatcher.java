package com.mohistmc.youer.gradle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Restores the {@code gson}-package legacy hover event API that Adventure removed in 5.0.0.
 *
 * <p>Adventure 4.x exposed two things that plugins compiled before Adventure 5 still reference:
 * <ul>
 *   <li>the marker interface {@code net.kyori.adventure.text.serializer.gson.LegacyHoverEventSerializer},
 *       a deprecated subtype of {@code ...serializer.json.LegacyHoverEventSerializer}, and</li>
 *   <li>the {@code default} overload
 *       {@code GsonComponentSerializer.Builder#legacyHoverEventSerializer(gson.LegacyHoverEventSerializer)},
 *       which simply widened its argument and delegated to the {@code json} overload.</li>
 * </ul>
 *
 * <p>Both were dropped in Adventure 5, so a plugin built against 4.x dies with
 * {@code NoSuchMethodError: ...Builder.legacyHoverEventSerializer(...gson.LegacyHoverEventSerializer)}
 * the first time it touches a component serializer. ItemsAdder 4.0.16 is one such plugin.
 *
 * <p>This patcher rewrites the {@code adventure-text-serializer-gson} artifact on the way into the
 * installer jar, adding the interface back and re-adding the default method with the exact body the
 * 4.x sources had. Everything else in the artifact is copied through byte for byte.
 *
 * <p>Deliberately free of Gradle types so it can be exercised directly from a plain {@code main}.
 */
public final class AdventureGsonCompatPatcher {
    private static final String BUILDER = "net/kyori/adventure/text/serializer/gson/GsonComponentSerializer$Builder";
    private static final String GSON_LEGACY = "net/kyori/adventure/text/serializer/gson/LegacyHoverEventSerializer";
    private static final String JSON_LEGACY = "net/kyori/adventure/text/serializer/json/LegacyHoverEventSerializer";
    private static final String GSON_DESC = "(L" + GSON_LEGACY + ";)L" + BUILDER + ";";
    private static final String JSON_DESC = "(L" + JSON_LEGACY + ";)L" + BUILDER + ";";

    private AdventureGsonCompatPatcher() {}

    /**
     * Reads the Adventure gson serializer at {@code input} and writes a compatibility-patched copy to
     * {@code output}. If the artifact already carries the 4.x API the input is copied verbatim, so
     * running this against an Adventure 4.x jar is a no-op rather than a corruption.
     *
     * @return true if anything was actually patched
     */
    public static boolean patch(Path input, Path output) throws IOException {
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }

        try (ZipFile in = new ZipFile(input.toFile())) {
            if (in.getEntry(GSON_LEGACY + ".class") != null) {
                Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
                return false;
            }

            ZipEntry builderEntry = in.getEntry(BUILDER + ".class");
            if (builderEntry == null) {
                throw new IOException("Not an adventure-text-serializer-gson artifact, " + BUILDER + " is missing: " + input);
            }

            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(output))) {
                Enumeration<? extends ZipEntry> entries = in.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) {
                        continue;
                    }
                    if (entry.getName().equals(builderEntry.getName())) {
                        write(out, entry.getName(), addLegacyOverload(readAll(in, entry)));
                    } else if (isSignatureFile(entry.getName())) {
                        // The jar is no longer byte-identical, so any bundled signature would fail validation.
                        continue;
                    } else {
                        write(out, entry.getName(), readAll(in, entry));
                    }
                }
                write(out, GSON_LEGACY + ".class", generateLegacyInterface());
            }
        }
        return true;
    }

    private static boolean isSignatureFile(String name) {
        String upper = name.toUpperCase(java.util.Locale.ROOT);
        return upper.startsWith("META-INF/")
                && (upper.endsWith(".SF") || upper.endsWith(".DSA") || upper.endsWith(".RSA") || upper.endsWith(".EC"));
    }

    private static byte[] readAll(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream is = zip.getInputStream(entry)) {
            return is.readAllBytes();
        }
    }

    private static void write(ZipOutputStream out, String name, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        // Fixed timestamp so repeated builds produce identical jars (the launcher checksums them).
        entry.setTime(0L);
        out.putNextEntry(entry);
        out.write(data);
        out.closeEntry();
    }

    /**
     * Re-adds {@code default Builder legacyHoverEventSerializer(gson.LegacyHoverEventSerializer)},
     * delegating to the surviving {@code json} overload exactly as Adventure 4.x did.
     */
    private static byte[] addLegacyOverload(byte[] original) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public void visitEnd() {
                MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, "legacyHoverEventSerializer", GSON_DESC, null, null);
                mv.visitAnnotation("Ljava/lang/Deprecated;", true).visitEnd();
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, BUILDER, "legacyHoverEventSerializer", JSON_DESC, true);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(2, 2);
                mv.visitEnd();
                super.visitEnd();
            }
        }, 0);
        return writer.toByteArray();
    }

    /** Regenerates the deprecated {@code gson.LegacyHoverEventSerializer} marker interface. */
    private static byte[] generateLegacyInterface() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(
                Opcodes.V17,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_DEPRECATED,
                GSON_LEGACY,
                null,
                "java/lang/Object",
                new String[] {JSON_LEGACY});
        writer.visitAnnotation("Ljava/lang/Deprecated;", true).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    /** Standalone entry point: {@code <input jar> <output jar>}. */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("usage: AdventureGsonCompatPatcher <input.jar> <output.jar>");
            System.exit(2);
        }
        boolean patched = patch(Path.of(args[0]), Path.of(args[1]));
        System.out.println(patched ? "patched " + args[1] : "already compatible, copied " + args[1]);
    }
}
