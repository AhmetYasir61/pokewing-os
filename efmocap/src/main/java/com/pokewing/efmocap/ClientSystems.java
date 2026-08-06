package com.pokewing.efmocap;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Client wiring: performance capture, clone replay, cinematic camera and video
 * capture, exposed through keybinds and the {@code /efmocap} client command.
 */
@Mod.EventBusSubscriber(modid = EFMocap.MOD_ID, value = Dist.CLIENT)
public final class ClientSystems {
    private ClientSystems() {}

    private static boolean loaded = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();

        if (!loaded && mc.level != null) {
            int n = TakeLibrary.INSTANCE.loadAll();
            CameraDirector.INSTANCE.load();
            loaded = true;
            if (n > 0 || CameraDirector.INSTANCE.keyCount() > 0) {
                msg("§7[efmocap] " + n + " çekim, "
                        + CameraDirector.INSTANCE.keyCount() + " kamera noktası yüklendi");
            }
        }

        handleKeys();

        if (mc.isPaused()) return;

        if (MocapRecorder.INSTANCE.isRecording()) {
            LocalPlayer p = mc.player;
            if (p != null) {
                EpicFightBridge.AnimSample anim = EFMocap.isEpicFightLoaded()
                        ? EpicFightBridge.sampleAnimation(EpicFightBridge.getPatch(p), 1.0f)
                        : new EpicFightBridge.AnimSample();
                MocapRecorder.INSTANCE.capture(p, anim);
            }
        }

