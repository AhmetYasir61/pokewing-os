package com.pokewing.pokeface;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/** Client config; everything here is also editable from the in-game menu. */
public final class PokeFaceConfig {

    public static final ForgeConfigSpec SPEC;
    private static final PokeFaceConfig INSTANCE;

    public final ForgeConfigSpec.BooleanValue enabled;
    public final ForgeConfigSpec.BooleanValue trackerEnabled;
    public final ForgeConfigSpec.ConfigValue<String> trackerAddress;
    public final ForgeConfigSpec.IntValue trackerPort;
    public final ForgeConfigSpec.IntValue trackerTimeoutMillis;
    public final ForgeConfigSpec.DoubleValue smoothing;
    public final ForgeConfigSpec.DoubleValue threatRadius;
    public final ForgeConfigSpec.BooleanValue showOwnFaceInFirstPerson;
    public final ForgeConfigSpec.DoubleValue frontOffset;
    public final ForgeConfigSpec.BooleanValue combatReactions;
    public final ForgeConfigSpec.BooleanValue voiceChatMouth;

    private PokeFaceConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("PokeFace - facial reactions").push("general");
        this.enabled = builder
                .comment("Master switch for the face overlay.")
                .define("enabled", true);
        this.smoothing = builder
                .comment("Blend speed toward the target face, per tick. 1.0 = instant.")
                .defineInRange("smoothing", 0.35D, 0.01D, 1.0D);
        this.showOwnFaceInFirstPerson = builder
                .comment("Render your own face overlay in third person / mirrors.")
                .define("showOwnFaceInFirstPerson", true);
        this.frontOffset = builder
                .comment("Extra distance the face is pushed out from the head, in blocks.",
                        "Raise this if another mod draws a layer over the head (Armourer's",
                        "Workshop custom skins, hats, masks) and clips the face.")
                .defineInRange("frontOffset", 0.001D, 0.0D, 0.25D);
        builder.pop();

        builder.comment("Webcam tracking via an OpenSeeFace-compatible tracker").push("tracker");
        this.trackerEnabled = builder
                .comment("Listen for face tracking packets. When no data arrives the mod",
                        "automatically falls back to combat reactions and the idle animator.")
                .define("enabled", true);
        this.trackerAddress = builder
                .comment("Address to bind the UDP listener to.")
                .define("address", "127.0.0.1");
        this.trackerPort = builder
                .comment("UDP port. OpenSeeFace/VSeeFace default is 11573.")
                .defineInRange("port", 11573, 1024, 65535);
        this.trackerTimeoutMillis = builder
                .comment("How long without packets before the camera counts as lost.")
                .defineInRange("timeoutMillis", 600, 100, 10000);
        builder.pop();

        builder.comment("Fallback drivers").push("reactions");
        this.combatReactions = builder
                .comment("React to damage, attacks and Epic Fight battle mode.")
                .define("combatReactions", true);
        this.threatRadius = builder
                .comment("Radius scanned for hostile mobs when picking the idle mood.")
                .defineInRange("threatRadius", 12.0D, 0.0D, 64.0D);
        this.voiceChatMouth = builder
                .comment("Drive the mouth from the Simple Voice Chat microphone.")
                .define("voiceChatMouth", true);
        builder.pop();
    }

    static {
        Pair<PokeFaceConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(PokeFaceConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    public static boolean enabled() {
        return INSTANCE.enabled.get();
    }

    public static boolean trackerEnabled() {
        return INSTANCE.trackerEnabled.get();
    }

    public static String trackerAddress() {
        return INSTANCE.trackerAddress.get();
    }

    public static int trackerPort() {
        return INSTANCE.trackerPort.get();
    }

    public static int trackerTimeoutMillis() {
        return INSTANCE.trackerTimeoutMillis.get();
    }

    public static double smoothing() {
        return INSTANCE.smoothing.get();
    }

    public static double threatRadius() {
        return INSTANCE.threatRadius.get();
    }

    public static boolean combatReactions() {
        return INSTANCE.combatReactions.get();
    }

    public static boolean voiceChatMouth() {
        return INSTANCE.voiceChatMouth.get();
    }

    public static float frontOffset() {
        return INSTANCE.frontOffset.get().floatValue();
    }

    public static boolean showOwnFace() {
        return INSTANCE.showOwnFaceInFirstPerson.get();
    }

    public static void setTrackerEnabled(boolean value) {
        INSTANCE.trackerEnabled.set(value);
        INSTANCE.trackerEnabled.save();
    }
}
