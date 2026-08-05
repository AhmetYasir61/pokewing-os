package com.pokewing.efbbs;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Fully standalone converter -- runs OUTSIDE Minecraft with NO extra libraries.
 * Only efbbs.jar is needed on the classpath (no Gson, no Minecraft, no Forge):
 *
 * <pre>
 *   java -cp efbbs-forge-1.20.1-0.2.0.jar \
 *        com.pokewing.efbbs.OfflineConverter epic-fight-...jar out-folder [retarget.json]
 * </pre>
 *
 * <p>It reads Epic Fight's bundled animation JSON
 * ({@code assets/epicfight/animmodels/animations/**}) and writes BBS-ready
 * Bedrock animation JSON. For resource-pack animations (e.g. "EF Plus /
 * Pierced"), use the in-game {@code /efbbs exportall} command instead.</p>
 */
public final class OfflineConverter {
    private static final String ANIM_PREFIX = "assets/epicfight/animmodels/animations/";

    // Conversion settings (mirror RetargetConfig defaults). Overridable via an
    // optional retarget.json passed as the third argument.
    private Map<String, String> jointToBone = defaultMap();
    private double translationScale = 16.0;
    private double rotXSign = 1, rotYSign = 1, rotZSign = 1;
    private boolean rotationOnly = true; // limbs rotation-only (see RetargetConfig)
    private int decimals = 4;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java -cp efbbs.jar com.pokewing.efbbs.OfflineConverter "
                    + "<epicfight.jar|folder> <output-folder> [retarget.json]");
            return;
        }
        Path input = Paths.get(args[0]);
        Path outDir = Paths.get(args[1]);
        Files.createDirectories(outDir);

        OfflineConverter c = new OfflineConverter();
        if (args.length >= 3) c.loadRetarget(Paths.get(args[2]));

        int ok = 0, fail = 0;
        String name = input.getFileName().toString().toLowerCase();
        if (name.endsWith(".jar") || name.endsWith(".zip")) {
            try (ZipFile zip = new ZipFile(input.toFile())) {
                Enumeration<? extends ZipEntry> e = zip.entries();
                while (e.hasMoreElements()) {
                    ZipEntry entry = e.nextElement();
                    String n = entry.getName();
                    if (!n.startsWith(ANIM_PREFIX) || !n.endsWith(".json")) continue;
                    if (n.contains("/data/")) continue;
                    String an = n.substring(ANIM_PREFIX.length(), n.length() - 5).replace('/', '.');
                    try (InputStream is = zip.getInputStream(entry)) {
                        if (c.convertOne(an, readAll(is), outDir)) ok++; else fail++;
                    } catch (Exception ex) {
                        fail++;
                        System.err.println("skip " + an + ": " + ex.getMessage());
                    }
                }
            }
        } else {
            List<Path> files = new ArrayList<>();
            try (Stream<Path> s = Files.walk(input)) {
                s.filter(p -> p.toString().endsWith(".json")).forEach(files::add);
            }
            for (Path p : files) {
                String an = input.relativize(p).toString().replace('\\', '.').replace('/', '.');
                if (an.endsWith(".json")) an = an.substring(0, an.length() - 5);
                try {
                    if (c.convertOne(an, new String(Files.readAllBytes(p), StandardCharsets.UTF_8), outDir))
                        ok++;
                    else fail++;
                } catch (Exception ex) {
                    fail++;
                    System.err.println("skip " + an + ": " + ex.getMessage());
                }
            }
        }
        System.out.println("Done. Converted " + ok + " animations (" + fail + " skipped) -> " + outDir);
    }

    @SuppressWarnings("unchecked")
    private boolean convertOne(String animName, String json, Path outDir) throws Exception {
        Object root = MiniJson.parse(json);
        if (!(root instanceof Map<?, ?> obj)) return false;
        Object jointsObj = obj.get("animation");
        if (jointsObj == null) jointsObj = obj.get("joints");
        if (!(jointsObj instanceof List<?> joints) || joints.isEmpty()) return false;

        // joint name -> ordered frames of {time, tx,ty,tz, qx,qy,qz,qw}
        Map<String, List<double[]>> perJoint = new LinkedHashMap<>();
        double maxTime = 0;
        for (Object jo : joints) {
            Map<String, Object> j = (Map<String, Object>) jo;
            String jn = (String) j.get("name");
            List<Object> times = (List<Object>) j.get("time");
            List<Object> transforms = (List<Object>) j.get("transform");
            if (times == null || transforms == null) continue;
            List<double[]> frames = new ArrayList<>();
            for (int k = 0; k < times.size(); k++) {
                double t = ((Number) times.get(k)).doubleValue();
                List<Object> mat = (List<Object>) transforms.get(k);
                double[] m = new double[16];
                for (int q = 0; q < 16 && q < mat.size(); q++) m[q] = ((Number) mat.get(q)).doubleValue();
                float[] d = MatrixUtil.decompose(m);
                frames.add(new double[] {t, d[0], d[1], d[2], d[3], d[4], d[5], d[6]});
                if (t > maxTime) maxTime = t;
            }
            if (!frames.isEmpty()) perJoint.put(jn, frames);
        }
        if (perJoint.isEmpty()) return false;

        // Collapse joints onto bones (most-keyframes wins per bone).
        Map<String, List<double[]>> chosen = new LinkedHashMap<>();
        for (Map.Entry<String, List<double[]>> e : perJoint.entrySet()) {
            String bone = jointToBone.get(e.getKey());
            if (bone == null) continue;
            List<double[]> ex = chosen.get(bone);
            if (ex == null || e.getValue().size() > ex.size()) chosen.put(bone, e.getValue());
        }

        Map<String, Object> bones = new LinkedHashMap<>();
        for (Map.Entry<String, List<double[]>> e : chosen.entrySet()) {
            Map<String, Object> rotation = new LinkedHashMap<>();
            Map<String, Object> position = rotationOnly ? null : new LinkedHashMap<>();
            for (double[] f : e.getValue()) {
                String key = String.format(java.util.Locale.ROOT, "%.4f", f[0]);
                float[] euler = MathUtil.quaternionToEulerXYZDegrees(
                        (float) f[4], (float) f[5], (float) f[6], (float) f[7]);
                rotation.put(key, list(
                        round(euler[0] * rotXSign), round(euler[1] * rotYSign), round(euler[2] * rotZSign)));
                if (position != null) {
                    position.put(key, list(
                            round(f[1] * translationScale), round(f[2] * translationScale),
                            round(f[3] * translationScale)));
                }
            }
            Map<String, Object> boneObj = new LinkedHashMap<>();
            boneObj.put("rotation", rotation);
            if (position != null) boneObj.put("position", position);
            bones.put(e.getKey(), boneObj);
        }

        Map<String, Object> anim = new LinkedHashMap<>();
        anim.put("loop", Boolean.FALSE);
        anim.put("animation_length", round(maxTime));
        anim.put("bones", bones);
        Map<String, Object> animations = new LinkedHashMap<>();
        animations.put("efbbs." + animName.replaceAll("[^a-zA-Z0-9._]", "_"), anim);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("format_version", "1.8.0");
        out.put("animations", animations);

        String safe = animName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path file = outDir.resolve(safe + ".animation.json");
        Files.write(file, MiniJson.write(out).getBytes(StandardCharsets.UTF_8));
        return true;
    }

    @SuppressWarnings("unchecked")
    private void loadRetarget(Path path) {
        try {
            if (!Files.exists(path)) return;
            Object root = MiniJson.parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
            if (!(root instanceof Map<?, ?> m)) return;
            Object jtb = m.get("jointToBone");
            if (jtb instanceof Map<?, ?> map && !map.isEmpty()) {
                jointToBone = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : map.entrySet())
                    jointToBone.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
            if (m.get("translationScale") instanceof Number n) translationScale = n.doubleValue();
            if (m.get("rotXSign") instanceof Number n) rotXSign = n.doubleValue();
            if (m.get("rotYSign") instanceof Number n) rotYSign = n.doubleValue();
            if (m.get("rotZSign") instanceof Number n) rotZSign = n.doubleValue();
            if (m.get("rotationOnly") instanceof Boolean b) rotationOnly = b;
            if (m.get("decimals") instanceof Number n) decimals = n.intValue();
        } catch (Exception e) {
            System.err.println("Could not read retarget file, using defaults: " + e.getMessage());
        }
    }

    private double round(double v) {
        double f = Math.pow(10, decimals);
        return Math.round(v * f) / f;
    }

    private static List<Object> list(double a, double b, double c) {
        List<Object> l = new ArrayList<>(3);
        l.add(a); l.add(b); l.add(c);
        return l;
    }

    private static String readAll(InputStream is) throws Exception {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    /** Mirrors RetargetConfig.defaults() for standalone use. */
    private static Map<String, String> defaultMap() {
        Map<String, String> m = new LinkedHashMap<>();
        // "Root" intentionally unmapped (its raw orientation tips the model).
        m.put("Torso", "low_body"); m.put("Chest", "body");
        m.put("Head", "head");
        m.put("Shoulder_R", "right_arm"); m.put("Arm_R", "right_arm");
        m.put("Elbow_R", "right_arm"); m.put("Hand_R", "right_arm");
        m.put("Shoulder_L", "left_arm"); m.put("Arm_L", "left_arm");
        m.put("Elbow_L", "left_arm"); m.put("Hand_L", "left_arm");
        m.put("Thigh_R", "right_leg"); m.put("Leg_R", "right_leg"); m.put("Knee_R", "right_leg");
        m.put("Thigh_L", "left_leg"); m.put("Leg_L", "left_leg"); m.put("Knee_L", "left_leg");
        return m;
    }
}
