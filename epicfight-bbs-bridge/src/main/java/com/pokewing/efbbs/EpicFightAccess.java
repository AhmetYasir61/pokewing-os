package com.pokewing.efbbs;

import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reflection-based access to Epic Fight's animation data.
 *
 * <p>We use reflection so the mod compiles and loads WITHOUT the Epic Fight jar
 * on the classpath, and so a minor Epic Fight update that renames an internal
 * field does not hard-crash -- it degrades to a logged warning. Method names are
 * based on Epic Fight's 1.20.1 API:
 * {@code AnimationClip#getJointTransforms()} -> Map&lt;String,TransformSheet&gt;,
 * {@code TransformSheet#getKeyframes()} -> Keyframe[],
 * {@code Keyframe#time()} / {@code Keyframe#transform()},
 * {@code JointTransform} fields translation (Vec3f) + rotation (Quaternionf).</p>
 */
public final class EpicFightAccess {
    private EpicFightAccess() {}

    /** One sampled joint keyframe in Epic Fight space. */
    public static final class Frame {
        public float time;
        public float tx, ty, tz;          // translation
        public float qx, qy, qz, qw = 1f; // rotation quaternion
    }

    /** One recorded root-motion sample: world position (relative to start) + body yaw. */
    public static final class RootFrame {
        public float time;
        public float x, y, z;   // blocks, relative to recording origin
        public float yawDeg;    // body yaw in degrees
    }

    /** A full extracted animation: joint name -> ordered keyframes. */
    public static final class Extracted {
        public String name;
        public float length;
        public final Map<String, List<Frame>> joints = new LinkedHashMap<>();
        /** Optional root-motion track (set by the live recorder; empty for clip exports). */
        public final List<RootFrame> rootMotion = new ArrayList<>();
    }

    /**
     * Sample the LIVE, fully-blended Epic Fight pose of an entity right now
     * (client-side). Works for any entity that has an Epic Fight patch, including
     * other players in multiplayer. Returns joint-name -> {tx,ty,tz,qx,qy,qz,qw}
     * in Epic Fight space, or null if the entity has no Epic Fight patch.
     */
    public static Map<String, float[]> sampleLivePose(Object entity, float partialTick) {
        try {
            Class<?> caps = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> livingPatch = Class.forName(
                    "yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch");
            Method getPatch = caps.getMethod("getEntityPatch",
                    Class.forName("net.minecraft.world.entity.Entity"), Class.class);
            Object patch = getPatch.invoke(null, entity, livingPatch);
            if (patch == null) return null;

            // Prefer the client animator (holds the rendered, blended pose).
            Object animator = tryInvoke(patch, "getClientAnimator");
            if (animator == null) animator = tryInvoke(patch, "getAnimator");
            if (animator == null) return null;

            Object pose = animator.getClass()
                    .getMethod("getPose", float.class).invoke(animator, partialTick);
            if (pose == null) return null;
            Object dataObj = invoke(pose, "getJointTransformData");
            if (!(dataObj instanceof Map<?, ?> data)) return null;

            Map<String, float[]> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : data.entrySet()) {
                Frame f = new Frame();
                readTransform(e.getValue(), f);
                out.put(String.valueOf(e.getKey()),
                        new float[] {f.tx, f.ty, f.tz, f.qx, f.qy, f.qz, f.qw});
            }
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Extract a single animation clip by its Epic Fight registry key. */
    public static Extracted extract(ResourceLocation key) throws Exception {
        Object staticAnimation = resolveStaticAnimation(key);
        if (staticAnimation == null) {
            throw new IllegalStateException("Animation not found in Epic Fight: " + key);
        }
        Object clip = invoke(staticAnimation, "getAnimationClip");
        if (clip == null || isEmptyClip(clip)) {
            // Clip may be lazy-loaded; force a load then retry.
            tryInvokeVoid(staticAnimation, "loadAnimation");
            clip = invoke(staticAnimation, "getAnimationClip");
        }
        if (clip == null) {
            throw new IllegalStateException("StaticAnimation had no AnimationClip: " + key);
        }
        return extractFromClip(key.toString(), clip);
    }

    /**
     * Enumerate every registered animation key Epic Fight currently holds, via
     * {@code AnimationManager.getInstance().getAnimations(a -> true)}.
     */
    public static List<ResourceLocation> listAllKeys() throws Exception {
        Object manager = resolveAnimationManager();
        List<ResourceLocation> out = new ArrayList<>();
        // getAnimations(Predicate) -> Map<ResourceLocation, AnimationAccessor>
        try {
            Method getAnimations = manager.getClass()
                    .getMethod("getAnimations", java.util.function.Predicate.class);
            Object mapObj = getAnimations.invoke(manager, (java.util.function.Predicate<Object>) a -> true);
            if (mapObj instanceof Map<?, ?> map) {
                for (Object k : map.keySet()) {
                    if (k instanceof ResourceLocation rl) out.add(rl);
                }
                return out;
            }
        } catch (NoSuchMethodException ignored) {
            // fall through to registry scan
        }
        Map<Object, Object> registry = findRegistryMap(manager);
        if (registry != null) {
            for (Object k : registry.keySet()) {
                if (k instanceof ResourceLocation rl) out.add(rl);
            }
        }
        return out;
    }

    /**
     * Offline path: parse an Epic Fight animation JSON file (the format under
     * {@code assets/epicfight/animmodels/animations/...}). Each joint entry has
     * {@code name}, {@code time:[...]} and {@code transform:[[16 floats], ...]}
     * where each transform is a row-major 4x4 matrix.
     */
    public static Extracted extractFromJson(String name, com.google.gson.JsonObject json) {
        Extracted ex = new Extracted();
        ex.name = name;
        com.google.gson.JsonArray joints = json.has("animation")
                ? json.getAsJsonArray("animation")
                : (json.has("joints") ? json.getAsJsonArray("joints") : new com.google.gson.JsonArray());

        float maxTime = 0f;
        for (com.google.gson.JsonElement el : joints) {
            com.google.gson.JsonObject jo = el.getAsJsonObject();
            String jointName = jo.get("name").getAsString();
            com.google.gson.JsonArray times = jo.getAsJsonArray("time");
            com.google.gson.JsonArray transforms = jo.getAsJsonArray("transform");
            List<Frame> frames = new ArrayList<>();
            for (int i = 0; i < times.size(); i++) {
                com.google.gson.JsonArray mat = transforms.get(i).getAsJsonArray();
                double[] m = new double[16];
                for (int k = 0; k < 16 && k < mat.size(); k++) m[k] = mat.get(k).getAsDouble();
                float[] d = MatrixUtil.decompose(m);
                Frame f = new Frame();
                f.time = times.get(i).getAsFloat();
                f.tx = d[0]; f.ty = d[1]; f.tz = d[2];
                f.qx = d[3]; f.qy = d[4]; f.qz = d[5]; f.qw = d[6];
                frames.add(f);
                if (f.time > maxTime) maxTime = f.time;
            }
            if (!frames.isEmpty()) ex.joints.put(jointName, frames);
        }
        ex.length = maxTime;
        return ex;
    }

    private static boolean isEmptyClip(Object clip) {
        try {
            Object jt = invoke(clip, "getJointTransforms");
            return !(jt instanceof Map<?, ?> m) || m.isEmpty();
        } catch (Exception e) {
            return true;
        }
    }

    // --- internals -------------------------------------------------------

    private static Extracted extractFromClip(String name, Object clip) throws Exception {
        Extracted ex = new Extracted();
        ex.name = name;

        Object jointMapObj = invoke(clip, "getJointTransforms");
        if (!(jointMapObj instanceof Map<?, ?> jointMap)) {
            throw new IllegalStateException("getJointTransforms() did not return a Map");
        }

        float maxTime = 0f;
        for (Map.Entry<?, ?> entry : jointMap.entrySet()) {
            String jointName = String.valueOf(entry.getKey());
            Object sheet = entry.getValue();
            Object kfObj = invoke(sheet, "getKeyframes");
            if (kfObj == null || !kfObj.getClass().isArray()) continue;

            int len = java.lang.reflect.Array.getLength(kfObj);
            List<Frame> frames = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                Object kf = java.lang.reflect.Array.get(kfObj, i);
                Frame f = new Frame();
                f.time = ((Number) invoke(kf, "time")).floatValue();
                Object transform = invoke(kf, "transform");
                readTransform(transform, f);
                frames.add(f);
                if (f.time > maxTime) maxTime = f.time;
            }
            if (!frames.isEmpty()) {
                ex.joints.put(jointName, frames);
            }
        }
        ex.length = maxTime;
        return ex;
    }

    private static void readTransform(Object transform, Frame f) {
        if (transform == null) return;
        // JointTransform exposes translation()/rotation()/scale() accessor methods.
        // Vec3f has public x,y,z fields; joml Quaternionf has x,y,z,w.
        Object translation = tryInvoke(transform, "translation");
        if (translation == null) translation = getFieldSilently(transform, "translation");
        if (translation != null) {
            f.tx = floatField(translation, "x");
            f.ty = floatField(translation, "y");
            f.tz = floatField(translation, "z");
        }
        Object rotation = tryInvoke(transform, "rotation");
        if (rotation == null) rotation = getFieldSilently(transform, "rotation");
        if (rotation != null) {
            f.qx = floatField(rotation, "x");
            f.qy = floatField(rotation, "y");
            f.qz = floatField(rotation, "z");
            f.qw = floatFieldOr(rotation, "w", 1f);
        }
    }

    // AnimationManager.byKey(ResourceLocation) is a STATIC method returning an
    // AnimationAccessor (a Supplier). accessor.get() -> StaticAnimation.
    private static Object resolveStaticAnimation(ResourceLocation key) throws Exception {
        Class<?> cls = Class.forName("yesman.epicfight.api.animation.AnimationManager");
        try {
            Method byKey = cls.getMethod("byKey", ResourceLocation.class);
            Object accessor = byKey.invoke(null, key); // static
            if (accessor != null) {
                Object anim = tryInvoke(accessor, "get");
                if (anim != null) return anim;
            }
        } catch (NoSuchMethodException ignored) {
            // fall through to registry scan below
        }
        Object manager = resolveAnimationManager();
        Map<Object, Object> registry = findRegistryMap(manager);
        if (registry != null) {
            Object v = registry.get(key);
            if (v != null) {
                Object got = tryInvoke(v, "get");
                return got != null ? got : v;
            }
        }
        return null;
    }

    private static Object resolveAnimationManager() throws Exception {
        Class<?> cls = Class.forName("yesman.epicfight.api.animation.AnimationManager");
        try {
            Method getInstance = cls.getMethod("getInstance");
            Object inst = getInstance.invoke(null);
            if (inst != null) return inst;
        } catch (NoSuchMethodException ignored) {}
        for (String fld : new String[] {"INSTANCE", "instance"}) {
            try {
                Field f = cls.getDeclaredField(fld);
                f.setAccessible(true);
                Object inst = f.get(null);
                if (inst != null) return inst;
            } catch (NoSuchFieldException ignored) {}
        }
        return cls;
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> findRegistryMap(Object manager) {
        Class<?> cls = (manager instanceof Class<?> c) ? c : manager.getClass();
        Object target = (manager instanceof Class<?>) ? null : manager;
        for (Field f : cls.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    Object v = f.get(target);
                    if (v instanceof Map<?, ?> map && !map.isEmpty()) {
                        // Heuristic: registry is keyed by ResourceLocation.
                        Object anyKey = map.keySet().iterator().next();
                        if (anyKey instanceof ResourceLocation) {
                            return (Map<Object, Object>) v;
                        }
                    }
                } catch (IllegalAccessException ignored) {}
            }
        }
        return null;
    }

    // --- tiny reflection helpers ----------------------------------------

    private static Object invoke(Object target, String method) throws Exception {
        return target.getClass().getMethod(method).invoke(target);
    }

    private static Object tryInvoke(Object target, String method) {
        try { return target.getClass().getMethod(method).invoke(target); }
        catch (Exception e) { return null; }
    }

    private static void tryInvokeVoid(Object target, String method) {
        try { target.getClass().getMethod(method).invoke(target); }
        catch (Exception ignored) {}
    }

    private static Object getFieldSilently(Object target, String name) {
        try {
            Field f = target.getClass().getField(name);
            return f.get(target);
        } catch (Exception e) {
            try {
                Field f = target.getClass().getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private static float floatField(Object target, String name) {
        return floatFieldOr(target, name, 0f);
    }

    private static float floatFieldOr(Object target, String name, float def) {
        Object v = getFieldSilently(target, name);
        if (v instanceof Number n) return n.floatValue();
        // Some vector types expose x()/y()/z() accessors instead of fields.
        Object m = tryInvoke(target, name);
        if (m instanceof Number n) return n.floatValue();
        return def;
    }
}
