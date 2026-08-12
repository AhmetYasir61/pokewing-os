package com.pokewing.pokeface.face;

import java.util.random.RandomGenerator;

/**
 * The "no camera" fallback: a small procedural animator that keeps the face
 * alive with randomised blinks, gaze saccades and slow expression drift so a
 * player without a tracker never renders as a frozen mask.
 */
public final class ProceduralIdle {

    private static final int BLINK_TICKS = 4;

    private final RandomGenerator random;
    private final FaceState target = new FaceState();

    private int blinkCountdown;
    private int blinkTicksLeft;
    private int saccadeCountdown;
    private int driftCountdown;

    public ProceduralIdle(RandomGenerator random) {
        this.random = random;
        this.blinkCountdown = nextBlinkDelay();
        this.saccadeCountdown = nextSaccadeDelay();
        this.driftCountdown = nextDriftDelay();
    }

    private int nextBlinkDelay() {
        return 40 + this.random.nextInt(120);   // ~2-8 s
    }

    private int nextSaccadeDelay() {
        return 20 + this.random.nextInt(80);
    }

    private int nextDriftDelay() {
        return 200 + this.random.nextInt(400);  // ~10-30 s
    }

    /**
     * @param moodBias expression the surroundings suggest (e.g. hurt/low health);
     *                 the idle animator drifts around it instead of pure random.
     */
    public FaceState tick(Expression moodBias) {
        if (--this.blinkCountdown <= 0) {
            this.blinkTicksLeft = BLINK_TICKS;
            this.blinkCountdown = nextBlinkDelay();
        }
        if (this.blinkTicksLeft > 0) {
            this.blinkTicksLeft--;
            float phase = this.blinkTicksLeft / (float) BLINK_TICKS;
            // Triangle profile: shut fast, open a touch slower.
            float closed = phase > 0.5F ? (1.0F - phase) * 2.0F : phase * 2.0F;
            this.target.blinkLeft = this.target.blinkRight = FaceState.clamp(closed, 0.0F, 1.0F);
        } else {
            this.target.blinkLeft = this.target.blinkRight = 0.0F;
        }

        if (--this.saccadeCountdown <= 0) {
            this.saccadeCountdown = nextSaccadeDelay();
            this.target.gazeX = (this.random.nextFloat() - 0.5F) * 1.2F;
            this.target.gazeY = (this.random.nextFloat() - 0.5F) * 0.8F;
        }

        if (--this.driftCountdown <= 0) {
            this.driftCountdown = nextDriftDelay();
            FaceState pose = moodBias.pose(0.35F + this.random.nextFloat() * 0.35F);
            this.target.brow = pose.brow;
            this.target.mouthOpen = pose.mouthOpen;
            this.target.mouthSmile = pose.mouthSmile;
            this.target.intensity = pose.intensity;
            this.target.expression = moodBias;
        }
        return this.target;
    }
}
