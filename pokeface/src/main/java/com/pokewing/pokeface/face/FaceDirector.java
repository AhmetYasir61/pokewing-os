package com.pokewing.pokeface.face;

import com.pokewing.pokeface.PokeFaceConfig;
import com.pokewing.pokeface.tracker.TrackerSource;

import java.util.random.RandomGenerator;

/**
 * Resolves which face source wins each tick and blends the result.
 *
 * <p>Priority, highest first:
 * <ol>
 *   <li>Live webcam tracker (when enabled, bound and receiving packets)</li>
 *   <li>The strongest unexpired {@link ReactionTrigger} — combat reactions and
 *       the manual reaction wheel both land here</li>
 *   <li>Procedural idle, biased by the player's current situation</li>
 * </ol>
 * Falling through the list is the documented behaviour when no camera is found
 * or the player never picked one in the settings screen.
 */
public final class FaceDirector {

    private final ProceduralIdle idle;
    private final FaceState current = new FaceState();
    private final FaceState scratch = new FaceState();

    private TrackerSource tracker;
    private ReactionTrigger active;
    private Expression moodBias = Expression.NEUTRAL;
    private Source lastSource = Source.IDLE;

    public enum Source {
        TRACKER, REACTION, IDLE
    }

    public FaceDirector(RandomGenerator random) {
        this.idle = new ProceduralIdle(random);
    }

    public void setTracker(TrackerSource tracker) {
        this.tracker = tracker;
    }

    public void setMoodBias(Expression moodBias) {
        this.moodBias = moodBias;
    }

    /** Pushes a reaction; a weaker one never interrupts a stronger live reaction. */
    public void push(ReactionTrigger trigger) {
        if (this.active == null || this.active.expired() || trigger.priority >= this.active.priority) {
            this.active = trigger;
        }
    }

    public Source lastSource() {
        return this.lastSource;
    }

    public boolean trackerLive() {
        return this.tracker != null && this.tracker.isLive();
    }

    public FaceState tick() {
        FaceState target;
        if (this.tracker != null && this.tracker.poll(this.scratch)) {
            target = this.scratch;
            this.lastSource = Source.TRACKER;
        } else if (this.active != null && this.active.tick()) {
            target = this.active.pose();
            this.lastSource = Source.REACTION;
        } else {
            this.active = null;
            target = this.idle.tick(this.moodBias);
            this.lastSource = Source.IDLE;
        }
        this.current.approach(target, (float) PokeFaceConfig.smoothing());
        return this.current;
    }

    public FaceState current() {
        return this.current;
    }
}
