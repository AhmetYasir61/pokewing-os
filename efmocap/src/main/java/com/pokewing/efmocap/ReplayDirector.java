package com.pokewing.efmocap;

import java.util.ArrayList;
import java.util.List;

/**
 * Plays recordings back on clone actors against a single scene clock. Every
 * staged take reads its frame from that one clock (offset by its own start
 * delay), so actors stay in sync, scrubbing lands every actor on exactly the
 * right frame, and a take can be made to enter late without re-recording it.
 */
public final class ReplayDirector {
    public static final ReplayDirector INSTANCE = new ReplayDirector();

    private static final class Replay {
        final CloneActor actor;
        final MocapRecording rec;
        Replay(CloneActor actor, MocapRecording rec) {
            this.actor = actor; this.rec = rec;
        }
    }

    private final List<Replay> replays = new ArrayList<>();
    private int clock;
    private boolean paused;
    private boolean loop = true;

    private ReplayDirector() {}

    public int activeCount() { return replays.size(); }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean p) { paused = p; }
    public int sceneTick() { return clock; }

    /** Scene length: the last frame any staged take reaches. */
    public int sceneLength() {
        int max = 0;
        for (Replay r : replays) {
            max = Math.max(max, r.rec.startOffset + r.rec.length());
        }
        return max;
    }

    /** Stage every saved take together as one scene. */
    public int playScene(boolean looping) {
        clearAll();
        loop = looping;
        int n = 0;
        for (MocapRecording rec : TakeLibrary.INSTANCE.all()) {
            if (stage(rec)) n++;
        }
        clock = 0;
        applyAll();
        return n;
    }

    /** Stage a single take on its own. */
    public boolean play(MocapRecording rec, boolean looping) {
        if (rec == null || rec.isEmpty()) return false;
        loop = looping;
        boolean ok = stage(rec);
        if (ok) { clock = 0; applyAll(); }
        return ok;
    }

    private boolean stage(MocapRecording rec) {
        if (rec == null || rec.isEmpty()) return false;
        MocapFrame f0 = rec.frames.get(0);
        Character cast = CharacterLibrary.INSTANCE.get(rec.character);
        CloneActor actor = CloneActor.spawn(rec.name, cast, f0.x, f0.y, f0.z);
        if (actor == null) return false;
        replays.add(new Replay(actor, rec));
        return true;
    }

    /** Restart the scene from the top. */
    public void restart() {
        clock = 0;
        for (Replay r : replays) { r.actor.revive(); r.actor.resetInterpolation(); }
        CarrySystem.INSTANCE.reset();
        applyAll();
    }

    /** Jump the whole scene to a tick — used to scrub the timeline. */
    public void seek(int tick) {
        int len = Math.max(1, sceneLength());
        clock = Math.max(0, Math.min(tick, len));
        for (Replay r : replays) r.actor.resetInterpolation();
        applyAll();
    }

    /** Advance the scene one tick. Call once per client tick. */
    public void tick() {
        if (replays.isEmpty()) return;
        if (!paused) {
            clock++;
            int len = sceneLength();
            if (clock >= len) {
                if (loop) {
                    clock = 0;
                    for (Replay r : replays) { r.actor.revive(); r.actor.resetInterpolation(); }
                    CarrySystem.INSTANCE.reset();
                } else {
                    clock = len;   // hold on the last frame; corpses stay put
                }
            }
        }
        applyAll();
    }

    /** Pose every actor for the current clock position. */
    private void applyAll() {
        for (Replay r : replays) {
            int local = clock - r.rec.startOffset;

            if (local < 0) {
                // Hasn't entered yet: hold the opening pose.
                r.actor.revive();
                r.actor.apply(r.rec.frameAt(0));
                continue;
            }
            if (r.rec.deathTick >= 0 && local >= r.rec.deathTick) {
                r.actor.applyCorpse(local - r.rec.deathTick);
                continue;
            }
            r.actor.revive();
            r.actor.apply(r.rec.frameAt(Math.min(local, r.rec.length() - 1)));
        }

        // Second pass: a performer who shouldered a body carries it here too,
        // once every actor has been placed for this tick.
        for (Replay r : replays) {
            int local = clock - r.rec.startOffset;
            if (local < 0 || local >= r.rec.length()) continue;
            MocapFrame f = r.rec.frameAt(local);
            if (f == null || f.carrying == null || f.carrying.isEmpty()) continue;
            carryTo(f.carrying, r.actor.x(), r.actor.y(), r.actor.z(), r.actor.yaw());
        }
    }

    /**
     * Pose the whole scene at an exact fractional tick. Offline rendering steps
     * this by 20/fps per output frame, so the film advances by the same amount
     * every frame no matter how long the frame took to write to disk.
     */
    public void renderAt(double tickPos) {
        clock = (int) Math.floor(tickPos);
        float frac = (float) (tickPos - clock);

        for (Replay r : replays) {
            double local = tickPos - r.rec.startOffset;
            if (local < 0) {
                r.actor.revive();
                r.actor.applyExact(r.rec.frameAt(0), r.rec.frameAt(0), 0f);
                continue;
            }
            if (r.rec.deathTick >= 0 && local >= r.rec.deathTick) {
                r.actor.applyCorpse((int) (local - r.rec.deathTick));
                continue;
            }
            int i = (int) Math.floor(local);
            int last = r.rec.length() - 1;
            r.actor.revive();
            r.actor.applyExact(r.rec.frameAt(Math.min(i, last)),
                    r.rec.frameAt(Math.min(i + 1, last)), frac);
        }

        for (Replay r : replays) {
            int local = (int) Math.floor(tickPos - r.rec.startOffset);
            if (local < 0 || local >= r.rec.length()) continue;
            MocapFrame f = r.rec.frameAt(local);
            if (f == null || f.carrying == null || f.carrying.isEmpty()) continue;
            carryTo(f.carrying, r.actor.x(), r.actor.y(), r.actor.z(), r.actor.yaw());
        }
    }

    public void clearAll() {
        for (Replay r : replays) r.actor.despawn();
        replays.clear();
        clock = 0;
        CarrySystem.INSTANCE.reset();
    }

    // --- corpses ---------------------------------------------------------

    /** Take name of the nearest corpse within {@code range}, or null. */
    public String nearestCorpseTake(double x, double y, double z, double range) {
        String best = null;
        double bestSq = range * range;
        for (Replay r : replays) {
            if (!r.actor.isDead() || r.actor.isCarried()) continue;
            double dx = r.actor.x() - x, dy = r.actor.y() - y, dz = r.actor.z() - z;
            double d = dx * dx + dy * dy + dz * dz;
            if (d <= bestSq) { bestSq = d; best = r.rec.name; }
        }
        return best;
    }

    public void setCarried(String takeName, boolean carried) {
        Replay r = find(takeName);
        if (r != null) r.actor.setCarried(carried);
    }

    /** Ride a corpse on a carrier's back. False if it's no longer a corpse. */
    public boolean carryTo(String takeName, double x, double y, double z, float yaw) {
        Replay r = find(takeName);
        if (r == null || !r.actor.isDead()) return false;
        r.actor.setCarried(true);
        r.actor.setCarriedPose(x, y, z, yaw);
        return true;
    }

    public void placeCorpse(String takeName, double x, double y, double z, float yaw) {
        Replay r = find(takeName);
        if (r != null) r.actor.placeAt(x, y, z, yaw);
    }

    private Replay find(String takeName) {
        for (Replay r : replays) {
            if (r.rec.name.equals(takeName)) return r;
        }
        return null;
    }

    /** Playback position within a take, or -1 if it isn't staged. */
    public int currentTickOf(MocapRecording rec) {
        for (Replay r : replays) {
            if (r.rec == rec) return Math.max(0, clock - rec.startOffset);
        }
        return -1;
    }
}
