package com.pokewing.efmocap.bbs;

/**
 * Small math helpers for converting Epic Fight transforms into the Euler-degree
 * form that Blockbench / Bedrock animation JSON expects.
 *
 * <p>Coordinate-system caveat: Epic Fight builds its poses with its own
 * {@code OpenMatrix4f} (rotations as quaternions), while Blockbench stores
 * per-bone rotation as XYZ Euler angles in degrees. The raw conversion below is
 * a straight quaternion -> XYZ-Euler decomposition. Because the two rigs may
 * differ in axis orientation and rest pose, {@link RetargetConfig} exposes
 * per-axis sign flips and a global scale so the result can be tuned without
 * recompiling.</p>
 */
public final class MathUtil {
    private MathUtil() {}

    /** Quaternion (x,y,z,w) -> XYZ Euler angles in DEGREES, order X then Y then Z. */
    public static float[] quaternionToEulerXYZDegrees(float x, float y, float z, float w) {
        // Normalise to be safe.
        float n = (float) Math.sqrt(x * x + y * y + z * z + w * w);
        if (n > 1.0e-6f) {
            x /= n; y /= n; z /= n; w /= n;
        }

        // Roll (X)
        float sinrCosp = 2.0f * (w * x + y * z);
        float cosrCosp = 1.0f - 2.0f * (x * x + y * y);
        float roll = (float) Math.atan2(sinrCosp, cosrCosp);

        // Pitch (Y) with gimbal clamp
        float sinp = 2.0f * (w * y - z * x);
        float pitch;
        if (Math.abs(sinp) >= 1.0f) {
            pitch = (float) Math.copySign(Math.PI / 2.0, sinp);
        } else {
            pitch = (float) Math.asin(sinp);
        }

        // Yaw (Z)
        float sinyCosp = 2.0f * (w * z + x * y);
        float cosyCosp = 1.0f - 2.0f * (y * y + z * z);
        float yaw = (float) Math.atan2(sinyCosp, cosyCosp);

        return new float[] {
                (float) Math.toDegrees(roll),
                (float) Math.toDegrees(pitch),
                (float) Math.toDegrees(yaw)
        };
    }

    public static float round(float v, int decimals) {
        double f = Math.pow(10, decimals);
        return (float) (Math.round(v * f) / f);
    }
}
