package com.pokewing.efbbs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts an extracted Epic Fight animation into Blockbench (Bedrock) animation
 * JSON and writes it to {@code config/efbbs/exported/<name>.animation.json}.
 *
 * <p>Point BBS mod at that file (or copy it next to your actor's .bbmodel) and
 * select the animation on the actor -- no keyframing required.</p>
 */
public final class AnimationExporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private AnimationExporter() {}

    public static Path outputDir() {
        return FMLPaths.CONFIGDIR.get().resolve("efbbs").resolve("exported");
    }

    /** In-game path: export a live Epic Fight animation by registry key. */
    public static Path export(ResourceLocation key, RetargetConfig cfg) throws Exception {
        return writeExtracted(EpicFightAccess.extract(key), cfg, outputDir());
    }

    /** Write an already-extracted animation into {@code dir} as bedrock JSON. */
    public static Path writeExtracted(EpicFightAccess.Extracted ex, RetargetConfig cfg, Path dir)
            throws Exception {
        JsonObject root = buildBlockbench(ex, cfg);
        String safe = ex.name.replaceAll("[^a-zA-Z0-9._-]", "_");
        Files.createDirectories(dir);
        Path file = dir.resolve(safe + ".animation.json");
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(root, w);
        }
        return file;
    }

    private static JsonObject buildBlockbench(EpicFightAccess.Extracted ex, RetargetConfig cfg) {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.8.0");

        JsonObject animations = new JsonObject();
        JsonObject anim = new JsonObject();
        anim.addProperty("loop", false);
        anim.addProperty("animation_length", MathUtil.round(ex.length, cfg.decimals));

        JsonObject bones = new JsonObject();

        // Collapse Epic Fight joints onto target bones. When several joints map
        // to one bone, keep the joint that carries the most keyframes.
        Map<String, List<EpicFightAccess.Frame>> chosen = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, List<EpicFightAccess.Frame>> e : ex.joints.entrySet()) {
            String bone = cfg.jointToBone.get(e.getKey());
            if (bone == null) continue; // unmapped joint -> skipped
            List<EpicFightAccess.Frame> existing = chosen.get(bone);
            if (existing == null || e.getValue().size() > existing.size()) {
                chosen.put(bone, e.getValue());
            }
        }

        for (Map.Entry<String, List<EpicFightAccess.Frame>> e : chosen.entrySet()) {
            JsonObject boneObj = new JsonObject();
            JsonObject rotation = new JsonObject();
            JsonObject position = cfg.rotationOnly ? null : new JsonObject();

            for (EpicFightAccess.Frame f : e.getValue()) {
                String t = fmtTime(f.time);

                float[] euler = MathUtil.quaternionToEulerXYZDegrees(f.qx, f.qy, f.qz, f.qw);
                rotation.add(t, arr(
                        MathUtil.round(euler[0] * cfg.rotXSign, cfg.decimals),
                        MathUtil.round(euler[1] * cfg.rotYSign, cfg.decimals),
                        MathUtil.round(euler[2] * cfg.rotZSign, cfg.decimals)));

                if (position != null) {
                    position.add(t, arr(
                            MathUtil.round(f.tx * cfg.translationScale, cfg.decimals),
                            MathUtil.round(f.ty * cfg.translationScale, cfg.decimals),
                            MathUtil.round(f.tz * cfg.translationScale, cfg.decimals)));
                }
            }

            boneObj.add("rotation", rotation);
            if (position != null) boneObj.add("position", position);
            bones.add(e.getKey(), boneObj);
        }

        // Root motion: bake recorded world position + body yaw onto the bone
        // mapped from the "Root" joint (falls back to a dedicated "root" bone).
        if (!ex.rootMotion.isEmpty()) {
            String rootBone = cfg.rootBone != null ? cfg.rootBone
                    : cfg.jointToBone.getOrDefault("Root", "anchor");
            JsonObject boneObj = bones.has(rootBone)
                    ? bones.getAsJsonObject(rootBone) : new JsonObject();
            JsonObject position = new JsonObject();
            JsonObject rotation = boneObj.has("rotation")
                    ? boneObj.getAsJsonObject("rotation") : new JsonObject();
            for (EpicFightAccess.RootFrame rf : ex.rootMotion) {
                String t = fmtTime(rf.time);
                // World blocks -> model units. BBS/Blockbench: +Y up, so map
                // world (x,y,z) -> (x, y, z) * scale; tune signs via config.
                position.add(t, arr(
                        MathUtil.round(rf.x * cfg.translationScale * cfg.rootXSign, cfg.decimals),
                        MathUtil.round(rf.y * cfg.translationScale, cfg.decimals),
                        MathUtil.round(rf.z * cfg.translationScale * cfg.rootZSign, cfg.decimals)));
                if (cfg.recordRootYaw) {
                    rotation.add(t, arr(0f, MathUtil.round(rf.yawDeg * cfg.rotYSign, cfg.decimals), 0f));
                }
            }
            boneObj.add("position", position);
            if (cfg.recordRootYaw) boneObj.add("rotation", rotation);
            bones.add(rootBone, boneObj);
        }

        anim.add("bones", bones);
        animations.add("efbbs." + ex.name.replaceAll("[^a-zA-Z0-9._]", "_"), anim);
        root.add("animations", animations);
        return root;
    }

    private static JsonArray arr(float a, float b, float c) {
        JsonArray j = new JsonArray();
        j.add(a); j.add(b); j.add(c);
        return j;
    }

    private static String fmtTime(float t) {
        // Blockbench keys times as decimal-second strings.
        return String.format(Locale.ROOT, "%.4f", t);
    }
}
