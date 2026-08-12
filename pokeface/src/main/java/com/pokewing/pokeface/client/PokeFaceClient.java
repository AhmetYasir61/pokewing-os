package com.pokewing.pokeface.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pokewing.pokeface.PokeFace;
import com.pokewing.pokeface.PokeFaceConfig;
import com.pokewing.pokeface.compat.VoiceChatCompat;
import com.pokewing.pokeface.face.CombatReactions;
import com.pokewing.pokeface.face.Expression;
import com.pokewing.pokeface.face.FaceDirector;
import com.pokewing.pokeface.face.FaceProfile;
import com.pokewing.pokeface.face.FaceState;
import com.pokewing.pokeface.face.ReactionTrigger;
import com.pokewing.pokeface.net.FaceProfilePacket;
import com.pokewing.pokeface.net.FaceSyncPacket;
import com.pokewing.pokeface.net.PokeFaceNetwork;
import com.pokewing.pokeface.tracker.OpenSeeFaceTracker;
import com.pokewing.pokeface.tracker.TrackerSource;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.Random;
import java.util.UUID;

/** Client entry point: keybinds, ticking, tracker lifecycle and profile storage. */
public final class PokeFaceClient {

    public static final KeyMapping KEY_WHEEL = new KeyMapping(
            "key.pokeface.wheel", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, "key.categories.pokeface");
    public static final KeyMapping KEY_MENU = new KeyMapping(
            "key.pokeface.menu", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8, "key.categories.pokeface");
    public static final KeyMapping KEY_TOGGLE_TRACKER = new KeyMapping(
            "key.pokeface.toggle_tracker", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN, "key.categories.pokeface");

    private static final FaceDirector DIRECTOR = new FaceDirector(new Random());
    private static final FaceProfile DEFAULT_PROFILE = new FaceProfile();
    private static FaceProfile localProfile = new FaceProfile();
    private static TrackerSource tracker;
    private static int syncCooldown;

