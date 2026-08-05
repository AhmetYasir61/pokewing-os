package com.pokewing.efmocap;

/**
 * One recorded tick of a performer: world transform + the Epic Fight animation
 * being played. Deliberately tiny -- action-level capture (an animation id, not
 * a full pose) keeps recordings small and replays at native fidelity.
 */
public final class MocapFrame {
    public double x, y, z;      // world position
    public float yRot;          // head yaw
    public float yBodyRot;      // body yaw
    public float xRot;          // pitch
    public int animId = -1;     // Epic Fight animation id playing this tick (-1 = none)
    public float elapsed;       // elapsed time within that animation

    public MocapFrame() {}

    public MocapFrame(double x, double y, double z, float yRot, float yBodyRot, float xRot,
                      int animId, float elapsed) {
        this.x = x; this.y = y; this.z = z;
        this.yRot = yRot; this.yBodyRot = yBodyRot; this.xRot = xRot;
        this.animId = animId; this.elapsed = elapsed;
    }
}
