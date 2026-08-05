package com.pokewing.efbbs;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Client-side live recorder. While recording, every client tick it samples the
 * fully-blended Epic Fight pose of each living entity near the player (including
 * other players in multiplayer) plus their world position, and feeds it to
 * {@link PoseRecorder}. On stop, one BBS animation file is written per entity.
 *
 * <p>Trigger it with the keybind (default: K) or the client command
 * {@code /efbbs rec start|stop}. Everything runs on the client, so it works in
 * singleplayer and on servers alike -- no server-side mod required.</p>
 */
@Mod.EventBusSubscriber(modid = EpicFightBBSBridge.MOD_ID, value = Dist.CLIENT)
public final class ClientRecording {
    private ClientRecording() {}

    private static double originX, originY, originZ;

    static void startRecording() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Vec3 p = mc.player.position();
        originX = p.x; originY = p.y; originZ = p.z;
        PoseRecorder.INSTANCE.start();
        msg("§a[efbbs] Kayıt başladı. Dövüş! Durdurmak için tekrar bas / '/efbbs rec stop'.");
    }

    static void stopRecording() {
        if (!PoseRecorder.INSTANCE.isRecording()) return;
        RetargetConfig cfg = RetargetConfig.loadOrCreate();
        List<Path> files = PoseRecorder.INSTANCE.stop(cfg);
        msg("§a[efbbs] Kayıt durdu. " + files.size() + " animasyon yazıldı -> "
                + AnimationExporter.outputDir());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Keybind toggle.
        if (Keys.TOGGLE_RECORD != null) {
            while (Keys.TOGGLE_RECORD.consumeClick()) {
                if (PoseRecorder.INSTANCE.isRecording()) stopRecording(); else startRecording();
            }
        }

        if (!PoseRecorder.INSTANCE.isRecording()) return;
        if (!EpicFightBBSBridge.isEpicFightLoaded()) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) return;

        RetargetConfig cfg = RetargetConfig.loadOrCreate();
        double r = cfg.recordRadius;
        Vec3 pp = mc.player.position();
        AABB box = new AABB(pp.x - r, pp.y - r, pp.z - r, pp.x + r, pp.y + r, pp.z + r);

        float time = PoseRecorder.INSTANCE.frameCount() / 20.0f; // client runs at 20 tps
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box);
        if (!entities.contains(mc.player)) entities.add(mc.player);

        for (LivingEntity e : entities) {
            Map<String, float[]> pose = EpicFightAccess.sampleLivePose(e, 1.0f);
            if (pose == null || pose.isEmpty()) continue; // no Epic Fight patch -> skip
            Vec3 ep = e.position();
            PoseRecorder.INSTANCE.recordFrame(
                    e.getUUID().toString(),
                    sanitize(e.getName().getString()),
                    pose, time,
                    (float) (ep.x - originX),
                    (float) (ep.y - originY),
                    (float) (ep.z - originZ),
                    e.yBodyRot);
        }
        PoseRecorder.INSTANCE.markTick();
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("efbbs").then(Commands.literal("rec")
                .then(Commands.literal("start").executes(ctx -> { startRecording(); return 1; }))
                .then(Commands.literal("stop").executes(ctx -> { stopRecording(); return 1; }))));
    }

    private static void msg(String s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal(s), false);
        EpicFightBBSBridge.LOG.info(s);
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /** Key mappings are registered on the MOD event bus. */
    @Mod.EventBusSubscriber(modid = EpicFightBBSBridge.MOD_ID, value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        public static net.minecraft.client.KeyMapping TOGGLE_RECORD;

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            TOGGLE_RECORD = new net.minecraft.client.KeyMapping(
                    "key.efbbs.toggle_record",
                    KeyConflictContext.IN_GAME,
                    com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_K,
                    "key.categories.efbbs");
            event.register(TOGGLE_RECORD);
        }
    }
}
