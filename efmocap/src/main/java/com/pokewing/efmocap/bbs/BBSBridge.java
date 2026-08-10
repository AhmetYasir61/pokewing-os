package com.pokewing.efmocap.bbs;

import com.pokewing.efmocap.EFMocap;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Reflection bridge to BBS (Blockbuster Studio), so BBS can act as the camera
 * and timeline for an EFMocap scene: while one of its films is playing, our
 * actors are posed from that film's clock instead of our own, and BBS's camera
 * sees them like any other part of the shot.
 *
 * <p>Reflection rather than a compile dependency for two reasons: BBS ships as
 * a Fabric mod that reaches Forge through Sinytra Connector, and EFMocap has to
 * keep loading with BBS absent.</p>
 */
public final class BBSBridge {
    private BBSBridge() {}

    private static final String CLIENT = "mchorse.bbs_mod.BBSModClient";

    private static Boolean present;

    public static boolean isLoaded() {
        if (present == null) {
            boolean listed = ModList.get() != null && ModList.get().isLoaded("bbs_mod");
            if (!listed) {
                // Under Connector the mod id isn't always visible to Forge's
                // list, so fall back to asking the classloader directly.
                try {
                    Class.forName(CLIENT);
                    listed = true;
                } catch (Throwable ignored) {}
            }
            present = listed;
            EFMocap.LOG.info("[efmocap] BBS present: {}", listed);
        }
        return present;
    }

    /** State of the BBS film currently playing, if any. */
    public static final class FilmState {
        public boolean playing;
        public boolean paused;
        public int tick;
        public int duration;
    }

    /**
     * Look for a running BBS film and report its playhead. Returns a state with
     * {@code playing == false} when BBS is absent or nothing is being played.
     */
    public static FilmState film() {
        FilmState st = new FilmState();
        if (!isLoaded()) return st;
        try {
            Object films = Class.forName(CLIENT).getMethod("getFilms").invoke(null);
            if (films == null) return st;

            Object controller = findController(films);
            if (controller == null) return st;

            Object tick = call(controller, "getTick");
            if (!(tick instanceof Integer t)) return st;

            st.playing = true;
            st.tick = t;
            Object dur = field(controller, "duration");
            if (dur instanceof Integer d) st.duration = d;
            Object paused = field(controller, "paused");
            if (paused instanceof Boolean b) st.paused = b;

            Object finished = call(controller, "hasFinished");
            if (finished instanceof Boolean f && f) st.playing = false;
        } catch (Throwable ignored) {}
        return st;
    }

    /**
     * Films keeps its controllers in a map keyed by film id; we don't know the
     * id, so take the first live controller we find.
     */
    private static Object findController(Object films) {
        for (Class<?> k = films.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) {
                if (!Map.class.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(films);
                    if (!(v instanceof Map<?, ?> map)) continue;
                    for (Object value : map.values()) {
                        if (value != null && value.getClass().getName()
                                .contains("FilmController")) {
                            return value;
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }
        return null;
    }

    private static Object call(Object target, String method) {
        try { return target.getClass().getMethod(method).invoke(target); }
        catch (Throwable t) { return null; }
    }

    private static Object field(Object target, String name) {
        for (Class<?> k = target.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable t) { return null; }
        }
        return null;
    }
}
