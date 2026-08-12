package com.pokewing.pokeface.net;

import com.pokewing.pokeface.PokeFace;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Broadcasts each player's face state and profile so everyone sees the same face. */
public final class PokeFaceNetwork {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(PokeFace.MOD_ID, "main"))
            .networkProtocolVersion(() -> VERSION)
            .clientAcceptedVersions(VERSION::equals)
            .serverAcceptedVersions(VERSION::equals)
            .simpleChannel();

    private PokeFaceNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, FaceSyncPacket.class,
                FaceSyncPacket::encode, FaceSyncPacket::decode, FaceSyncPacket::handle);
        CHANNEL.registerMessage(id, FaceProfilePacket.class,
                FaceProfilePacket::encode, FaceProfilePacket::decode, FaceProfilePacket::handle);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void broadcastTracking(net.minecraft.world.entity.Entity source, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> source), packet);
    }
}
