package com.pokewing.pokeface.net;

import com.pokewing.pokeface.face.Expression;
import com.pokewing.pokeface.face.FaceState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** One player's current blendshapes, quantised to 8 bytes plus the tile id. */
public record FaceSyncPacket(UUID player, long packed, Expression expression) {

    public static void encode(FaceSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.player);
        buf.writeLong(msg.packed);
        buf.writeByte(msg.expression.ordinal());
    }

    public static FaceSyncPacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        long packed = buf.readLong();
        int tile = buf.readByte() & 0xFF;
        Expression[] values = Expression.values();
        return new FaceSyncPacket(id, packed, values[tile % values.length]);
    }

    public static void handle(FaceSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                // Server side: retag with the real sender and fan out to viewers.
                FaceSyncPacket authenticated =
                        new FaceSyncPacket(context.getSender().getUUID(), msg.packed, msg.expression);
                PokeFaceNetwork.broadcastTracking(context.getSender(), authenticated);
            } else {
                com.pokewing.pokeface.client.ClientFaceStore.put(msg.player,
                        FaceState.unpack(msg.packed, msg.expression));
            }
        });
        context.setPacketHandled(true);
    }
}
