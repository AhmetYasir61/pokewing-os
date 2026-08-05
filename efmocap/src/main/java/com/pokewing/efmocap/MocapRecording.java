package com.pokewing.efmocap;

import java.util.ArrayList;
import java.util.List;

/** An ordered list of {@link MocapFrame}s captured at 20 tps, plus a name. */
public final class MocapRecording {
    public String name;
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
