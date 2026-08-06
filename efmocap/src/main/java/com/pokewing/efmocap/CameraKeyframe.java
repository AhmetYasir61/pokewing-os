package com.pokewing.efmocap;

/** One point on a cinematic camera path. */
public final class CameraKeyframe {
    public double x, y, z;
    public float yaw, pitch, roll;
    public double fov;
    /** Ticks spent travelling from the previous keyframe to this one. */
    public int travelTicks = 40;

    public CameraKeyframe() {}

    public CameraKeyframe(double x, double y, double z, float yaw, float pitch,
                          float roll, double fov, int travelTicks) {
        this.x = x; this.y = y; this.z = z;
        this.yaw = yaw; this.pitch = pitch; this.roll = roll;
        this.fov = fov; this.travelTicks = travelTicks;
    }
}
