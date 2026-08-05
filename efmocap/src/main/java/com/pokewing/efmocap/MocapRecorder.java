package com.pokewing.efmocap;

/**
 * Captures the local player's performance, one {@link MocapFrame} per client
 * tick. Action-level: it records the Epic Fight animation id (via
 * {@link EpicFightBridge#sampleAnimation}) rather than a raw pose.
 */
public final class MocapRecorder {
    public static final MocapRecorder INSTANCE = new MocapRecorder();

    private MocapRecording current;
    private boolean recording;

    private MocapRecorder() {}

    public boolean isRecording() { return recording; }
    public MocapRecording last() { return current; }

    public void start(String name) {
        current = new MocapRecording(name);
        recording = true;
        EFMocap.LOG.info("[efmocap] recording '{}' started", name);
    }

    public MocapRecording stop() {
        recording = false;
        if (current != null) {
            EFMocap.LOG.info("[efmocap] recording '{}' stopped, {} frames",
                    current.name, current.length());
        }
        return current;
    }

    /** Called each client tick while recording. */
    public void capture(double x, double y, double z, float yRot, float yBodyRot, float xRot,
                        EpicFightBridge.AnimSample anim) {
        if (!recording || current == null) return;
        current.frames.add(new MocapFrame(x, y, z, yRot, yBodyRot, xRot,
                anim.animationId, anim.elapsed));
    }
}
