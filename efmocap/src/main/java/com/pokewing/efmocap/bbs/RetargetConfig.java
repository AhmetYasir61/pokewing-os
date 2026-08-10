package com.pokewing.efmocap.bbs;

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

    /**
     * Skip per-joint translation (export rotation only for limbs). Default TRUE:
     * Epic Fight's joint translations are in its own bone space and, if written
     * as Blockbench position offsets, tear limbs off their sockets. Root motion
     * is unaffected -- it is written separately onto the root bone. Only set this
     * false if you specifically want raw joint translations.
     */
    public boolean rotationOnly = true;

    /** Decimal places to keep in the output JSON. */
    public int decimals = 4;

    // --- live recorder settings ---
    /** Radius (blocks) around the player within which entities are recorded. */
    public double recordRadius = 24.0;
    /** Bake body yaw onto the root bone during recording. */
    public boolean recordRootYaw = true;
    /** Name of the model's root bone that receives root motion (anchor for BBS
     *  native player rig, "root" for the bundled geo actor). */
    public String rootBone = "anchor";
    /** Root-motion axis sign flips (tune if the actor moves the wrong way). */
    public float rootXSign = 1.0f;
    public float rootZSign = 1.0f;

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
            com.pokewing.efmocap.EFMocap.LOG.warn("[efbbs] Failed to read retarget.json, using defaults", e);
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
            com.pokewing.efmocap.EFMocap.LOG.warn("[efbbs] Failed to write retarget.json", e);
        }
    }

    /** Default map for Epic Fight's biped armature -> generic Blockbench player bones. */
    public static RetargetConfig defaults() {
        RetargetConfig c = new RetargetConfig();
        Map<String, String> m = c.jointToBone;
        // Epic Fight biped armature joints (verified from the epicfight jar) on
        // the left -> BBS bone names on the right, read off BBS's own emoticons
        // rig (emoticons/steve/default.bobj). Edit the right side to match YOUR
        // model's bones.
        // NOTE: "Root" is intentionally NOT mapped here -- its raw Epic Fight
        // orientation would tip the whole model. The root bone (anchor) instead
        // receives only recorded root motion (position + body yaw).
        // Two-segment spine. Note the naming is BBS's, not anatomy's: `body`
        // is the waist and `low_body` the chest that carries the arms and head.
        m.put("Torso", "body");
        m.put("Chest", "low_body");
        m.put("Head", "head");
        // Limbs are two-segment, so elbows and knees get their own bone instead
        // of collapsing into the shoulder and hip. These are BBS's own emoticons
        // bone names (including the asymmetric `low_leg_right`), which EFMocap's
        // actor model copies, so one animation drives both. BBS's stock cubic
        // player rig has no lower segments and just ignores those channels.
        m.put("Shoulder_R", "right_arm");
        m.put("Arm_R", "right_arm");
        m.put("Elbow_R", "low_right_arm");
        m.put("Hand_R", "low_right_arm.end");
        m.put("Shoulder_L", "left_arm");
        m.put("Arm_L", "left_arm");
        m.put("Elbow_L", "low_left_arm");
        m.put("Hand_L", "low_left_arm.end");
        m.put("Thigh_R", "right_leg");
        m.put("Leg_R", "low_leg_right");
        m.put("Knee_R", "low_leg_right");
        m.put("Thigh_L", "left_leg");
        m.put("Leg_L", "low_left_leg");
        m.put("Knee_L", "low_left_leg");
        // Tool_R / Tool_L are weapon anchors -- usually left unmapped.
        return c;
    }
}
