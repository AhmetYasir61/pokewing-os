package com.pokewing.efmocap;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Reflection bridge to Epic Fight (so EFMocap compiles/loads without the Epic
 * Fight jar). Method names verified against epicfight 20.14.17 (MC 1.20.1).
 *
 * <p>Two capabilities matter here:</p>
 * <ul>
 *   <li>READ the performer's state — which animation is playing and its progress
 *       (to record).</li>
 *   <li>DRIVE a clone — play a given animation by id (to replay).</li>
 * </ul>
 */
public final class EpicFightBridge {
    private EpicFightBridge() {}

    private static final String CAPS = "yesman.epicfight.world.capabilities.EpicFightCapabilities";
    private static final String LIVING_PATCH =
            "yesman.epicfight.world.capabilities.entitypatch.entity.LivingEntityPatch"; // fallback tried below
    private static final String LIVING_PATCH_ALT =
            "yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch";
    private static final String ANIM_MANAGER = "yesman.epicfight.api.animation.AnimationManager";

    /** @return the Epic Fight LivingEntityPatch for an entity, or null. */
    public static Object getPatch(Object entity) {
        try {
            Class<?> caps = Class.forName(CAPS);
            Class<?> patchClass = livingPatchClass();
            Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
            Method getPatch = caps.getMethod("getEntityPatch", entityClass, Class.class);
            return getPatch.invoke(null, entity, patchClass);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Class<?> livingPatchClass() throws ClassNotFoundException {
        try {
            return Class.forName(LIVING_PATCH_ALT);
        } catch (ClassNotFoundException e) {
            return Class.forName(LIVING_PATCH);
        }
    }

    // --- reading the performer ------------------------------------------

    /** Result of sampling what an entity is currently playing. */
    public static final class AnimSample {
        public int animationId = -1;   // Epic Fight animation registry id, or -1
        public float elapsed = 0f;     // elapsed time within that animation
        public boolean ended = false;
    }

    /**
     * Best-effort read of the currently-playing animation on the base layer.
     * Epic Fight has no no-arg "current animation" getter, so we scan the
     * animator's mixer/layers via reflection. Returns id -1 if it can't tell,
     * in which case callers fall back to pose-level capture.
     */
    public static AnimSample sampleAnimation(Object patch, float partialTick) {
        AnimSample out = new AnimSample();
        if (patch == null) return out;
        try {
            // Verified path: ClientAnimator.baseLayer (public field) ->
            // Layer.animationPlayer (public field) -> AnimationPlayer.
            Object animator = tryInvoke(patch, "getClientAnimator");
            if (animator == null) animator = tryInvoke(patch, "getAnimator");
            if (animator == null) return out;

            Object player = getFieldPath(animator, "baseLayer", "animationPlayer");
            if (player == null) return out;

            Object accessor = tryInvoke(player, "getAnimation");
            Object id = accessor == null ? null : tryInvoke(accessor, "id");
            if (id instanceof Integer i) out.animationId = i;
            Object el = tryInvoke(player, "getElapsedTime");
            if (el instanceof Number n) out.elapsed = n.floatValue();
            Object end = tryInvoke(player, "isEnd");
            if (end instanceof Boolean b) out.ended = b;
        } catch (Throwable ignored) {}
        return out;
    }

    /** Walk a chain of (public or declared) fields. */
    private static Object getFieldPath(Object obj, String... fields) {
        Object cur = obj;
        for (String name : fields) {
            if (cur == null) return null;
            cur = getField(cur, name);
        }
        return cur;
    }

    private static Object getField(Object target, String name) {
        for (Class<?> k = target.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
            try {
                java.lang.reflect.Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable t) { return null; }
        }
        return null;
    }

    // --- driving a clone -------------------------------------------------

    /** Resolve an Epic Fight animation accessor by its registry id. */
    public static Object animationById(int id) {
        try {
            Class<?> mgr = Class.forName(ANIM_MANAGER);
            Method byId = mgr.getMethod("byId", int.class);
            return byId.invoke(null, id);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Play an animation (by id) on a clone patch, client-side, with a transition. */
    public static boolean playById(Object patch, int id, float transition) {
        if (patch == null || id < 0) return false;
        Object accessor = animationById(id);
        if (accessor == null) return false;
        // Try client-side first (filming is client-side), then the generic play.
        for (String m : new String[] {"playAnimationInClientSide", "playAnimation"}) {
            try {
                Method method = findPlayMethod(patch.getClass(), m);
                if (method != null) {
                    method.invoke(patch, accessor, transition);
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    /**
     * Force a clone to show an exact frame: ensure its base-layer animation is
     * {@code animId} and pin the elapsed time to {@code elapsed}. Called every
     * client tick AFTER Epic Fight's own tick so it overrides the clone's
     * auto-computed living motion (walk/idle) and stays frame-synced with the
     * recording.
     */
    public static boolean forceAnimation(Object patch, int animId, float prevElapsed, float elapsed) {
        if (patch == null || animId < 0) return false;
        try {
            Object animator = tryInvoke(patch, "getClientAnimator");
            if (animator == null) animator = tryInvoke(patch, "getAnimator");
            if (animator == null) return false;

            Object player = getFieldPath(animator, "baseLayer", "animationPlayer");
            Object curAnim = player == null ? null : tryInvoke(player, "getAnimation");
            Object curId = curAnim == null ? null : tryInvoke(curAnim, "id");

            boolean switched = false;
            if (!(curId instanceof Integer i) || i != animId) {
                playById(patch, animId, 0f);
                player = getFieldPath(animator, "baseLayer", "animationPlayer");
                switched = true;
            }
            if (player == null) return false;

            // Epic Fight renders the pose by interpolating prevElapsedTime ->
            // elapsedTime with the frame's partialTick. The single-arg
            // setElapsedTime writes BOTH fields, which pins the pose for the
            // whole tick and looks like stuttering. Feed the real previous
            // value so the clone animates smoothly between ticks.
            // On an animation switch prev == current, so it doesn't lerp across
            // two different animations.
            float prev = switched ? elapsed : prevElapsed;
            Method set2 = findMethod(player.getClass(), "setElapsedTime", float.class, float.class);
            if (set2 != null) {
                set2.invoke(player, prev, elapsed);
                return true;
            }
            Method set1 = findMethod(player.getClass(), "setElapsedTime", float.class);
            if (set1 != null) {
                set1.invoke(player, elapsed);
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static Method findMethod(Class<?> c, String name, Class<?>... params) {
        try { return c.getMethod(name, params); }
        catch (Throwable t) { return null; }
    }

    private static Method findPlayMethod(Class<?> patchClass, String name) {
        for (Method m : patchClass.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 2
                    && m.getParameterTypes()[1] == float.class) {
                return m;
            }
        }
        return null;
    }

    // --- helpers ---------------------------------------------------------

    private static Object tryInvoke(Object target, String method) {
        try { return target.getClass().getMethod(method).invoke(target); }
        catch (Throwable e) { return null; }
    }

    public static Optional<Object> patchOf(Object entity) {
        return Optional.ofNullable(getPatch(entity));
    }
}
