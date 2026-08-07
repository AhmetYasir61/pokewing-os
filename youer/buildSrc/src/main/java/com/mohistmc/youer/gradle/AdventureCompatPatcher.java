package com.mohistmc.youer.gradle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Restores the pieces of the Adventure 4.x API that Adventure 5 removed or re-typed, so plugins built
 * against 4.x keep linking against the 5.x artifacts Youer ships.
 *
 * <p>Adventure 5 is a breaking release. A plugin compiled against 4.x resolves its call sites by name
 * <em>and</em> descriptor, so a rename or a type change is a hard {@code NoSuchMethodError} /
 * {@code NoSuchFieldError} at class initialization — not something the plugin can guard against.
 * ItemsAdder 4.0.17 trips over three of these. Each shim below is a faithful redirect to the member
 * that replaced it, verified against the two versions' own javadoc:
 *
 * <table>
 *   <caption>Restored members</caption>
 *   <tr><th>Adventure 4.x</th><th>Adventure 5.x</th></tr>
 *   <tr><td>{@code GsonComponentSerializer.Builder#legacyHoverEventSerializer(gson.LegacyHoverEventSerializer)}</td>
 *       <td>the {@code json.LegacyHoverEventSerializer} overload; the {@code gson} interface is gone</td></tr>
 *   <tr><td>{@code JSONOptions.HoverEventValueMode.MODERN_ONLY / LEGACY_ONLY / BOTH}</td>
 *       <td>renamed to {@code CAMEL_CASE / VALUE_FIELD / ALL}</td></tr>
 *   <tr><td>{@code ClickEvent.Action} constants, and {@code ClickEvent#clickEvent(Action, String)}</td>
 *       <td>{@code Action} became a generic class hierarchy, so the constants are typed as their
 *           subclasses and the factory takes a {@code Payload}</td></tr>
 * </table>
 *
 * <p>Each patch is skipped when the artifact already exposes the 4.x member, so running this against
 * an Adventure 4.x jar copies it through untouched rather than corrupting it.
 *
 * <p>Deliberately free of Gradle types so it can be exercised directly from a plain {@code main}.
 */
public final class AdventureCompatPatcher {
    /** An Adventure artifact this patcher knows how to fix up. */
    public enum Module {
        API,
        JSON_SERIALIZER,
        GSON_SERIALIZER;

        /** Resolves the module from a Maven artifact id, or null when the artifact needs no patching. */
        public static Module forArtifact(String artifactId) {
            return switch (artifactId) {
                case "adventure-api" -> API;
                case "adventure-text-serializer-json" -> JSON_SERIALIZER;
                case "adventure-text-serializer-gson" -> GSON_SERIALIZER;
                default -> null;
            };
        }
    }

    private static final String CLICK_EVENT = "net/kyori/adventure/text/event/ClickEvent";
    private static final String ACTION = CLICK_EVENT + "$Action";
    private static final String PAYLOAD = CLICK_EVENT + "$Payload";
    private static final String PAYLOAD_TEXT = PAYLOAD + "$Text";
    private static final String HOVER_MODE = "net/kyori/adventure/text/serializer/json/JSONOptions$HoverEventValueMode";
    private static final String GSON_BUILDER = "net/kyori/adventure/text/serializer/gson/GsonComponentSerializer$Builder";
    private static final String GSON_LEGACY = "net/kyori/adventure/text/serializer/gson/LegacyHoverEventSerializer";
    private static final String JSON_LEGACY = "net/kyori/adventure/text/serializer/json/LegacyHoverEventSerializer";

    /** A static field under its 4.x name and type, forwarding to the constant that replaced it. */
    private record Alias(String name, String descriptor, String targetName, String targetDescriptor) {}

    private AdventureCompatPatcher() {}

    /**
     * Reads the Adventure artifact at {@code input} and writes a compatibility-patched copy to
     * {@code output}, or copies it verbatim when nothing needs restoring.
     *
     * @return true if anything was actually patched
     */
    public static boolean patch(Module module, Path input, Path output) throws IOException {
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }

        Map<String, UnaryOperator<byte[]>> transforms = new LinkedHashMap<>();
        Map<String, byte[]> additions = new LinkedHashMap<>();

        try (ZipFile in = new ZipFile(input.toFile())) {
            switch (module) {
                case GSON_SERIALIZER -> {
                    // The gson-package marker interface only exists on 4.x, so it doubles as the probe.
                    if (in.getEntry(GSON_LEGACY + ".class") == null) {
                        transforms.put(GSON_BUILDER, AdventureCompatPatcher::addLegacyHoverOverload);
                        additions.put(GSON_LEGACY, legacyHoverEventSerializerInterface());
                    }
                }
                case JSON_SERIALIZER -> {
                    byte[] mode = read(in, HOVER_MODE);
                    String self = "L" + HOVER_MODE + ";";
                    if (mode != null && !hasField(mode, "MODERN_ONLY", self)) {
                        List<Alias> aliases = List.of(
                                new Alias("MODERN_ONLY", self, "CAMEL_CASE", self),
                                new Alias("LEGACY_ONLY", self, "VALUE_FIELD", self),
                                new Alias("BOTH", self, "ALL", self));
                        transforms.put(HOVER_MODE, bytes -> addAliasFields(bytes, aliases));
                    }
                }
                case API -> {
                    byte[] action = read(in, ACTION);
                    String self = "L" + ACTION + ";";
                    if (action != null && !hasField(action, "OPEN_URL", self)) {
                        // Adventure 5 kept the constant names but re-typed each one to its own subclass,
                        // so re-declare them under the plain Action type they used to have.
                        List<Alias> aliases = new ArrayList<>();
                        for (String[] pair : new String[][] {
                                {"OPEN_URL", "OpenUrl"}, {"OPEN_FILE", "OpenFile"},
                                {"RUN_COMMAND", "RunCommand"}, {"SUGGEST_COMMAND", "SuggestCommand"},
                                {"CHANGE_PAGE", "ChangePage"}, {"COPY_TO_CLIPBOARD", "CopyToClipboard"}}) {
                            aliases.add(new Alias(pair[0], self, pair[0], "L" + ACTION + "$" + pair[1] + ";"));
                        }
                        transforms.put(ACTION, bytes -> addAliasFields(bytes, aliases));
                        transforms.put(CLICK_EVENT, AdventureCompatPatcher::addStringClickEventFactory);
                    }
                }
            }

            if (transforms.isEmpty() && additions.isEmpty()) {
                Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
                return false;
            }

            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(output))) {
                Enumeration<? extends ZipEntry> entries = in.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory() || isSignatureFile(entry.getName())) {
                        // Signatures cover the original bytes and would fail validation after patching.
                        continue;
                    }
                    byte[] data = readAll(in, entry);
                    String className = entry.getName().endsWith(".class")
                            ? entry.getName().substring(0, entry.getName().length() - ".class".length())
                            : null;
                    UnaryOperator<byte[]> transform = className == null ? null : transforms.get(className);
                    write(out, entry.getName(), transform == null ? data : transform.apply(data));
                }
                for (Map.Entry<String, byte[]> added : additions.entrySet()) {
                    write(out, added.getKey() + ".class", added.getValue());
                }
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- transforms

    /**
     * Re-adds {@code default Builder legacyHoverEventSerializer(gson.LegacyHoverEventSerializer)},
     * delegating to the surviving {@code json} overload exactly as Adventure 4.x did.
     */
    private static byte[] addLegacyHoverOverload(byte[] original) {
        return transform(original, cv -> new ClassVisitor(Opcodes.ASM9, cv) {
            @Override
            public void visitEnd() {
                String gsonDesc = "(L" + GSON_LEGACY + ";)L" + GSON_BUILDER + ";";
                String jsonDesc = "(L" + JSON_LEGACY + ";)L" + GSON_BUILDER + ";";
                MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "legacyHoverEventSerializer", gsonDesc, null, null);
                mv.visitAnnotation("Ljava/lang/Deprecated;", true).visitEnd();
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, GSON_BUILDER, "legacyHoverEventSerializer", jsonDesc, true);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
                super.visitEnd();
            }
        });
    }

    /**
     * Adds each alias as a real static field and assigns it from the constant that replaced it at the
     * end of {@code <clinit>}. The JVM keys fields on name <em>and</em> descriptor, so an alias may
     * reuse a name that already exists under a different type — which is exactly what
     * {@code ClickEvent.Action} needs.
     */
    private static byte[] addAliasFields(byte[] original, List<Alias> aliases) {
        return transform(original, cv -> new ClassVisitor(Opcodes.ASM9, cv) {
            private String owner;
            private boolean sawClinit;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.owner = name;
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"<clinit>".equals(name)) {
                    return mv;
                }
                sawClinit = true;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.RETURN) {
                            assignAliases(this, owner, aliases);
                        }
                        super.visitInsn(opcode);
                    }
                };
            }

            @Override
            public void visitEnd() {
                for (Alias alias : aliases) {
                    FieldVisitor fv = cv.visitField(
                            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                            alias.name(), alias.descriptor(), null, null);
                    fv.visitAnnotation("Ljava/lang/Deprecated;", true).visitEnd();
                    fv.visitEnd();
                }
                if (!sawClinit) {
                    MethodVisitor mv = cv.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                    mv.visitCode();
                    assignAliases(mv, owner, aliases);
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }
                super.visitEnd();
            }
        });
    }

    private static void assignAliases(MethodVisitor mv, String owner, List<Alias> aliases) {
        for (Alias alias : aliases) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, owner, alias.targetName(), alias.targetDescriptor());
            mv.visitFieldInsn(Opcodes.PUTSTATIC, owner, alias.name(), alias.descriptor());
        }
    }

    /**
     * Re-adds {@code ClickEvent.clickEvent(Action, String)}. Adventure 5 replaced the string argument
     * with a {@code Payload}, so wrap it the way {@code ClickEvent.runCommand} and friends do.
     */
    private static byte[] addStringClickEventFactory(byte[] original) {
        return transform(original, cv -> new ClassVisitor(Opcodes.ASM9, cv) {
            @Override
            public void visitEnd() {
                String legacyDesc = "(L" + ACTION + ";Ljava/lang/String;)L" + CLICK_EVENT + ";";
                String modernDesc = "(L" + ACTION + ";L" + PAYLOAD + ";)L" + CLICK_EVENT + ";";
                MethodVisitor mv = cv.visitMethod(
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "clickEvent", legacyDesc, null, null);
                mv.visitAnnotation("Ljava/lang/Deprecated;", true).visitEnd();
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, PAYLOAD, "string",
                        "(Ljava/lang/String;)L" + PAYLOAD_TEXT + ";", true);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, CLICK_EVENT, "clickEvent", modernDesc, false);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
                super.visitEnd();
            }
        });
    }

    /** Regenerates the deprecated {@code gson.LegacyHoverEventSerializer} marker interface. */
    private static byte[] legacyHoverEventSerializerInterface() {
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

    // ---------------------------------------------------------------- plumbing

    private static byte[] transform(byte[] original, UnaryOperator<ClassVisitor> wrapper) {
        ClassReader reader = new ClassReader(original);
        // COMPUTE_MAXS only: every method added here is straight-line code, so the existing stack map
        // frames stay valid and we avoid COMPUTE_FRAMES needing to load Adventure's class hierarchy.
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(wrapper.apply(writer), 0);
        return writer.toByteArray();
    }

    private static boolean hasField(byte[] classBytes, String name, String descriptor) {
        boolean[] found = {false};
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public FieldVisitor visitField(int access, String fieldName, String fieldDescriptor, String signature, Object value) {
                if (fieldName.equals(name) && fieldDescriptor.equals(descriptor)) {
                    found[0] = true;
                }
                return null;
            }
        }, ClassReader.SKIP_CODE);
        return found[0];
    }

    private static byte[] read(ZipFile zip, String className) throws IOException {
        ZipEntry entry = zip.getEntry(className + ".class");
        return entry == null ? null : readAll(zip, entry);
    }

    private static byte[] readAll(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream is = zip.getInputStream(entry)) {
            return is.readAllBytes();
        }
    }

    private static boolean isSignatureFile(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        return upper.startsWith("META-INF/")
                && (upper.endsWith(".SF") || upper.endsWith(".DSA") || upper.endsWith(".RSA") || upper.endsWith(".EC"));
    }

    private static void write(ZipOutputStream out, String name, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        // Fixed timestamp so repeated builds produce identical jars (the launcher checksums them).
        entry.setTime(0L);
        out.putNextEntry(entry);
        out.write(data);
        out.closeEntry();
    }

    /** Standalone entry point: {@code <module> <input jar> <output jar>}. */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("usage: AdventureCompatPatcher <API|JSON_SERIALIZER|GSON_SERIALIZER> <input.jar> <output.jar>");
            System.exit(2);
        }
        Module module = Module.valueOf(args[0]);
        boolean patched = patch(module, Path.of(args[1]), Path.of(args[2]));
        System.out.println((patched ? "patched " : "already compatible, copied ") + args[2]);
    }
}
