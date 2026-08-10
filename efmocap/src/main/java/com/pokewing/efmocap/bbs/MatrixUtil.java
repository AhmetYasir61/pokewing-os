package com.pokewing.efmocap.bbs;

/**
 * Decomposes the 4x4 row-major transform matrices found in Epic Fight's
 * bundled animation JSON files ({@code assets/epicfight/animmodels/animations/...})
 * into translation + rotation quaternion. Layout (row-major):
 *
 * <pre>
 *   [ r00 r01 r02 tx ]   indices  0  1  2  3
 *   [ r10 r11 r12 ty ]            4  5  6  7
 *   [ r20 r21 r22 tz ]            8  9 10 11
 *   [  0   0   0   1 ]           12 13 14 15
 * </pre>
 */
public final class MatrixUtil {
    private MatrixUtil() {}

    /** @return float[7] = {tx,ty,tz, qx,qy,qz,qw}. */
    public static float[] decompose(double[] m) {
        float tx = (float) m[3], ty = (float) m[7], tz = (float) m[11];

        double r00 = m[0],  r01 = m[1],  r02 = m[2];
        double r10 = m[4],  r11 = m[5],  r12 = m[6];
        double r20 = m[8],  r21 = m[9],  r22 = m[10];

        double qx, qy, qz, qw;
        double trace = r00 + r11 + r22;
        if (trace > 0) {
            double s = Math.sqrt(trace + 1.0) * 2.0;
            qw = 0.25 * s;
            qx = (r21 - r12) / s;
            qy = (r02 - r20) / s;
            qz = (r10 - r01) / s;
        } else if (r00 > r11 && r00 > r22) {
            double s = Math.sqrt(1.0 + r00 - r11 - r22) * 2.0;
            qw = (r21 - r12) / s;
            qx = 0.25 * s;
            qy = (r01 + r10) / s;
            qz = (r02 + r20) / s;
        } else if (r11 > r22) {
            double s = Math.sqrt(1.0 + r11 - r00 - r22) * 2.0;
            qw = (r02 - r20) / s;
            qx = (r01 + r10) / s;
            qy = 0.25 * s;
            qz = (r12 + r21) / s;
        } else {
            double s = Math.sqrt(1.0 + r22 - r00 - r11) * 2.0;
            qw = (r10 - r01) / s;
            qx = (r02 + r20) / s;
            qy = (r12 + r21) / s;
            qz = 0.25 * s;
        }
        return new float[] {tx, ty, tz, (float) qx, (float) qy, (float) qz, (float) qw};
    }
}
