package com.pokewing.efmocap;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Client wiring: capture the local player each tick while recording, advance
 * active replays each tick, and expose record / stage-scene / clear via
 * keybinds and the {@code /efmocap} client command.
 */
@Mod.EventBusSubscriber(modid = EFMocap.MOD_ID, value = Dist.CLIENT)
public final class ClientSystems {
    private ClientSystems() {}

    private static boolean takesLoaded = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();

        if (!takesLoaded && mc.level != null) {
            int n = TakeLibrary.INSTANCE.loadAll();
            takesLoaded = true;
            if (n > 0) msg("§7[efmocap] " + n + " kayıtlı çekim yüklendi");
        }

        if (Keys.RECORD != null) {
            while (Keys.RECORD.consumeClick()) toggleRecord();
        }
        if (Keys.PLAY != null) {
            while (Keys.PLAY.consumeClick()) stageScene();
        }
        if (Keys.CLEAR != null) {
            while (Keys.CLEAR.consumeClick()) {
                ReplayDirector.INSTANCE.clearAll();
                msg("§e[efmocap] klonlar temizlendi");
            }
        }

        if (mc.isPaused()) return;

        // Capture the performer.
        if (MocapRecorder.INSTANCE.isRecording()) {
            LocalPlayer p = mc.player;
            if (p != null) {
                EpicFightBridge.AnimSample anim = EFMocap.isEpicFightLoaded()
                        ? EpicFightBridge.sampleAnimation(EpicFightBridge.getPatch(p), 1.0f)
                        : new EpicFightBridge.AnimSample();
                MocapRecorder.INSTANCE.capture(p, anim);
            }
        }

        // Advance clones.
        ReplayDirector.INSTANCE.tick();
    }

    private static void toggleRecord() {
        if (MocapRecorder.INSTANCE.isRecording()) {
            MocapRecording rec = MocapRecorder.INSTANCE.stop();
            if (rec != null && !rec.isEmpty()) {
                TakeLibrary.INSTANCE.add(rec);
                msg("§a[efmocap] '" + rec.name + "' kaydedildi (" + rec.length()
                        + " kare). Sahneyi oynat: N");
            } else {
                msg("§c[efmocap] boş kayıt");
            }
        } else {
            MocapRecorder.INSTANCE.start(TakeLibrary.INSTANCE.nextName());
            msg("§a[efmocap] kayıt başladı. Dövüş! Durdurmak: K");
        }
    }

    /** Stage all saved takes together as one scene. */
    private static void stageScene() {
        int n = ReplayDirector.INSTANCE.playScene(true);
        msg(n > 0 ? "§a[efmocap] sahne oynuyor — " + n + " oyuncu"
                  : "§c[efmocap] kayıtlı çekim yok (K ile kaydet)");
    }

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
                    ReplayDirector.INSTANCE.restart();
                    msg("§asahne baştan"); return 1; }))
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
                    ReplayDirector.INSTANCE.clearAll();
                    msg("§eklonlar temizlendi"); return 1; })));
    }

    static void msg(String s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal(s), false);
        EFMocap.LOG.info(s);
    }

    /** Keybinds (registered on the MOD event bus). */
    @Mod.EventBusSubscriber(modid = EFMocap.MOD_ID, value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        public static net.minecraft.client.KeyMapping RECORD;
        public static net.minecraft.client.KeyMapping PLAY;
        public static net.minecraft.client.KeyMapping CLEAR;

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            RECORD = key("record", GLFW.GLFW_KEY_K);
            PLAY = key("play", GLFW.GLFW_KEY_N);
            CLEAR = key("clear", GLFW.GLFW_KEY_J);
            event.register(RECORD);
            event.register(PLAY);
            event.register(CLEAR);
        }

        private static net.minecraft.client.KeyMapping key(String id, int code) {
            return new net.minecraft.client.KeyMapping(
                    "key.efmocap." + id, KeyConflictContext.IN_GAME,
                    com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, code,
                    "key.categories.efmocap");
        }
    }
}
