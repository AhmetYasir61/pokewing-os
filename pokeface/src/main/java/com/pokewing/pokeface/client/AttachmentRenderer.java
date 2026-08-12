package com.pokewing.pokeface.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pokewing.pokeface.face.Attachment;
import com.pokewing.pokeface.face.FaceProfile;
import com.pokewing.pokeface.model.ModelLibrary;
import com.pokewing.pokeface.model.ObjModel;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the OBJ attachments for one anchor bone.
 *
 * <p>The caller is responsible for putting the pose stack on the bone, exactly
 * like {@link FaceRenderer} — that is what makes ears follow the head and a tail
 * follow the body through animation, including Epic Fight's.
 *
 * <p>Placement is in model pixels off the bone pivot, in the same space vanilla
 * model parts use (+Y down, -Z forward), so a model exported from Blockbench at
 * Minecraft's own scale needs no conversion. A glowing attachment is drawn with
 * {@link RenderType#eyes}, which is fullbright and is what shader packs read as
 * emissive.
 */
public final class AttachmentRenderer {

    private static final float PX = 1.0F / 16.0F;

    private AttachmentRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, FaceProfile profile,
                              Attachment.Anchor anchor, int light) {
        for (Attachment attachment : profile.normalisedAttachments()) {
            if (!attachment.visible || attachment.anchor != anchor || attachment.model.isEmpty()) {
                continue;
            }
            ObjModel model = ModelLibrary.model(attachment.model);
            ResourceLocation texture = ModelLibrary.texture(attachment.model);
            if (model == null || texture == null) {
                // The model file is missing on this client; the rest of the face
                // keeps rendering rather than the whole player breaking.
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(attachment.offsetX * PX, attachment.offsetY * PX, attachment.offsetZ * PX);
            if (attachment.rotateY != 0.0F) {
                poseStack.mulPose(Axis.YP.rotationDegrees(attachment.rotateY));
            }
            if (attachment.rotateX != 0.0F) {
                poseStack.mulPose(Axis.XP.rotationDegrees(attachment.rotateX));
            }
            if (attachment.rotateZ != 0.0F) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(attachment.rotateZ));
            }
            poseStack.scale(attachment.scale, attachment.scale, attachment.scale);

            VertexConsumer buffer = attachment.glow
                    ? buffers.getBuffer(RenderType.eyes(texture))
                    : buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
            int packedLight = attachment.glow ? LightTexture.FULL_BRIGHT : light;
            model.render(poseStack, buffer, packedLight, attachment.tint);
            poseStack.popPose();
        }
    }
}