        ReplayDirector.INSTANCE.tick();
        CameraDirector.INSTANCE.tick();
    }

    private static void handleKeys() {
        if (Keys.RECORD != null) while (Keys.RECORD.consumeClick()) toggleRecord();
        if (Keys.PLAY != null) while (Keys.PLAY.consumeClick()) stageScene();
        if (Keys.CLEAR != null) while (Keys.CLEAR.consumeClick()) {
            ReplayDirector.INSTANCE.clearAll();
            msg("§e[efmocap] klonlar temizlendi");
        }
        if (Keys.CAM_ADD != null) while (Keys.CAM_ADD.consumeClick()) {
            boolean ok = CameraDirector.INSTANCE.addKeyframeHere();
            msg(ok ? "§b[efmocap] kamera noktası " + CameraDirector.INSTANCE.keyCount()
                    + " eklendi" : "§ceklenemedi");
        }
        if (Keys.CINEMATIC != null) while (Keys.CINEMATIC.consumeClick()) playCinematic();
        if (Keys.CAM_STOP != null) while (Keys.CAM_STOP.consumeClick()) {
            CameraDirector.INSTANCE.stop();
            if (VideoRecorder.INSTANCE.isActive()) stopVideo();
            msg("§e[efmocap] kamera durdu");
        }
        if (Keys.VIDEO != null) while (Keys.VIDEO.consumeClick()) {
            if (VideoRecorder.INSTANCE.isActive()) stopVideo();
            else msg(VideoRecorder.INSTANCE.start()
                    ? "§a[efmocap] video kaydı başladı" : "§cvideo başlatılamadı");
        }
    }

    // --- render hooks ----------------------------------------------------

    /** Drive camera angles (including roll) while the cinematic plays. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        CameraPath.Sample s = CameraDirector.INSTANCE
                .sampleForRender((float) event.getPartialTick());
        if (s == null) return;
        event.setYaw(s.yaw);
        event.setPitch(s.pitch);
        event.setRoll(s.roll);
    }

    /** Drive FOV while the cinematic plays (dolly-zoom friendly). */
    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        CameraPath.Sample s = CameraDirector.INSTANCE
                .sampleForRender((float) event.getPartialTick());
        if (s != null && s.fov > 1) event.setFOV(s.fov);
    }

    /** Grab the finished frame when capturing video. */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) VideoRecorder.INSTANCE.captureFrame();
    }

    // --- actions ---------------------------------------------------------

    private static void toggleRecord() {
        if (MocapRecorder.INSTANCE.isRecording()) {
            MocapRecording rec = MocapRecorder.INSTANCE.stop();
            if (rec != null && !rec.isEmpty()) {
                TakeLibrary.INSTANCE.add(rec);
                msg("§a[efmocap] '" + rec.name + "' kaydedildi (" + rec.length() + " kare)");
            } else {
                msg("§c[efmocap] boş kayıt");
            }
        } else {
            MocapRecorder.INSTANCE.start(TakeLibrary.INSTANCE.nextName());
            msg("§a[efmocap] kayıt başladı. Dövüş! Durdurmak: K");
        }
    }

    private static void stageScene() {
        int n = ReplayDirector.INSTANCE.playScene(true);
        msg(n > 0 ? "§a[efmocap] sahne oynuyor — " + n + " oyuncu"
                  : "§c[efmocap] kayıtlı çekim yok (K ile kaydet)");
    }

    /** Restart the scene and fly the camera through it, recording if asked. */
    private static void playCinematic() {
        if (CameraDirector.INSTANCE.keyCount() < 2) {
            msg("§c[efmocap] en az 2 kamera noktası gerekli (C ile ekle)");
            return;
        }
        if (ReplayDirector.INSTANCE.activeCount() == 0) ReplayDirector.INSTANCE.playScene(true);
        else ReplayDirector.INSTANCE.restart();

        if (CameraDirector.INSTANCE.play()) {
            msg("§b[efmocap] sinematik oynuyor (" + CameraDirector.INSTANCE.keyCount()
                    + " nokta). Durdurmak: B");
        } else {
            msg("§ckamera başlatılamadı");
        }
    }

    private static void stopVideo() {
        double fps = VideoRecorder.INSTANCE.stop();
        msg("§a[efmocap] video kaydı durdu — " + VideoRecorder.INSTANCE.frameCount()
                + " kare @ " + String.format(java.util.Locale.ROOT, "%.1f", fps)
                + " fps, kodlanıyor…");
    }

    // --- commands --------------------------------------------------------

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("efmocap")
                .then(Commands.literal("rec")
                        .then(Commands.literal("start").executes(c -> {
                            MocapRecorder.INSTANCE.start(TakeLibrary.INSTANCE.nextName());
                            msg("§akayıt başladı"); return 1; }))
                        .then(Commands.literal("stop").executes(c -> { toggleRecord(); return 1; })))
                .then(Commands.literal("scene").executes(c -> { stageScene(); return 1; }))
                .then(Commands.literal("restart").executes(c -> {
                    ReplayDirector.INSTANCE.restart(); msg("§asahne baştan"); return 1; }))
                .then(Commands.literal("list").executes(c -> {
                    java.util.List<String> names = TakeLibrary.INSTANCE.names();
                    if (names.isEmpty()) { msg("§7kayıtlı çekim yok"); return 0; }
                    msg("§7çekimler (" + names.size() + "):");
                    for (String n : names) {
                        MocapRecording r = TakeLibrary.INSTANCE.get(n);
                        msg("§7 - " + n + " (" + (r == null ? 0 : r.length()) + " kare)");
                    }
                    return names.size(); }))
                .then(Commands.literal("play")
                        .then(Commands.argument("take", StringArgumentType.string())
                                .executes(c -> {
                                    String n = StringArgumentType.getString(c, "take");
                                    MocapRecording r = TakeLibrary.INSTANCE.get(n);
                                    if (r == null) { msg("§cçekim yok: " + n); return 0; }
                                    boolean ok = ReplayDirector.INSTANCE.play(r, true);
                                    msg(ok ? "§a'" + n + "' oynuyor" : "§cklon oluşturulamadı");
                                    return ok ? 1 : 0; })))
                .then(Commands.literal("delete")
                        .then(Commands.argument("take", StringArgumentType.string())
                                .executes(c -> {
                                    String n = StringArgumentType.getString(c, "take");
                                    boolean ok = TakeLibrary.INSTANCE.remove(n);
                                    msg(ok ? "§e'" + n + "' silindi" : "§cçekim yok: " + n);
                                    return ok ? 1 : 0; })))
                .then(Commands.literal("clear").executes(c -> {
                    ReplayDirector.INSTANCE.clearAll(); msg("§eklonlar temizlendi"); return 1; }))
                // --- camera ---
                .then(Commands.literal("cam")
                        .then(Commands.literal("add").executes(c -> {
                            CameraDirector.INSTANCE.addKeyframeHere();
                            msg("§bnokta " + CameraDirector.INSTANCE.keyCount()); return 1; }))
                        .then(Commands.literal("undo").executes(c -> {
                            boolean ok = CameraDirector.INSTANCE.removeLast();
                            msg(ok ? "§eson nokta silindi" : "§cnokta yok"); return 1; }))
                        .then(Commands.literal("clear").executes(c -> {
                            CameraDirector.INSTANCE.clearPath();
                            msg("§ekamera yolu temizlendi"); return 1; }))
                        .then(Commands.literal("play").executes(c -> { playCinematic(); return 1; }))
                        .then(Commands.literal("stop").executes(c -> {
                            CameraDirector.INSTANCE.stop(); msg("§ekamera durdu"); return 1; }))
                        .then(Commands.literal("info").executes(c -> {
                            msg("§7kamera: " + CameraDirector.INSTANCE.keyCount() + " nokta, "
                                    + CameraDirector.INSTANCE.path().durationTicks() + " tick ("
                                    + String.format(java.util.Locale.ROOT, "%.1f",
                                        CameraDirector.INSTANCE.path().durationTicks() / 20.0)
                                    + " sn)"); return 1; }))
                        .then(Commands.literal("speed")
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 2000))
                                        .executes(c -> {
                                            int t = IntegerArgumentType.getInteger(c, "ticks");
                                            CameraDirector.INSTANCE.segmentTicks = t;
                                            msg("§bnoktalar arası " + t + " tick ("
                                                    + String.format(java.util.Locale.ROOT, "%.2f", t / 20.0)
                                                    + " sn)"); return 1; }))))
                // --- video ---
                .then(Commands.literal("video")
                        .then(Commands.literal("start").executes(c -> {
                            msg(VideoRecorder.INSTANCE.start() ? "§avideo kaydı başladı"
                                    : "§cbaşlatılamadı"); return 1; }))
                        .then(Commands.literal("stop").executes(c -> { stopVideo(); return 1; }))
                        .then(Commands.literal("fps")
                                .then(Commands.argument("fps", IntegerArgumentType.integer(0, 240))
                                        .executes(c -> {
                                            int f = IntegerArgumentType.getInteger(c, "fps");
                                            VideoRecorder.INSTANCE.forcedFps = f;
                                            msg(f == 0 ? "§bfps otomatik ölçülecek"
                                                    : "§bçıkış fps: " + f); return 1; })))
                        .then(Commands.literal("ffmpeg")
                                .then(Commands.argument("path", StringArgumentType.greedyString())
                                        .executes(c -> {
                                            VideoRecorder.INSTANCE.ffmpeg =
                                                    StringArgumentType.getString(c, "path");
                                            msg("§bffmpeg: " + VideoRecorder.INSTANCE.ffmpeg);
                                            return 1; })))));
    }

    static void msg(String s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal(s), false);
        EFMocap.LOG.info(s);
    }

    /** Mod-bus client setup: keybinds and the invisible camera renderer. */
    @Mod.EventBusSubscriber(modid = EFMocap.MOD_ID, value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        public static net.minecraft.client.KeyMapping RECORD, PLAY, CLEAR,
                CAM_ADD, CINEMATIC, CAM_STOP, VIDEO;

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            RECORD = key("record", GLFW.GLFW_KEY_K);
            PLAY = key("play", GLFW.GLFW_KEY_N);
            CLEAR = key("clear", GLFW.GLFW_KEY_J);
            CAM_ADD = key("cam_add", GLFW.GLFW_KEY_C);
            CINEMATIC = key("cinematic", GLFW.GLFW_KEY_V);
            CAM_STOP = key("cam_stop", GLFW.GLFW_KEY_B);
            VIDEO = key("video", GLFW.GLFW_KEY_P);
            for (var k : new net.minecraft.client.KeyMapping[]
                    {RECORD, PLAY, CLEAR, CAM_ADD, CINEMATIC, CAM_STOP, VIDEO}) {
                event.register(k);
            }
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            // The camera must never be drawn.
            event.registerEntityRenderer(ModEntities.CAMERA.get(), NoopRenderer::new);
        }

        private static net.minecraft.client.KeyMapping key(String id, int code) {
            return new net.minecraft.client.KeyMapping(
                    "key.efmocap." + id, KeyConflictContext.IN_GAME,
                    com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, code,
                    "key.categories.efmocap");
        }
    }
}
