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
        // Stamp the take with whichever character is cast in the editor.
        current.character = CharacterLibrary.INSTANCE.active;
        recording = true;
        EFMocap.LOG.info("[efmocap] recording '{}' started as '{}'", name, current.character);
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
    public void capture(net.minecraft.world.entity.LivingEntity e,
                        EpicFightBridge.AnimSample anim) {
        if (!recording || current == null) return;
        MocapFrame f = new MocapFrame(e.getX(), e.getY(), e.getZ(),
                e.getYRot(), e.yBodyRot, e.getXRot(), anim.animationId, anim.elapsed);
        ItemUtil.record(e, f);
        current.frames.add(f);
    }
}
