package com.pokewing.pokeface.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pokewing.pokeface.PokeFace;
import com.pokewing.pokeface.PokeFaceConfig;
import com.pokewing.pokeface.face.FaceProfile;
import com.pokewing.pokeface.face.FaceState;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws the eye and mouth features on the front plane of the head.
 *
 * <p>Everything is emitted in <b>head-local model space</b>: the caller is
 * responsible for putting the pose stack on the head bone first, which is what
 * makes the face follow head rotation and animation instead of floating at a
 * guessed world position. In that space the head pivot is the origin, one unit
 * is one block (so model pixels are divided by 16), <b>+Y points down</b> and
 * <b>-Z is the facing direction</b> — the same convention vanilla
 * {@code ModelPart} rendering uses.
 *
 * <p>The animated features are the <b>only</b> thing drawn on top of the player:
 * plain textured quads in the normal entity buffer, no mixin, no model surgery,
 * no renderer replacement. The hand-painted face patch is not a layer at all —
 * it is written into the skin texture itself by {@link SkinOverride}.
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
    private static final float PX = 1.0F / 16.0F;
    /** Front plane of the 8x8x8 head cube, in model pixels from the head pivot. */
    private static final float FACE_PLANE_PX = -4.0F;
    /** Eye row, in model pixels above the head pivot (Y is down, hence negative). */
    private static final float EYE_ROW_PX = -4.5F;
    /** Mouth row, in model pixels above the head pivot. */
    private static final float MOUTH_ROW_PX = -2.5F;
    /**
     * Centre of the solid white texel in the atlas' bottom-right corner, sampled
     * when drawing flat-coloured hand-painted pixels.
     */
    private static final float BLANK_U = 127.5F / 128.0F;
    private static final float BLANK_V = 79.5F / 80.0F;

    private FaceRenderer() {
    }

    /**
     * Renders the face. {@code poseStack} must already be transformed onto the
     * head bone — see {@link FaceOverlayLayer} for the vanilla path and
     * {@link EpicFightRenderHook} for the Epic Fight path.
     */
    public static void renderInHeadSpace(PoseStack poseStack, MultiBufferSource buffers,
                                         FaceState face, FaceProfile profile, int light) {
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(ATLAS));
        int col = face.expression.ordinal() % COLUMNS;
        int row = profile.styleEnum().row() % ROWS;

        drawEyes(poseStack, buffer, face, profile, col, row, light);
        drawMouth(poseStack, buffer, face, profile, col, row, light);
    }

    private static void drawEyes(PoseStack poseStack, VertexConsumer buffer, FaceState face,
                                 FaceProfile profile, int col, int row, int light) {
        float halfW = 1.0F * profile.eyeScale;                 // model pixels
        float halfH = 1.0F * profile.eyeScale;
        float spacing = profile.eyeSpacing * 0.5F;
        float centerY = EYE_ROW_PX + profile.eyeOffsetY;       // Y is down: - is up
        float centerX = profile.eyeOffsetX;

        // Blink squashes the eye vertically around its centre, which reads the
        // same way at any scale and needs no extra atlas frames.
        float openL = 1.0F - Mth.clamp(face.blinkLeft, 0.0F, 1.0F);
        float openR = 1.0F - Mth.clamp(face.blinkRight, 0.0F, 1.0F);
        // The gaze moves the iris inside the eye, not the eye itself, so the
        // white stays put the way a real eye does.
        float gazeX = face.gazeX * halfW * 0.5F;
        float gazeY = -face.gazeY * halfH * 0.5F;

        float u0 = col / (float) COLUMNS;
        float u1 = (col + 0.5F) / COLUMNS;
        float v0 = row / (float) ROWS;
        float v1 = (row + 0.5F) / ROWS;

        for (int side = 0; side < 2; side++) {
            boolean left = side == 0;
            // Convergence turns each eye toward the middle of the face, which is
            // what makes a pair read as looking at something instead of staring
            // in parallel. It is inward on both sides, hence the opposite signs.
            float converge = left ? profile.eyeConverge : -profile.eyeConverge;
            float perEyeX = left ? profile.eyeLeftOffsetX : profile.eyeRightOffsetX;
            float perEyeY = left ? profile.eyeLeftOffsetY : profile.eyeRightOffsetY;
            float sx = centerX + (left ? -spacing : spacing) + converge + perEyeX;
            float sy = centerY + perEyeY;
            float open = left ? openL : openR;
            float top = sy - halfH * open;
            float bottom = sy + halfH * open;
            int iris = left ? profile.eyeColor : profile.eyeColorRight;
            int sclera = left ? profile.scleraColor : profile.scleraColorRight;
            int pupil = left ? profile.pupilColor : profile.pupilColorRight;
            // Mirroring the right eye keeps the pair symmetric; without it the
            // same sprite is cloned and an asymmetric eye points the wrong way.
            boolean mirror = !left && profile.mirrorRightEye;

            if (profile.drawSclera) {
                quad(poseStack, buffer, sx - halfW, sx + halfW, top, bottom,
                        mirror ? u1 : u0, mirror ? u0 : u1, v0, v1, sclera, light);
            }

            // The moving part: hand-drawn art if there is any, otherwise an iris
            // (and pupil) sized as a fraction of the eye.
            float irisHalfW = halfW * profile.irisScale;
            float irisHalfH = halfH * profile.irisScale * open;
            // Kept inside the white, so the iris never slides off the eye.
            float slackX = Math.max(0.0F, halfW - irisHalfW);
            float slackY = Math.max(0.0F, halfH * open - irisHalfH);
            float ix = sx + Mth.clamp(gazeX, -slackX, slackX);
            float iy = sy + Mth.clamp(gazeY, -slackY, slackY);

            if (profile.hasEyeArt()) {
                drawEyeArt(poseStack, buffer, profile, ix - irisHalfW, ix + irisHalfW,
                        iy - irisHalfH, iy + irisHalfH, mirror, light);
            } else {
                quad(poseStack, buffer, ix - irisHalfW, ix + irisHalfW, iy - irisHalfH, iy + irisHalfH,
                        mirror ? u1 : u0, mirror ? u0 : u1, v0, v1, iris, light);
                if (profile.pupilScale > 0.05F) {
                    float pupilHalfW = irisHalfW * profile.pupilScale;
                    float pupilHalfH = irisHalfH * profile.pupilScale;
                    quad(poseStack, buffer, ix - pupilHalfW, ix + pupilHalfW,
                            iy - pupilHalfH, iy + pupilHalfH,
                            BLANK_U, BLANK_U, BLANK_V, BLANK_V, pupil, light);
                }
            }

            if (profile.drawBrows) {
                drawBrow(poseStack, buffer, profile, face, sx, sy - halfH, left, light);
            }
        }
    }

    /**
     * A brow drawn as a short stack of steps so it can tilt.
     *
     * <p>The tilt is driven by {@link FaceState#brow}: at full anger the inner
     * end (the one nearest the middle of the face) drops and the outer end
     * lifts, so the pair forms the inverted V that reads as a scowl. A positive
     * brow value raises the whole brow instead, for surprise.
     */
    private static void drawBrow(PoseStack poseStack, VertexConsumer buffer, FaceProfile profile,
                                 FaceState face, float eyeCenterX, float eyeTop, boolean left, int light) {
        int steps = 4;
        float halfLength = profile.browLength * 0.5F;
        float thickness = profile.browThickness;
        float raise = Math.max(0.0F, face.brow) * 0.8F;
        float baseY = eyeTop - 0.7F + profile.browOffsetY - raise;
        // Negative brow = angry: drop the inner end by up to browTilt pixels.
        float tilt = Math.max(0.0F, -face.brow) * profile.browTilt;
        float stepW = (halfLength * 2.0F) / steps;

        for (int i = 0; i < steps; i++) {
            float x0 = eyeCenterX - halfLength + stepW * i;
            float x1 = x0 + stepW;
            // 0 at the outer end of the face, 1 at the inner end - which side is
            // "inner" flips between the left and right brow.
            float towardCenter = (i + 0.5F) / steps;
            if (left) {
                towardCenter = 1.0F - towardCenter;
            }
            // +Y is down, so adding the tilt at the inner end is what drops it.
            float y = baseY + tilt * towardCenter;
            quad(poseStack, buffer, x0, x1, y, y + thickness,
                    BLANK_U, BLANK_U, BLANK_V, BLANK_V, profile.lineColor, light);
        }
    }

    /**
     * Draws a hand-painted eye sprite stretched across the eye rectangle. The
     * canvas resolution is the player's choice (4x4 up to 32x32) and is
     * independent of the on-head size, so a detailed eye stays detailed when the
     * eye size slider is turned down.
     */
    private static void drawEyeArt(PoseStack poseStack, VertexConsumer buffer, FaceProfile profile,
                                   float x0, float x1, float y0, float y1, boolean mirror, int light) {
        int size = profile.eyeArtSize;
        float stepX = (x1 - x0) / size;
        float stepY = (y1 - y0) / size;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int argb = profile.eyePixel(mirror ? size - 1 - x : x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                float px0 = x0 + x * stepX;
                float py0 = y0 + y * stepY;
                quad(poseStack, buffer, px0, px0 + stepX, py0, py0 + stepY,
                        BLANK_U, BLANK_U, BLANK_V, BLANK_V, argb, light);
            }
        }
    }

    private static void drawMouth(PoseStack poseStack, VertexConsumer buffer, FaceState face,
                                  FaceProfile profile, int col, int row, int light) {
        float halfW = 1.6F * profile.mouthScale;
        float open = Mth.clamp(face.mouthOpen, 0.0F, 1.0F);
        float halfH = (0.3F + open * 1.4F) * profile.mouthScale;
        float cx = profile.mouthOffsetX;
        float cy = MOUTH_ROW_PX + profile.mouthOffsetY - face.mouthSmile * 0.3F;

        float u0 = col / (float) COLUMNS;
        float u1 = (col + 0.5F) / COLUMNS;
        float v0 = (row + 0.5F) / ROWS;
        float v1 = (row + 1.0F) / ROWS;

        // Inner mouth first, then the teeth strip along the upper lip.
        int inner = open > 0.05F ? profile.mouthInnerColor : profile.lineColor;
        quad(poseStack, buffer, cx - halfW, cx + halfW, cy - halfH, cy + halfH,
                u0, u1, v0, v1, inner, light);

        if (open > 0.35F) {
            float teeth = halfH * 0.5F;
            quad(poseStack, buffer, cx - halfW, cx + halfW, cy - halfH, cy - halfH + teeth,
                    u1, (col + 1.0F) / COLUMNS, v0, v1, profile.teethColor, light);
        }
    }

    /** Coordinates are in model pixels; converted to blocks here. */
    private static void quad(PoseStack poseStack, VertexConsumer buffer,
                             float x0, float x1, float y0, float y1,
                             float u0, float u1, float v0, float v1, int argb, int light) {
        quadAt(poseStack, buffer, x0, x1, y0, y1, u0, u1, v0, v1, argb, light, 0.0F);
    }

    private static void quadAt(PoseStack poseStack, VertexConsumer buffer,
                               float x0, float x1, float y0, float y1,
                               float u0, float u1, float v0, float v1,
                               int argb, int light, float depthBias) {
        var pose = poseStack.last();
        float z = (FACE_PLANE_PX * PX) - PokeFaceConfig.frontOffset() + depthBias;
        float a = (argb >>> 24 & 0xFF) / 255.0F;
        float r = (argb >>> 16 & 0xFF) / 255.0F;
        float g = (argb >>> 8 & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        // Wound counter-clockwise as seen from the front (-Z), so the quad is not
        // culled when the player faces the camera.
        vertex(buffer, pose, x0 * PX, y1 * PX, z, u0, v1, r, g, b, a, light);
        vertex(buffer, pose, x1 * PX, y1 * PX, z, u1, v1, r, g, b, a, light);
        vertex(buffer, pose, x1 * PX, y0 * PX, z, u1, v0, r, g, b, a, light);
        vertex(buffer, pose, x0 * PX, y0 * PX, z, u0, v0, r, g, b, a, light);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, float r, float g, float b, float a, int light) {
        buffer.vertex(pose.pose(), x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), 0.0F, 0.0F, -1.0F)
                .endVertex();
    }
}
