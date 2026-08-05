package com.pokewing.efbbs;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minecraft-agnostic recording buffer. {@link ClientRecording} feeds it live
 * samples each client tick; on stop it converts every recorded entity into a
 * BBS-ready Bedrock animation (via {@link AnimationExporter}).
 *
 * <p>Kept free of Minecraft imports so it is trivial to reason about and could
 * be unit-tested; all entity/world access lives in {@link ClientRecording}.</p>
 */
public final class PoseRecorder {
    public static final PoseRecorder INSTANCE = new PoseRecorder();

    private static final class EntityRec {
        final EpicFightAccess.Extracted ex = new EpicFightAccess.Extracted();
    }

    private final Map<String, EntityRec> recs = new LinkedHashMap<>();
    private volatile boolean recording = false;
    private int frameCount = 0;

    private PoseRecorder() {}

    public boolean isRecording() { return recording; }
    public int frameCount() { return frameCount; }
    public int entityCount() { return recs.size(); }

    public synchronized void start() {
        recs.clear();
        frameCount = 0;
        recording = true;
    }

    /**
     * Record one entity's sample for the current tick.
     *
     * @param uuid  stable per-entity id
     * @param name  human-readable name used for the output file
     * @param pose  joint-name -> {tx,ty,tz,qx,qy,qz,qw} in Epic Fight space
     * @param time  seconds since recording started
     * @param rx,ry,rz  world position relative to the recording origin (blocks)
     * @param yaw   body yaw in degrees
     */
    public synchronized void recordFrame(String uuid, String name, Map<String, float[]> pose,
                                         float time, float rx, float ry, float rz, float yaw) {
        if (!recording) return;
        EntityRec rec = recs.computeIfAbsent(uuid, k -> {
            EntityRec r = new EntityRec();
            r.ex.name = "rec_" + name;
            return r;
        });
        for (Map.Entry<String, float[]> e : pose.entrySet()) {
            float[] v = e.getValue();
            EpicFightAccess.Frame f = new EpicFightAccess.Frame();
            f.time = time;
            f.tx = v[0]; f.ty = v[1]; f.tz = v[2];
            f.qx = v[3]; f.qy = v[4]; f.qz = v[5]; f.qw = v[6];
            rec.ex.joints.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(f);
        }
        EpicFightAccess.RootFrame rf = new EpicFightAccess.RootFrame();
        rf.time = time; rf.x = rx; rf.y = ry; rf.z = rz; rf.yawDeg = yaw;
        rec.ex.rootMotion.add(rf);
        if (time > rec.ex.length) rec.ex.length = time;
    }

    public synchronized void markTick() { frameCount++; }

    /** Stop recording and write one animation file per recorded entity. */
    public synchronized List<Path> stop(RetargetConfig cfg) {
        recording = false;
        List<Path> written = new ArrayList<>();
        int i = 0;
        for (EntityRec rec : recs.values()) {
            if (rec.ex.joints.isEmpty()) continue;
            // Ensure unique file names even if two entities share a display name.
            rec.ex.name = rec.ex.name + "_" + (i++);
            try {
                written.add(AnimationExporter.writeExtracted(rec.ex, cfg, AnimationExporter.outputDir()));
            } catch (Exception e) {
                EpicFightBBSBridge.LOG.warn("[efbbs] failed to write recording {}", rec.ex.name, e);
            }
        }
        recs.clear();
        return written;
    }
}
