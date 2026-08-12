package com.pokewing.pokeface.tracker;

import com.pokewing.pokeface.face.FaceState;

/**
 * A live face-capture source. Minecraft's JVM has no camera access of its own,
 * so a source is always a bridge to an external tracker process that streams
 * blendshapes into the game.
 */
public interface TrackerSource extends AutoCloseable {

    String name();

    /** Opens sockets/handles. Must not throw when the tracker is simply absent. */
    void start();

    /**
     * Writes the newest sample into {@code out}.
     *
     * @return false when no fresh sample arrived inside the staleness window, in
     *         which case the caller falls back to the reaction/idle drivers.
     */
    boolean poll(FaceState out);

    /** True while packets are arriving; drives the "camera found" HUD state. */
    boolean isLive();

    @Override
    void close();
}
