package com.pokewing.pokeface.face;

/**
 * The single data structure every face source produces and the renderer consumes.
 *
 * <p>Values are normalised blendshape weights in [0,1] (or [-1,1] for the head
 * pose channels). Keeping every source behind this one struct is what lets the
 * webcam tracker, the combat reaction driver and the procedural idle animator be
 * swapped at runtime without the renderer knowing which one is currently live.
 */
public final class FaceState {

    /** 0 = eye fully open, 1 = fully closed. */
    public float blinkLeft;
    public float blinkRight;
    /** -1 = frowning/angry brow, +1 = raised/surprised brow. */
    public float brow;
    /** 0 = closed mouth, 1 = wide open. */
    public float mouthOpen;
    /** -1 = sad corners down, +1 = smiling. */
    public float mouthSmile;
    /** Head yaw/pitch offsets in [-1,1], used for the eye gaze offset. */
    public float gazeX;
    public float gazeY;
    /** Overall emotional intensity, drives the tint/blush overlay. */
    public float intensity;
    /** Which expression tile of the atlas the renderer should start from. */
    public Expression expression = Expression.NEUTRAL;

    public FaceState() {
    }

    public FaceState copy() {
        FaceState c = new FaceState();
        c.set(this);
        return c;
    }

    public void set(FaceState o) {
        this.blinkLeft = o.blinkLeft;
        this.blinkRight = o.blinkRight;
        this.brow = o.brow;
        this.mouthOpen = o.mouthOpen;
        this.mouthSmile = o.mouthSmile;
        this.gazeX = o.gazeX;
        this.gazeY = o.gazeY;
        this.intensity = o.intensity;
        this.expression = o.expression;
    }

    /** Frame-rate independent approach of {@code target}; {@code alpha} in [0,1]. */
    public void approach(FaceState target, float alpha) {
        this.blinkLeft = lerp(this.blinkLeft, target.blinkLeft, alpha);
        this.blinkRight = lerp(this.blinkRight, target.blinkRight, alpha);
        this.brow = lerp(this.brow, target.brow, alpha);
        this.mouthOpen = lerp(this.mouthOpen, target.mouthOpen, alpha);
        this.mouthSmile = lerp(this.mouthSmile, target.mouthSmile, alpha);
        this.gazeX = lerp(this.gazeX, target.gazeX, alpha);
        this.gazeY = lerp(this.gazeY, target.gazeY, alpha);
        this.intensity = lerp(this.intensity, target.intensity, alpha);
        // The tile itself is discrete: snap once the blend is past the halfway
        // point so a expression change never renders a half-morphed tile.
        if (alpha >= 1.0F || this.expression != target.expression && alpha > 0.5F) {
            this.expression = target.expression;
        }
    }

    private static float lerp(float from, float to, float alpha) {
        return from + (to - from) * alpha;
    }

    public static float clamp(float v, float min, float max) {
        return v < min ? min : Math.min(v, max);
    }

    /** Packs the state into 8 bytes for the network sync packet. */
    public long pack() {
        long v = 0L;
        v |= (long) quantize(blinkLeft, 0, 1) << 56;
        v |= (long) quantize(blinkRight, 0, 1) << 48;
        v |= (long) quantize(brow, -1, 1) << 40;
        v |= (long) quantize(mouthOpen, 0, 1) << 32;
        v |= (long) quantize(mouthSmile, -1, 1) << 24;
        v |= (long) quantize(gazeX, -1, 1) << 16;
        v |= (long) quantize(gazeY, -1, 1) << 8;
        v |= quantize(intensity, 0, 1);
        return v;
    }

    public static FaceState unpack(long v, Expression expression) {
        FaceState s = new FaceState();
        s.blinkLeft = dequantize((int) (v >>> 56 & 0xFF), 0, 1);
        s.blinkRight = dequantize((int) (v >>> 48 & 0xFF), 0, 1);
        s.brow = dequantize((int) (v >>> 40 & 0xFF), -1, 1);
        s.mouthOpen = dequantize((int) (v >>> 32 & 0xFF), 0, 1);
        s.mouthSmile = dequantize((int) (v >>> 24 & 0xFF), -1, 1);
        s.gazeX = dequantize((int) (v >>> 16 & 0xFF), -1, 1);
        s.gazeY = dequantize((int) (v >>> 8 & 0xFF), -1, 1);
        s.intensity = dequantize((int) (v & 0xFF), 0, 1);
        s.expression = expression;
        return s;
    }

    private static int quantize(float value, float min, float max) {
        float n = (clamp(value, min, max) - min) / (max - min);
        return Math.round(n * 255.0F) & 0xFF;
    }

    private static float dequantize(int raw, float min, float max) {
        return min + (raw / 255.0F) * (max - min);
    }
}
