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
            Object animator = tryInvoke(patch, "getClientAnimator");
            if (animator == null) animator = tryInvoke(patch, "getAnimator");
            if (animator == null) return out;

            // Scan reachable AnimationPlayer instances for a live StaticAnimation.
            Object player = findActivePlayer(animator);
            if (player != null) {
                Object accessor = tryInvoke(player, "getAnimation");
                Integer id = accessor == null ? null : (Integer) tryInvoke(accessor, "id");
                if (id != null) out.animationId = id;
                Object el = tryInvoke(player, "getElapsedTime");
                if (el instanceof Number n) out.elapsed = n.floatValue();
                Object end = tryInvoke(player, "isEnd");
                if (end instanceof Boolean b) out.ended = b;
            }
        } catch (Throwable ignored) {}
        return out;
    }

    // Heuristic: reflect over the animator's fields to find an AnimationPlayer
    // whose animation is non-null. Robustness improved as we learn the mixer.
    private static Object findActivePlayer(Object animator) {
        for (java.lang.reflect.Field f : allFields(animator.getClass())) {
            try {
                f.setAccessible(true);
                Object v = f.get(animator);
                if (v == null) continue;
                if (v.getClass().getName().endsWith("AnimationPlayer")) return v;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static java.util.List<java.lang.reflect.Field> allFields(Class<?> c) {
        java.util.List<java.lang.reflect.Field> out = new java.util.ArrayList<>();
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            java.util.Collections.addAll(out, k.getDeclaredFields());
        }
        return out;
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
