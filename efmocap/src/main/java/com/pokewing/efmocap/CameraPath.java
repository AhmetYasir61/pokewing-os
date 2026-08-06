package com.pokewing.efmocap;

import java.util.ArrayList;
import java.util.List;

/**
 * A cinematic camera path. Positions and angles are interpolated with
 * Catmull-Rom splines so the camera glides continuously through the keyframes
 * instead of stopping at each one. Yaw values are unwrapped first, so a path
 * crossing 359 deg -> 1 deg sweeps the short way instead of spinning around.
 */
public final class CameraPath {
    public final List<CameraKeyframe> keys = new ArrayList<>();

    /** Sampled camera state. */
    public static final class Sample {
        public double x, y, z;
        public float yaw, pitch, roll;
        public double fov;
    }

    public boolean isEmpty() { return keys.isEmpty(); }
    public int size() { return keys.size(); }
    public void clear() { keys.clear(); }
    public void add(CameraKeyframe k) { keys.add(k); }

    /** Total path length in ticks. */
    public int durationTicks() {
        int t = 0;
        for (int i = 1; i < keys.size(); i++) t += Math.max(1, keys.get(i).travelTicks);
        return t;
    }

    /** Cumulative start time of each keyframe. */
    private int[] times() {
        int[] t = new int[keys.size()];
        for (int i = 1; i < keys.size(); i++) {
            t[i] = t[i - 1] + Math.max(1, keys.get(i).travelTicks);
        }
        return t;
    }

    /** Yaw sequence unwrapped so successive values never jump more than 180 deg. */
    private float[] unwrappedYaws() {
        float[] y = new float[keys.size()];
        if (keys.isEmpty()) return y;
        y[0] = keys.get(0).yaw;
        for (int i = 1; i < keys.size(); i++) {
            float prev = y[i - 1];
            float cur = keys.get(i).yaw;
            float d = cur - prev;
            while (d > 180f) { cur -= 360f; d = cur - prev; }
            while (d < -180f) { cur += 360f; d = cur - prev; }
            y[i] = cur;
        }
        return y;
    }

    /**
     * Sample the path at {@code time} ticks (fractional for sub-tick smoothness).
     * Returns null when the path has no keyframes.
     */
    public Sample sample(float time) {
        if (keys.isEmpty()) return null;
        Sample s = new Sample();
        if (keys.size() == 1) {
            CameraKeyframe k = keys.get(0);
            s.x = k.x; s.y = k.y; s.z = k.z;
            s.yaw = k.yaw; s.pitch = k.pitch; s.roll = k.roll; s.fov = k.fov;
            return s;
        }

        int[] t = times();
        int total = t[t.length - 1];
        float clamped = Math.max(0f, Math.min(time, total));

        int i = 0;
        while (i < keys.size() - 2 && clamped > t[i + 1]) i++;

        float span = Math.max(1, t[i + 1] - t[i]);
        float u = (clamped - t[i]) / span;
        u = Math.max(0f, Math.min(1f, u));

        float[] yaws = unwrappedYaws();
        int i0 = Math.max(0, i - 1), i1 = i, i2 = i + 1, i3 = Math.min(keys.size() - 1, i + 2);
        CameraKeyframe k0 = keys.get(i0), k1 = keys.get(i1), k2 = keys.get(i2), k3 = keys.get(i3);

        s.x = catmull(k0.x, k1.x, k2.x, k3.x, u);
        s.y = catmull(k0.y, k1.y, k2.y, k3.y, u);
        s.z = catmull(k0.z, k1.z, k2.z, k3.z, u);
        s.yaw = (float) catmull(yaws[i0], yaws[i1], yaws[i2], yaws[i3], u);
        s.pitch = (float) catmull(k0.pitch, k1.pitch, k2.pitch, k3.pitch, u);
        s.roll = (float) catmull(k0.roll, k1.roll, k2.roll, k3.roll, u);
        s.fov = catmull(k0.fov, k1.fov, k2.fov, k3.fov, u);
        return s;
    }

    /** Uniform Catmull-Rom; passes through p1 at u=0 and p2 at u=1. */
    private static double catmull(double p0, double p1, double p2, double p3, double u) {
        double u2 = u * u, u3 = u2 * u;
        return 0.5 * ((2 * p1)
                + (-p0 + p2) * u
                + (2 * p0 - 5 * p1 + 4 * p2 - p3) * u2
                + (-p0 + 3 * p1 - 3 * p2 + p3) * u3);
    }
}
