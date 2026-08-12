package com.pokewing.pokeface.face;

/** A timed expression override pushed by gameplay events or the reaction wheel. */
public final class ReactionTrigger {

    public final Expression expression;
    public final float intensity;
    public final int priority;
    private int ticksLeft;

    public ReactionTrigger(Expression expression, float intensity, int durationTicks, int priority) {
        this.expression = expression;
        this.intensity = FaceState.clamp(intensity, 0.0F, 1.0F);
        this.ticksLeft = durationTicks;
        this.priority = priority;
    }

    public boolean tick() {
        return --this.ticksLeft > 0;
    }

    public boolean expired() {
        return this.ticksLeft <= 0;
    }

    public FaceState pose() {
        return this.expression.pose(this.intensity);
    }
}
