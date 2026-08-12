package com.pokewing.pokeface.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pokewing.pokeface.PokeFace;
import com.pokewing.pokeface.face.FaceProfile;
import com.pokewing.pokeface.face.FaceState;
import com.pokewing.pokeface.face.FaceStyle;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Draws the eye and mouth features on the front plane of the player's head.
 *
 * <p>Everything is plain textured quads emitted into the normal entity buffer —
 * no mixin, no model surgery, no renderer replacement. That is what lets the
 * same code path be reused for both the vanilla player renderer and Epic Fight's
 * patched renderer, and it is why installing this cannot break either one.
 */
public final class FaceRenderer {

    /**
     * Feature atlas: 8 expression columns x 5 style rows, each tile 16x16 px,
     * split into an eye half (top 8px) and a mouth half (bottom 8px).
     */
    public static final ResourceLocation ATLAS =
            new ResourceLocation(PokeFace.MOD_ID, "textures/face/expressions.png");

    private static final int COLUMNS = 8;
    private static final int ROWS = 5;
    /**
     * Front plane of the 8x8x8 head cube. The base nudge avoids z-fighting with
     * the skin; the configurable extra offset clears head layers drawn by other
     * mods (Armourer's Workshop skins, hats, masks).
     */
    private static final float FRONT_Z = -0.2510F;
    /** Head pivot height in blocks for a standing player. */
    private static final float HEAD_PIVOT_Y = 1.5F;

    private FaceRenderer() {
    }

    /**
     * Renders the face assuming {@code poseStack} is at the player's feet with the
     * world's axes (the state both the vanilla layer and the Epic Fight post-render
     * hook hand us).
     */
    public static void render(PoseStack poseStack, MultiBufferSource buffers, Player player,
                              FaceState face, FaceProfile profile, float partialTick, int light) {
        float headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float pitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());

        poseStack.pushPose();
        poseStack.translate(0.0D, player.isCrouching() ? HEAD_PIVOT_Y - 0.15F : HEAD_PIVOT_Y, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-(headYaw - bodyYaw) - bodyYaw + 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(ATLAS));
        int col = face.expression.ordinal() % COLUMNS;
        int row = profile.styleEnum().row() % ROWS;

        drawEyes(poseStack, buffer, face, profile, col, row, light);
        drawMouth(poseStack, buffer, face, profile, col, row, light);

        poseStack.popPose();
    }

    private static void drawEyes(PoseStack poseStack, VertexConsumer buffer, FaceState face,
                                 FaceProfile profile, int col, int row, int light) {
        float px = 1.0F / 16.0F;
        float half = 0.5F * profile.eyeScale * 2.0F * px;
        float spacing = profile.eyeSpacing * px * 0.5F;
        float baseY = (1.0F + profile.eyeOffsetY) * px;
        float baseX = profile.eyeOffsetX * px;

        // Blink squashes the quad vertically around its centre, which reads the
        // same way at any scale and needs no extra atlas frames.
        float openL = 1.0F - Mth.clamp(face.blinkLeft, 0.0F, 1.0F);
        float openR = 1.0F - Mth.clamp(face.blinkRight, 0.0F, 1.0F);
        float gazeX = face.gazeX * px * 0.6F;
        float gazeY = face.gazeY * px * 0.6F;

        float u0 = col / (float) COLUMNS;
        float u1 = (col + 0.5F) / COLUMNS;
        float v0 = row / (float) ROWS;
        float v1 = (row + 0.5F) / ROWS;

        quad(poseStack, buffer, baseX - spacing - half + gazeX, baseX - spacing + half + gazeX,
                baseY - half * openL + gazeY, baseY + half * openL + gazeY,
                u0, u1, v0, v1, profile.eyeColor, light);
        // The right eye mirrors the same sprite so one drawn eye covers both.
        quad(poseStack, buffer, baseX + spacing + half + gazeX, baseX + spacing - half + gazeX,
                baseY - half * openR + gazeY, baseY + half * openR + gazeY,
                u0, u1, v0, v1, profile.eyeColor, light);

        if (Math.abs(face.brow) > 0.05F) {
            float browY = baseY + half + Math.max(0.0F, face.brow) * px * 0.8F;
            float tilt = face.brow < 0.0F ? -face.brow * px * 0.5F : 0.0F;
            quad(poseStack, buffer, baseX - spacing - half, baseX - spacing + half,
                    browY - px * 0.25F + tilt, browY + px * 0.25F,
                    u1, (col + 1.0F) / COLUMNS, v0, v1, profile.lineColor, light);
            quad(poseStack, buffer, baseX + spacing + half, baseX + spacing - half,
                    browY - px * 0.25F + tilt, browY + px * 0.25F,
                    u1, (col + 1.0F) / COLUMNS, v0, v1, profile.lineColor, light);
        }
    }

    private static void drawMouth(PoseStack poseStack, VertexConsumer buffer, FaceState face,
                                  FaceProfile profile, int col, int row, int light) {
        float px = 1.0F / 16.0F;
        float width = 1.6F * profile.mouthScale * px;
        float open = Mth.clamp(face.mouthOpen, 0.0F, 1.0F);
        float height = (0.25F + open * 1.5F) * profile.mouthScale * px;
        float cx = profile.mouthOffsetX * px;
        float cy = (-2.0F + profile.mouthOffsetY + face.mouthSmile * 0.4F) * px;

        float u0 = col / (float) COLUMNS;
        float u1 = (col + 0.5F) / COLUMNS;
        float v0 = (row + 0.5F) / ROWS;
        float v1 = (row + 1.0F) / ROWS;

        // Inner mouth first, then the lip line on top of it.
        int inner = open > 0.05F ? profile.mouthInnerColor : profile.lineColor;
        quad(poseStack, buffer, cx - width, cx + width, cy - height, cy + height,
                u0, u1, v0, v1, inner, light);

        if (open > 0.35F) {
            float teeth = height * 0.28F;
            quad(poseStack, buffer, cx - width, cx + width, cy + height - teeth, cy + height,
                    u1, (col + 1.0F) / COLUMNS, v0, v1, profile.teethColor, light);
        }
    }

    private static void quad(PoseStack poseStack, VertexConsumer buffer,
                             float x0, float x1, float y0, float y1,
                             float u0, float u1, float v0, float v1, int argb, int light) {
        var pose = poseStack.last();
        float a = (argb >>> 24 & 0xFF) / 255.0F;
        float r = (argb >>> 16 & 0xFF) / 255.0F;
        float g = (argb >>> 8 & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        vertex(buffer, pose, x0, y0, u0, v1, r, g, b, a, light);
        vertex(buffer, pose, x1, y0, u1, v1, r, g, b, a, light);
        vertex(buffer, pose, x1, y1, u1, v0, r, g, b, a, light);
        vertex(buffer, pose, x0, y1, u0, v0, r, g, b, a, light);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y,
                               float u, float v, float r, float g, float b, float a, int light) {
        buffer.vertex(pose.pose(), x, y, FRONT_Z - com.pokewing.pokeface.PokeFaceConfig.frontOffset())
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), 0.0F, 0.0F, -1.0F)
                .endVertex();
    }

    /** Used by the customisation screen's preview. */
    public static FaceStyle[] styles() {
        return FaceStyle.values();
    }
}
