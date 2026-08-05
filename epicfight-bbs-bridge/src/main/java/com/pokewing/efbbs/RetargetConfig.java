package com.pokewing.efbbs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Retargeting configuration: how Epic Fight joints map onto the bone names of
 * the rigged Blockbench/glTF model you drive in BBS, plus coordinate tweaks.
 *
 * <p>Stored at {@code config/efbbs/retarget.json}. On first run a default biped
 * map is written matching Epic Fight's built-in biped armature joint names. Edit
 * that file to match your own actor model's bone names (a static .obj model has
 * no bones and cannot be animated -- use a rigged .bbmodel/glTF actor).</p>
 */
public class RetargetConfig {
    /** Epic Fight joint name -> your model's bone name. */
    public Map<String, String> jointToBone = new LinkedHashMap<>();

    /** Multiply all translation values by this (Epic Fight units -> model units). */
    public float translationScale = 16.0f; // meters-ish -> Blockbench pixels

    /** Per-axis rotation sign flips, applied after quaternion->euler. */
    public float rotXSign = 1.0f;
    public float rotYSign = 1.0f;
    public float rotZSign = 1.0f;

    /** Skip exporting joint translation (keep only rotation). Often cleaner for humanoids. */
    public boolean rotationOnly = false;

    /** Decimal places to keep in the output JSON. */
    public int decimals = 4;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve("efbbs").resolve("retarget.json");
    }

    public static RetargetConfig loadOrCreate() {
        Path path = configPath();
        try {
            if (Files.exists(path)) {
                try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    RetargetConfig cfg = GSON.fromJson(r, RetargetConfig.class);
                    if (cfg != null && cfg.jointToBone != null && !cfg.jointToBone.isEmpty()) {
                        return cfg;
                    }
                }
            }
        } catch (Exception e) {
            EpicFightBBSBridge.LOG.warn("[efbbs] Failed to read retarget.json, using defaults", e);
        }
        RetargetConfig cfg = defaults();
        cfg.save();
        return cfg;
    }

    /** Load from an arbitrary path (offline use, no Forge runtime needed). */
    public static RetargetConfig fromFileOrDefaults(Path path) {
        try {
            if (path != null && Files.exists(path)) {
                try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    RetargetConfig cfg = GSON.fromJson(r, RetargetConfig.class);
                    if (cfg != null && cfg.jointToBone != null && !cfg.jointToBone.isEmpty()) {
                        return cfg;
                    }
                }
            }
        } catch (Exception ignored) {}
        return defaults();
    }

    public void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
        } catch (IOException e) {
            EpicFightBBSBridge.LOG.warn("[efbbs] Failed to write retarget.json", e);
        }
    }

    /** Default map for Epic Fight's biped armature -> generic Blockbench player bones. */
    public static RetargetConfig defaults() {
        RetargetConfig c = new RetargetConfig();
        Map<String, String> m = c.jointToBone;
        // Epic Fight biped armature joints (verified from epicfight jar) on the
        // left -> common Blockbench player-bone names on the right. Edit the
        // right-hand side to match YOUR actor model's bone names.
        m.put("Root", "root");
        m.put("Torso", "body");
        m.put("Chest", "body");
        m.put("Head", "head");
        m.put("Shoulder_R", "right_arm");
        m.put("Arm_R", "right_arm");
        m.put("Elbow_R", "right_arm");
        m.put("Hand_R", "right_arm");
        m.put("Shoulder_L", "left_arm");
        m.put("Arm_L", "left_arm");
        m.put("Elbow_L", "left_arm");
        m.put("Hand_L", "left_arm");
        m.put("Thigh_R", "right_leg");
        m.put("Leg_R", "right_leg");
        m.put("Knee_R", "right_leg");
        m.put("Thigh_L", "left_leg");
        m.put("Leg_L", "left_leg");
        m.put("Knee_L", "left_leg");
        // Tool_R / Tool_L are weapon anchors -- usually left unmapped.
        return c;
    }
}