    private PokeFaceClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(PokeFaceClient::clientSetup);
        modBus.addListener(PokeFaceClient::registerKeys);
        modBus.addListener(PokeFaceClient::addLayers);
        MinecraftForge.EVENT_BUS.register(new PokeFaceClient.Ticker());
        MinecraftForge.EVENT_BUS.register(new EpicFightRenderHook());
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            localProfile = FaceProfile.load(profilePath());
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (mc, parent) -> new FaceCustomizeScreen(parent)));
            restartTracker();
        });
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KEY_WHEEL);
        event.register(KEY_MENU);
        event.register(KEY_TOGGLE_TRACKER);
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            EntityRenderer<?> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                addLayer(playerRenderer);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addLayer(PlayerRenderer renderer) {
        ((LivingEntityRenderer) renderer).addLayer(new FaceOverlayLayer(renderer));
    }

    public static Path profilePath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve(PokeFace.MOD_ID).resolve("face-profile.json");
    }

    public static FaceDirector director() {
        return DIRECTOR;
    }

    public static FaceProfile localProfile() {
        return localProfile;
    }

    /** Persists the profile and tells everyone who can see us about it. */
    public static void saveLocalProfile() {
        localProfile.save(profilePath());
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            PokeFaceNetwork.sendToServer(new FaceProfilePacket(player.getUUID(), localProfile));
        }
        restartTracker();
    }

    public static boolean isLocalPlayer(Player player) {
        LocalPlayer local = Minecraft.getInstance().player;
        return local != null && local.getUUID().equals(player.getUUID());
    }

    public static FaceState faceFor(Player player) {
        if (isLocalPlayer(player)) {
            return PokeFaceConfig.showOwnFace() ? DIRECTOR.current() : null;
        }
        FaceState state = ClientFaceStore.get(player.getUUID());
        if (state != null && PokeFaceConfig.voiceChatMouth()) {
            // Remote players sync at 10 Hz, so lip-sync is applied locally per
            // frame instead of riding the network packet.
            float voice = VoiceChatCompat.mouthOpenFor(player.getUUID(),
                    player.level().getGameTime(), 0.0F);
            if (voice > 0.02F) {
                state.mouthOpen = Math.max(state.mouthOpen, voice);
            }
        }
        return state;
    }

    public static FaceProfile profileFor(Player player) {
        if (isLocalPlayer(player)) {
            return localProfile;
        }
        FaceProfile p = ClientFaceStore.profile(player.getUUID());
        return p != null ? p : DEFAULT_PROFILE;
    }

    public static boolean trackerLive() {
        return DIRECTOR.trackerLive();
    }

    public static FaceDirector.Source lastSource() {
        return DIRECTOR.lastSource();
    }

    /** (Re)opens the tracker socket after a config or profile change. */
    public static void restartTracker() {
        if (tracker != null) {
            tracker.close();
            tracker = null;
            DIRECTOR.setTracker(null);
        }
        if (!PokeFaceConfig.trackerEnabled() || !localProfile.useTracker) {
            CombatReactions.logOnce("face tracking disabled - using reactions and idle animation");
            return;
        }
        tracker = new OpenSeeFaceTracker(PokeFaceConfig.trackerAddress(), PokeFaceConfig.trackerPort(),
                PokeFaceConfig.trackerTimeoutMillis());
        tracker.start();
        DIRECTOR.setTracker(tracker);
    }

    public static void pushManualReaction(Expression expression) {
        DIRECTOR.push(new ReactionTrigger(expression, 1.0F, 60, CombatReactions.PRIORITY_MANUAL));
    }

    /** Per-tick driver: mood bias, voice-chat mouth, blending and network sync. */
    public static final class Ticker {

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            while (KEY_WHEEL.consumeClick()) {
                mc.setScreen(new ReactionWheelScreen());
            }
            while (KEY_MENU.consumeClick()) {
                mc.setScreen(new FaceCustomizeScreen(null));
            }
            while (KEY_TOGGLE_TRACKER.consumeClick()) {
                PokeFaceConfig.setTrackerEnabled(!PokeFaceConfig.trackerEnabled());
                restartTracker();
            }

            LocalPlayer player = mc.player;
            if (player == null || !PokeFaceConfig.enabled()) {
                return;
            }
            DIRECTOR.setMoodBias(PokeFaceConfig.combatReactions()
                    ? CombatReactions.moodFor(player) : Expression.NEUTRAL);
            FaceState state = DIRECTOR.tick();
            applyVoiceChatMouth(state);

            if (--syncCooldown <= 0) {
                syncCooldown = 2; // 10 Hz is plenty for a face at render distance.
                PokeFaceNetwork.sendToServer(
                        new FaceSyncPacket(player.getUUID(), state.pack(), state.expression));
            }
        }

        /**
         * Voice wins over whatever the mouth was doing, because a talking player
         * whose mouth is shut looks broken in a way a wrong brow never does.
         */
        private void applyVoiceChatMouth(FaceState state) {
            if (!PokeFaceConfig.voiceChatMouth() || !localProfile.voiceChatMouth
                    || !VoiceChatCompat.isLoaded()) {
                return;
            }
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            float voice = VoiceChatCompat.mouthOpenFor(player.getUUID(),
                    player.level().getGameTime(), 0.0F);
            if (voice > 0.02F) {
                state.mouthOpen = Math.max(state.mouthOpen, voice);
            }
        }

        @SubscribeEvent
        public void onLoggedOut(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            ClientFaceStore.clear();
        }

        @SubscribeEvent
        public void onLoggedIn(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
            UUID id = event.getPlayer() != null ? event.getPlayer().getUUID() : null;
            if (id != null) {
                PokeFaceNetwork.sendToServer(new FaceProfilePacket(id, localProfile));
            }
        }
    }

    /** Marker used by the client-only classes to assert their dist. */
    public static Dist dist() {
        return Dist.CLIENT;
    }
}
