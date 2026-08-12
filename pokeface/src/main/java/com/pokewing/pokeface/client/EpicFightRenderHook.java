package com.pokewing.pokeface.client;

import com.pokewing.pokeface.PokeFaceConfig;
import com.pokewing.pokeface.compat.EpicFightCompat;
import com.pokewing.pokeface.face.FaceState;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Epic Fight render path.
 *
 * <p>When Epic Fight is installed it swaps in its own patched player renderer,
 * whose layer list our vanilla {@link FaceOverlayLayer} is never added to. Rather
 * than mixin into that renderer — which would be the thing most likely to break
 * Epic Fight, RealCamera or a Connector setup — the face is drawn from Forge's
 * public {@link RenderPlayerEvent.Post} hook, after Epic Fight has finished its
 * own drawing. The pose stack is back at the entity origin there, which is
 * exactly what {@link FaceRenderer} expects.
 */
public final class EpicFightRenderHook {

    private static final Set<UUID> HANDLED = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        HANDLED.remove(event.getEntity().getUUID());
        if (!PokeFaceConfig.enabled() || !EpicFightCompat.isLoaded()) {
            return;
        }
        Player player = event.getEntity();
        if (player.isInvisible() || !(player instanceof AbstractClientPlayer)) {
            return;
        }
        FaceState state = PokeFaceClient.faceFor(player);
        if (state == null) {
            return;
        }
        HANDLED.add(player.getUUID());
        FaceRenderer.render(event.getPoseStack(), event.getMultiBufferSource(), player, state,
                PokeFaceClient.profileFor(player), event.getPartialTick(), event.getPackedLight());
    }

    /** Prevents the vanilla layer from drawing a second face over the EF one. */
    public static boolean handledThisFrame(Player player) {
        return HANDLED.contains(player.getUUID());
    }
}
