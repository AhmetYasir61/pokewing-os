package com.pokewing.efmocap;

/**
 * A model pinned to an Epic Fight bone — a tail, ears, a cape, a weapon on the
 * back, whatever you model. The mesh is a Wavefront {@code .obj} dropped into
 * {@code config/efmocap/attachments}; anything in that folder shows up in the
 * character editor.
 *
 * <p>{@link #bone} is an Epic Fight joint name (Head, Chest, Torso, Arm_R …),
 * and the offsets/rotation place the model relative to that bone.</p>
 */
public class Attachment {
    /** OBJ filename under config/efmocap/attachments ("" = nothing to draw). */
    public String model = "";
    /** Optional PNG in the same folder; empty falls back to the character skin. */
    public String texture = "";
    /** Epic Fight joint this rides on. */
    public String bone = "Head";

    /** Offset from the bone, in pixels (1/16 block). */
    public float offsetX = 0, offsetY = 0, offsetZ = 0;
    /** Rotation in degrees. */
    public float rotX = 0, rotY = 0, rotZ = 0;
    public float scale = 1.0f;

    public Attachment() {}

    public Attachment(String model, String bone) {
        this.model = model;
        this.bone = bone;
    }

    public String describe() {
        String m = model.isEmpty() ? "(model yok)" : model.replaceAll("(?i)\\.obj$", "");
        return m + " → " + bone;
    }
}
