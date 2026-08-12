package com.pokewing.pokeface.tracker;

import com.pokewing.pokeface.PokeFace;
import com.pokewing.pokeface.face.FaceState;

import java.util.List;

/**
 * Listens on every supported protocol at once and uses whichever one is actually
 * sending.
 *
 * <p>Trackers disagree about who speaks what: OpenSeeFace's {@code facetracker.py}
 * sends the OpenSeeFace format, while VSeeFace <em>receives</em> that format and
 * sends VMC instead. Binding both removes the single most confusing failure mode
 * — a socket that connects and then never receives a tick.
 */
public final class CompositeTracker implements TrackerSource {

    private final List<TrackerSource> sources;
    private TrackerSource lastLive;

    public CompositeTracker(List<TrackerSource> sources) {
        this.sources = sources;
    }

    @Override
    public String name() {
        TrackerSource live = this.lastLive;
        return live != null ? live.name() : "no tracker data";
    }

    @Override
    public void start() {
        this.sources.forEach(TrackerSource::start);
        PokeFace.LOGGER.info("PokeFace: face tracking listeners up: {}",
                this.sources.stream().map(TrackerSource::name).toList());
    }

    @Override
    public boolean poll(FaceState out) {
        // A source that just delivered wins; otherwise the first live one does.
        if (this.lastLive != null && this.lastLive.poll(out)) {
            return true;
        }
        for (TrackerSource source : this.sources) {
            if (source.poll(out)) {
                if (source != this.lastLive) {
                    PokeFace.LOGGER.info("PokeFace: face tracking data arriving from {}", source.name());
                    this.lastLive = source;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isLive() {
        return this.sources.stream().anyMatch(TrackerSource::isLive);
    }

    /** Total packets seen across all listeners; 0 means nothing is sending. */
    public long packetCount() {
        long total = 0L;
        for (TrackerSource source : this.sources) {
            if (source instanceof OscVmcTracker vmc) {
                total += vmc.packetCount();
            } else if (source instanceof OpenSeeFaceTracker osf) {
                total += osf.packetCount();
            }
        }
        return total;
    }

    @Override
    public void close() {
        this.sources.forEach(TrackerSource::close);
        this.lastLive = null;
    }
}
