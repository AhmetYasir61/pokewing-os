package com.pokewing.pokeface.net;

import com.pokewing.pokeface.face.FaceProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** A player's appearance settings, sent on join and whenever the menu is saved. */
public record FaceProfilePacket(UUID player, FaceProfile profile) {

    public static void encode(FaceProfilePacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.player);
        msg.profile.write(buf);
    }

    public static FaceProfilePacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        return new FaceProfilePacket(id, FaceProfile.read(buf));
    }

    public static void handle(FaceProfilePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                PokeFaceNetwork.broadcastTracking(context.getSender(),
                        new FaceProfilePacket(context.getSender().getUUID(), msg.profile));
            } else {
                com.pokewing.pokeface.client.ClientFaceStore.putProfile(msg.player, msg.profile);
            }
        });
        context.setPacketHandled(true);
    }
}
