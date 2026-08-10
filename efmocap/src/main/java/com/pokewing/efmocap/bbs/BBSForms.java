package com.pokewing.efmocap.bbs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pokewing.efmocap.EFMocap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.loading.FMLPaths;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Lets BBS be the character builder for EFMocap actors: a form authored in
 * BBS's own editor — model, texture, body parts, the lot — can be exported to
 * {@code config/efmocap/forms} and pinned to an Epic Fight bone, where BBS
 * draws it with its own renderer while Epic Fight keeps animating the body.
 *
 * <p>All reflection: BBS arrives on Forge through Sinytra Connector, which
 * remaps its signatures, so methods are matched by name and argument count
 * rather than exact types, and EFMocap still runs with BBS absent.</p>
 */
public final class BBSForms {
    private BBSForms() {}

    private static final String FORM_UTILS = "mchorse.bbs_mod.forms.FormUtils";
    private static final String FORM_UTILS_CLIENT = "mchorse.bbs_mod.forms.FormUtilsClient";
    private static final String DATA_TO_STRING = "mchorse.bbs_mod.data.DataToString";
    private static final String MC_ENTITY = "mchorse.bbs_mod.forms.entities.MCEntity";
    private static final String RENDER_CTX = "mchorse.bbs_mod.forms.renderers.FormRenderingContext";
    private static final String RENDER_TYPE = "mchorse.bbs_mod.forms.renderers.FormRenderType";

    private static final Map<String, Object> FORMS = new HashMap<>();
    private static final Map<Entity, Object> ENTITY_WRAPPERS = new java.util.WeakHashMap<>();

    public static Path formsDir() {
        return FMLPaths.CONFIGDIR.get().resolve("efmocap").resolve("forms");
    }

    /** Form files exported from BBS and dropped into the forms folder. */
    public static List<String> available() {
        List<String> out = new ArrayList<>();
        Path d = formsDir();
        try {
            Files.createDirectories(d);
            try (Stream<Path> s = Files.list(d)) {
                s.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                 .forEach(p -> out.add(p.getFileName().toString()));
            }
        } catch (Exception ignored) {}
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    /** Parse a BBS form file, once. Null when BBS is absent or it won't read. */
    public static Object load(String fileName) {
        if (fileName == null || fileName.isEmpty() || !BBSBridge.isLoaded()) return null;
        if (FORMS.containsKey(fileName)) return FORMS.get(fileName);

        Object form = null;
        try {
            Path p = formsDir().resolve(fileName);
            if (Files.isRegularFile(p)) {
                String json = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                Object map = Class.forName(DATA_TO_STRING)
                        .getMethod("mapFromString", String.class).invoke(null, json);
                if (map != null) {
                    Method fromData = byName(Class.forName(FORM_UTILS), "fromData", 1, map.getClass());
                    if (fromData != null) form = fromData.invoke(null, map);
                }
            }
        } catch (Throwable t) {
            EFMocap.LOG.warn("[efmocap] could not read BBS form {}", fileName, t);
        }
        // Cache failures too, so a bad file isn't re-parsed every frame.
        FORMS.put(fileName, form);
        if (form != null) EFMocap.LOG.info("[efmocap] loaded BBS form {}", fileName);
        return form;
    }

    public static void reload() {
        FORMS.clear();
    }

    /**
     * Draw a form with BBS's renderer using the transform already on the stack,
     * so callers can place it on a bone first.
     */
    public static boolean render(Object form, Entity entity, PoseStack stack,
                                 int light, int overlay, float partialTick) {
        if (form == null || !BBSBridge.isLoaded()) return false;
        try {
            Object wrapper = wrap(entity);
            if (wrapper == null) return false;

            Class<?> ctxClass = Class.forName(RENDER_CTX);
            Object ctx = ctxClass.getConstructor().newInstance();

            Object entityType = enumConstant(RENDER_TYPE, "ENTITY");
            Method set = byName(ctxClass, "set", 6, null);
            if (set == null || entityType == null) return false;
            set.invoke(ctx, entityType, wrapper, stack, light, overlay, partialTick);

            // Give BBS the live camera; some form types need it for billboarding.
            Method camera = byName(ctxClass, "camera", 1,
                    Minecraft.getInstance().gameRenderer.getMainCamera().getClass());
            if (camera != null) {
                camera.invoke(ctx, Minecraft.getInstance().gameRenderer.getMainCamera());
            }

            Method render = byName(Class.forName(FORM_UTILS_CLIENT), "render", 2, null);
            if (render == null) return false;
            render.invoke(null, form, ctx);
            return true;
        } catch (Throwable t) {
            EFMocap.LOG.warn("[efmocap] BBS form render failed", t);
            return false;
        }
    }

    /** BBS wants its own entity abstraction; wrap each clone once. */
    private static Object wrap(Entity entity) {
        Object cached = ENTITY_WRAPPERS.get(entity);
        if (cached != null) return cached;
        try {
            Class<?> mc = Class.forName(MC_ENTITY);
            for (Constructor<?> c : mc.getConstructors()) {
                if (c.getParameterCount() == 1
                        && c.getParameterTypes()[0].isInstance(entity)) {
                    Object w = c.newInstance(entity);
                    ENTITY_WRAPPERS.put(entity, w);
                    return w;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Match by name and arity — Connector remaps BBS's parameter types, so the
     * declared signature won't line up with what we can name here.
     */
    private static Method byName(Class<?> owner, String name, int args, Class<?> firstArg) {
        Method fallback = null;
        for (Method m : owner.getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args) continue;
            if (firstArg != null && m.getParameterTypes()[0].isAssignableFrom(firstArg)) return m;
            if (fallback == null) fallback = m;
        }
        return fallback;
    }

    private static Object enumConstant(String className, String name) {
        try {
            Class<?> c = Class.forName(className);
            java.lang.reflect.Field f = c.getField(name);
            return f.get(null);
        } catch (Throwable t) {
            return null;
        }
    }
}
