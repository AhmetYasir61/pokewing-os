package com.pokewing.efmocap;

import com.mojang.brigadier.CommandDispatcher;
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
 * Phase 1 client wiring: capture the local player each tick while recording,
 * advance active replays each tick, and expose record / play / clear via a
 * keybind and the {@code /efmocap} client command.
 */
@Mod.EventBusSubscriber(modid = EFMocap.MOD_ID, value = Dist.CLIENT)
public final class ClientSystems {
    private ClientSystems() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (Keys.RECORD != null) {
            while (Keys.RECORD.consumeClick()) toggleRecord();
        }
        if (Keys.PLAY != null) {
            while (Keys.PLAY.consumeClick()) playLast();
        }
        if (Keys.CLEAR != null) {
            while (Keys.CLEAR.consumeClick()) { ReplayDirector.INSTANCE.clearAll(); msg("§e[efmocap] klonlar temizlendi"); }
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;

        // Capture the performer.
        if (MocapRecorder.INSTANCE.isRecording()) {
            LocalPlayer p = mc.player;
            if (p != null) {
                EpicFightBridge.AnimSample anim = EFMocap.isEpicFightLoaded()
                        ? EpicFightBridge.sampleAnimation(EpicFightBridge.getPatch(p), 1.0f)
                        : new EpicFightBridge.AnimSample();
                MocapRecorder.INSTANCE.capture(p.getX(), p.getY(), p.getZ(),
                        p.getYRot(), p.yBodyRot, p.getXRot(), anim);
            }
        }

        // Advance clones.
        ReplayDirector.INSTANCE.tick();
    }

    private static void toggleRecord() {
        if (MocapRecorder.INSTANCE.isRecording()) {
            MocapRecording rec = MocapRecorder.INSTANCE.stop();
            msg("§a[efmocap] kayıt durdu (" + (rec == null ? 0 : rec.length()) + " kare). Oynatmak: N");
        } else {
            MocapRecorder.INSTANCE.start("take_" + System.currentTimeMillis());
            msg("§a[efmocap] kayıt başladı. Dövüş! Durdurmak: K");
        }
    }

    private static void playLast() {
        MocapRecording rec = MocapRecorder.INSTANCE.last();
        if (rec == null || rec.isEmpty()) { msg("§c[efmocap] önce kayıt al (K)"); return; }
        boolean ok = ReplayDirector.INSTANCE.play(rec, true);
        msg(ok ? "§a[efmocap] klon oynuyor (toplam " + ReplayDirector.INSTANCE.activeCount() + ")"
               : "§c[efmocap] klon oluşturulamadı");
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("efmocap")
                .then(Commands.literal("rec")
                        .then(Commands.literal("start").executes(c -> { MocapRecorder.INSTANCE.start("take_" + System.currentTimeMillis()); msg("§akayıt başladı"); return 1; }))
                        .then(Commands.literal("stop").executes(c -> { MocapRecorder.INSTANCE.stop(); msg("§akayıt durdu"); return 1; })))
                .then(Commands.literal("play").executes(c -> { playLast(); return 1; }))
                .then(Commands.literal("clear").executes(c -> { ReplayDirector.INSTANCE.clearAll(); msg("§eklonlar temizlendi"); return 1; })));
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
