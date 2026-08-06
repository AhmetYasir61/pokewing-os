package com.pokewing.efmocap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Drives playback of recordings on clone actors. Phase 1: one clone per replay,
 * advanced one frame per client tick. Multiple replays can run at once (the
 * seed of Phase 2's layered scenes).
 */
public final class ReplayDirector {
    public static final ReplayDirector INSTANCE = new ReplayDirector();

    private static final class Replay {
        final CloneActor actor;
        final MocapRecording rec;
        int tick;
        boolean loop;
        /** Take is over but the body stayed behind; keep holding the corpse. */
        boolean lingering;
        Replay(CloneActor actor, MocapRecording rec, boolean loop) {
            this.actor = actor; this.rec = rec; this.loop = loop;
        }
    }

    private final List<Replay> replays = new ArrayList<>();

    private ReplayDirector() {}

    public int activeCount() { return replays.size(); }

    /**
     * Stage every saved take at once, all starting from frame 0, so separately
     * recorded performances play as one choreographed scene.
     */
    public int playScene(boolean loop) {
        clearAll();
        int n = 0;
        for (MocapRecording rec : TakeLibrary.INSTANCE.all()) {
            if (play(rec, loop)) n++;
        }
        return n;
    }

    /** Restart every active replay at frame 0 (re-syncs the scene). */
    public void restart() {
        for (Replay r : replays) {
            r.tick = 0;
            r.lingering = false;
            r.actor.revive();
            r.actor.resetInterpolation();
            r.actor.apply(r.rec.frameAt(0));
        }
    }

    /** Spawn a clone and start replaying the recording on it. */
    public boolean play(MocapRecording rec, boolean loop) {
        if (rec == null || rec.isEmpty()) return false;
        MocapFrame f0 = rec.frames.get(0);
        Character cast = CharacterLibrary.INSTANCE.get(rec.character);
        CloneActor actor = CloneActor.spawn(rec.name, cast, f0.x, f0.y, f0.z);
        if (actor == null) return false;
        actor.apply(f0);
        replays.add(new Replay(actor, rec, loop));
        return true;
    }

    /** Advance every active replay by one frame. Call once per client tick. */
    public void tick() {
        for (Iterator<Replay> it = replays.iterator(); it.hasNext(); ) {
            Replay r = it.next();

            // A body left behind after its take ended: keep holding the pose.
            if (r.lingering) {
                r.actor.apply(r.rec.frameAt(r.rec.length() - 1));
                continue;
            }

            r.tick++;
            if (r.tick >= r.rec.length()) {
                if (r.loop) {
                    r.tick = 0;
                    // A new run of the scene: the fallen get back up.
                    r.actor.revive();
                    // Snap instead of sliding all the way back from the end.
                    r.actor.resetInterpolation();
                } else if (r.actor.isDead()) {
                    // Don't delete the body -- the death should stay on camera.
                    r.lingering = true;
                    continue;
                } else {
                    r.actor.despawn();
                    it.remove();
                    continue;
                }
            }

            if (r.rec.deathTick >= 0 && r.tick >= r.rec.deathTick) {
                r.actor.die();
            }
            r.actor.apply(r.rec.frameAt(r.tick));
        }
    }

    /** Current playback position of a take, or -1 if it isn't staged. */
    public int currentTickOf(MocapRecording rec) {
        for (Replay r : replays) {
            if (r.rec == rec) return r.tick;
        }
        return -1;
    }

    public void clearAll() {
        for (Replay r : replays) r.actor.despawn();
        replays.clear();
    }
}
