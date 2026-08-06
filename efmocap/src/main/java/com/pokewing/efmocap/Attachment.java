package com.pokewing.efmocap;

/**
 * A cosmetic part bolted onto an Epic Fight bone — a tail, ears, horns. Kept
 * parametric rather than a full model format so characters can be built from the
 * editor without an external modelling tool.
 *
 * <p>{@link #bone} is an Epic Fight joint name (Head, Chest, Torso, Arm_R …).</p>
 */
public class Attachment {
    /** tail | ears | horns | box */
    public String preset = "tail";
    public String bone = "Torso";

    /** Offset from the bone, in pixels (1/16 block). */
    public float offsetX = 0, offsetY = 0, offsetZ = 0;
    /** Extra rotation in degrees. */
    public float rotX = 0, rotY = 0, rotZ = 0;

    public float scale = 1.0f;

    /** Tail-like chains: how many segments, and how much each one droops. */
    public int segments = 5;
    public float segLength = 3.0f;
    public float thickness = 2.0f;
    public float droop = 10.0f;

    /** ARGB tint applied to the part. */
    public int color = 0xFFFFFFFF;
    /** Optional PNG under config/efmocap/attachments; empty = use the skin. */
    public String texture = "";

    public Attachment() {}

    public Attachment(String preset, String bone) {
        this.preset = preset;
        this.bone = bone;
        if ("ears".equals(preset) || "horns".equals(preset)) {
            this.segments = 1;
            this.segLength = 4f;
            this.thickness = 3f;
            this.droop = 0f;
        }
    }

    public String describe() {
        return preset + " @ " + bone;
    }
}
