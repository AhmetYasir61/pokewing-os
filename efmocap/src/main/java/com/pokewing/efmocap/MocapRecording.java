package com.pokewing.efmocap;

import java.util.ArrayList;
import java.util.List;

/** An ordered list of {@link MocapFrame}s captured at 20 tps, plus a name. */
public final class MocapRecording {
    public String name;
    /** Character this take was performed as (empty = default look). */
    public String character = "";
    /**
     * Tick at which this actor dies, or -1 if it survives. At that point the
     * clone plays the death animation and stays on the ground as a corpse
     * instead of being removed, so the death reads on camera every take.
     */
    public int deathTick = -1;
    /**
     * Ticks to wait before this actor enters. Lets a take recorded on its own
     * be slotted into the scene later — one fighter arrives after another —
     * without re-performing it.
     */
    public int startOffset = 0;
    public final List<MocapFrame> frames = new ArrayList<>();

    public MocapRecording(String name) { this.name = name; }

    public int length() { return frames.size(); }
    public boolean isEmpty() { return frames.isEmpty(); }

    public MocapFrame frameAt(int tick) {
        if (frames.isEmpty()) return null;
        int i = Math.max(0, Math.min(tick, frames.size() - 1));
        return frames.get(i);
    }
}
