package com.pokewing.efmocap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Owns the cinematic camera: capturing keyframes from where you're standing,
 * then flying an invisible {@link CameraEntity} along the path while the scene
 * plays. You never need a separate flight controller -- fly normally, look at
 * what you want, and drop a keyframe.
 */
public final class CameraDirector {
    public static final CameraDirector INSTANCE = new CameraDirector();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final CameraPath path = new CameraPath();
    private CameraEntity camera;
    private boolean playing;
    private int tick;
    /** Default travel time between two keyframes, in ticks. */
    public int segmentTicks = 40;

    private CameraDirector() {}

    public CameraPath path() { return path; }
    public boolean isPlaying() { return playing; }
    public int keyCount() { return path.size(); }

    // --- authoring -------------------------------------------------------

    /** Capture a keyframe at the player's current eye position and look angles. */
    public boolean addKeyframeHere() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return false;
        CameraKeyframe k = new CameraKeyframe(
                p.getX(), p.getEyeY(), p.getZ(),
                p.getYRot(), p.getXRot(), 0f,
                mc.options.fov().get(),
                path.isEmpty() ? 0 : segmentTicks);
        path.add(k);
        save();
        return true;
    }

    public void clearPath() {
        path.clear();
        save();
    }

    public boolean removeLast() {
        if (path.isEmpty()) return false;
        path.keys.remove(path.keys.size() - 1);
        save();
        return true;
    }

    // --- playback --------------------------------------------------------

    /** Start flying the camera along the path. Returns false if unusable. */
    public boolean play() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || path.isEmpty()) return false;

        CameraPath.Sample s = path.sample(0f);
        camera = new CameraEntity(ModEntities.CAMERA.get(), mc.level);
        camera.setId(FakeIds.next());
        camera.setPos(s.x, s.y, s.z);
        camera.setYRot(s.yaw);
        camera.setXRot(s.pitch);
        camera.roll = s.roll;
        camera.xo = s.x; camera.yo = s.y; camera.zo = s.z;
        camera.xOld = s.x; camera.yOld = s.y; camera.zOld = s.z;
        mc.level.putNonPlayerEntity(camera.getId(), camera);

        mc.setCameraEntity(camera);
        tick = 0;
        playing = true;
        return true;
    }

    /** Stop and hand the view back to the player. */
    public void stop() {
        Minecraft mc = Minecraft.getInstance();
        playing = false;
        offlineTime = null;
        if (mc.player != null) mc.setCameraEntity(mc.player);
        if (camera != null) {
            camera.discard();
            if (mc.level != null) {
                mc.level.removeEntity(camera.getId(),
                        net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
            camera = null;
        }
    }

    /** Advance one client tick. */
    public void tick() {
        if (!playing || camera == null) return;

        CameraPath.Sample prev = path.sample(tick);
        tick++;
        CameraPath.Sample cur = path.sample(tick);
        if (prev == null || cur == null) { stop(); return; }

        // Previous tick -> current tick, so the renderer lerps smoothly.
        camera.xo = prev.x; camera.yo = prev.y; camera.zo = prev.z;
        camera.xOld = prev.x; camera.yOld = prev.y; camera.zOld = prev.z;
        camera.yRotO = prev.yaw; camera.xRotO = prev.pitch;
        camera.setPos(cur.x, cur.y, cur.z);
        camera.setYRot(cur.yaw);
        camera.setXRot(cur.pitch);
        camera.roll = cur.roll;

        if (tick >= path.durationTicks()) {
            stop();
        }
    }

    /** Sub-tick sample used by the render events for angles and FOV. */
    public CameraPath.Sample sampleForRender(float partialTick) {
        if (!playing) return null;
        // Offline rendering owns the clock; the wall-clock partialTick would
        // otherwise nudge the camera off the frame we're trying to capture.
        if (offlineTime != null) return path.sample(offlineTime.floatValue());
        return path.sample(Math.min(tick + partialTick, path.durationTicks()));
    }

    private Double offlineTime;

    /** True once an offline render has passed the end of the path. */
    public boolean offlineFinished() {
        return offlineTime != null && offlineTime >= path.durationTicks();
    }

    /** Place the camera at an exact fractional tick for offline rendering. */
    public void renderAt(double tickPos) {
        if (camera == null) return;
        offlineTime = tickPos;
        CameraPath.Sample s = path.sample((float) tickPos);
        if (s == null) return;
        // Old == new so Minecraft's own interpolation can't move it.
        camera.xo = s.x; camera.yo = s.y; camera.zo = s.z;
        camera.xOld = s.x; camera.yOld = s.y; camera.zOld = s.z;
        camera.setPos(s.x, s.y, s.z);
        camera.yRotO = s.yaw; camera.setYRot(s.yaw);
        camera.xRotO = s.pitch; camera.setXRot(s.pitch);
        camera.roll = s.roll;
    }

    // --- persistence -----------------------------------------------------

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("efmocap").resolve("camera.json");
    }

    public void save() {
        try {
            Files.createDirectories(file().getParent());
            try (Writer w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
                GSON.toJson(path, w);
            }
        } catch (Exception e) {
            EFMocap.LOG.warn("[efmocap] failed to save camera path", e);
        }
    }

    public void load() {
        try {
            if (!Files.exists(file())) return;
            try (Reader r = Files.newBufferedReader(file(), StandardCharsets.UTF_8)) {
                CameraPath loaded = GSON.fromJson(r, CameraPath.class);
                if (loaded != null && loaded.keys != null) {
                    path.clear();
                    path.keys.addAll(loaded.keys);
                }
            }
        } catch (Exception e) {
            EFMocap.LOG.warn("[efmocap] failed to load camera path", e);
        }
    }
}
