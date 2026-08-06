package com.pokewing.efmocap;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Captures rendered frames to PNGs and stitches them into an .mp4 with ffmpeg.
 *
 * <p>PNG encoding runs on a background thread with a bounded queue and a
 * caller-runs policy: if the writer falls behind, the render thread waits
 * instead of dropping frames or exhausting memory. That trades frame rate for a
 * complete, correctly-ordered capture — the right call for filming.</p>
 *
 * <p>The output frame rate is measured from the real capture duration, so the
 * video plays back at true speed even if the game didn't hold a steady fps.</p>
 */
public final class VideoRecorder {
    public static final VideoRecorder INSTANCE = new VideoRecorder();

    private ThreadPoolExecutor writer;
    private Path dir;
    private int frames;
    private long startNanos;
    private volatile boolean active;
    private boolean hidGui;

    /** Set > 0 to force an output frame rate instead of measuring it. */
    public double forcedFps = 0;
    /** ffmpeg executable; override if it isn't on PATH. */
    public String ffmpeg = "ffmpeg";

    private VideoRecorder() {}

    public boolean isActive() { return active; }
    public int frameCount() { return frames; }
    public Path outputDir() { return dir; }

    public static Path renderRoot() {
        return FMLPaths.GAMEDIR.get().resolve("efmocap-render");
    }

    public boolean start() {
        if (active) return false;
        try {
            dir = renderRoot().resolve("take_" + System.currentTimeMillis());
            Files.createDirectories(dir);
        } catch (Exception e) {
            EFMocap.LOG.error("[efmocap] cannot create render dir", e);
            return false;
        }
        // Small bounded queue; caller-runs keeps every frame, in order.
        writer = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(8), r -> {
                    Thread t = new Thread(r, "efmocap-frame-writer");
                    t.setDaemon(true);
                    return t;
                }, new ThreadPoolExecutor.CallerRunsPolicy());

        frames = 0;
        startNanos = System.nanoTime();
        active = true;

        Minecraft mc = Minecraft.getInstance();
        hidGui = mc.options.hideGui;
        mc.options.hideGui = true;
        EFMocap.LOG.info("[efmocap] video capture -> {}", dir);
        return true;
    }

    /** Called at the end of each rendered frame while capturing. */
    public void captureFrame() {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getMainRenderTarget() == null) return;
        try {
            NativeImage img = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            final int index = frames++;
            final Path out = dir.resolve(String.format(Locale.ROOT, "frame_%06d.png", index));
            writer.execute(() -> {
                try {
                    img.writeToFile(out);
                } catch (Exception e) {
                    EFMocap.LOG.warn("[efmocap] frame {} write failed", index, e);
                } finally {
                    img.close();
                }
            });
        } catch (Throwable t) {
            EFMocap.LOG.warn("[efmocap] frame capture failed", t);
        }
    }

    /**
     * Stop capturing and kick off encoding. Returns the measured fps, or -1 if
     * nothing was captured.
     */
    public double stop() {
        if (!active) return -1;
        active = false;

        Minecraft mc = Minecraft.getInstance();
        mc.options.hideGui = hidGui;

        double seconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;
        double fps = forcedFps > 0 ? forcedFps
                : (seconds > 0.1 && frames > 1 ? frames / seconds : 30.0);

        writer.shutdown();
        final int total = frames;
        final Path folder = dir;
        final double useFps = Math.max(1.0, Math.min(fps, 240.0));

        new Thread(() -> {
            try {
                writer.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            if (total <= 0) {
                EFMocap.LOG.warn("[efmocap] no frames captured");
                return;
            }
            encode(folder, useFps);
        }, "efmocap-encode").start();

        return useFps;
    }

    private void encode(Path folder, double fps) {
        Path out = folder.resolve("video.mp4");
        ProcessBuilder pb = new ProcessBuilder(
                ffmpeg, "-y",
                "-framerate", String.format(Locale.ROOT, "%.3f", fps),
                "-i", folder.resolve("frame_%06d.png").toString(),
                "-c:v", "libx264",
                "-preset", "medium",
                "-crf", "18",
                "-pix_fmt", "yuv420p",
                out.toString());
        pb.redirectErrorStream(true);
        try {
            EFMocap.LOG.info("[efmocap] encoding at {} fps -> {}", fps, out);
            Process p = pb.start();
            try (var in = p.getInputStream()) {
                in.readAllBytes(); // drain so ffmpeg never blocks on a full pipe
            }
            int code = p.waitFor();
            if (code == 0) {
                EFMocap.LOG.info("[efmocap] video ready: {}", out);
                ClientSystems.msg("§a[efmocap] video hazır: " + out);
            } else {
                EFMocap.LOG.warn("[efmocap] ffmpeg exited with {}", code);
                ClientSystems.msg("§c[efmocap] ffmpeg hata verdi (kod " + code
                        + "). PNG kareler burada: " + folder);
            }
        } catch (Exception e) {
            EFMocap.LOG.warn("[efmocap] ffmpeg could not be run", e);
            ClientSystems.msg("§e[efmocap] ffmpeg bulunamadı. PNG kareler: " + folder);
        }
    }
}
