package com.pokewing.pokeface.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pokewing.pokeface.PokeFaceConfig;
import com.pokewing.pokeface.face.FaceProfile;
import com.pokewing.pokeface.face.FaceState;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * Vanilla render path: a normal {@link RenderLayer} on the player renderer.
 * Epic Fight draws players through its own patched renderer, which is covered by
 * {@link EpicFightRenderHook} instead — both end up calling {@link FaceRenderer}.
 */
public final class FaceOverlayLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public FaceOverlayLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!PokeFaceConfig.enabled() || player.isInvisible() || EpicFightRenderHook.handledThisFrame(player)) {
            return;
        }
        FaceState state = PokeFaceClient.faceFor(player);
        if (state == null) {
            return;
        }
        FaceProfile profile = PokeFaceClient.profileFor(player);
        FaceRenderer.render(poseStack, buffers, player, state, profile, partialTick, light);
    }
}
