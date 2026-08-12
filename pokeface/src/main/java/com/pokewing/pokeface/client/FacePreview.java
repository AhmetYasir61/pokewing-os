package com.pokewing.pokeface.client;

import com.pokewing.pokeface.face.FaceProfile;
import com.pokewing.pokeface.face.FaceState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A flat, 2D preview of the face: the head's front skin pixels with the animated
 * features drawn on top, at any size.
 *
 * <p>The menu used to preview the character by rendering the actual player
 * entity, which goes through whatever renderer is installed — under Epic Fight
 * that is a patched renderer whose armature is not posed for an inventory
 * render, so the preview came out stretched and broken. Drawing the face
 * ourselves sidesteps the entity renderer entirely, is correct under any
 * combination of mods, and is cheap enough to use as a thumbnail in a list of a
 * thousand characters.
 *
 * <p>The geometry mirrors {@link FaceRenderer}: the head's 8x8 face box, with
 * the eye row 4.5 and the mouth row 2.5 model pixels above the head pivot.
 */
public final class FacePreview {

    /** The face box is 8x8 skin pixels at (8,8) on a 64x64 skin. */
    private static final int FACE_U = 8;
    private static final int FACE_V = 8;
    private static final int HAT_U = 40;
    private static final int BOX = 8;
    /** Rows measured from the top of the face box, matching FaceRenderer. */
    private static final float EYE_ROW = 3.5F;
    private static final float MOUTH_ROW = 5.5F;

    private FacePreview() {
    }

    /**
     * @param scale pixels on screen per skin pixel; the preview is 8*scale square
     */
    public static void draw(GuiGraphics graphics, int x, int y, int scale,
                            FaceProfile profile, FaceState face) {
        int size = BOX * scale;
        graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF101010);

        ResourceLocation skin = skin();
        if (skin != null) {
            graphics.blit(skin, x, y, size, size, FACE_U, FACE_V, BOX, BOX, 64, 64);
            // The hat layer is part of the face as the world sees it.
            graphics.blit(skin, x, y, size, size, HAT_U, FACE_V, BOX, BOX, 64, 64);
        } else {
            graphics.fill(x, y, x + size, y + size, 0xFF6B4423);
        }

        drawEyes(graphics, x, y, scale, profile, face);
        drawMouth(graphics, x, y, scale, profile, face);
    }

    private static ResourceLocation skin() {
        if (Minecraft.getInstance().player instanceof AbstractClientPlayer player) {
            return player.getSkinTextureLocation();
        }
        return null;
    }

    private static void drawEyes(GuiGraphics graphics, int ox, int oy, int scale,
                                 FaceProfile profile, FaceState face) {
        float halfW = profile.eyeScale;
        float halfH = profile.eyeScale;
        float spacing = profile.eyeSpacing * 0.5F;
        float centerY = EYE_ROW + profile.eyeOffsetY;
        float centerX = 4.0F + profile.eyeOffsetX;

        float openL = 1.0F - Mth.clamp(face.blinkLeft, 0.0F, 1.0F);
        float openR = 1.0F - Mth.clamp(face.blinkRight, 0.0F, 1.0F);
        float gazeX = face.gazeX * halfW * 0.5F;
        float gazeY = -face.gazeY * halfH * 0.5F;

        for (int side = 0; side < 2; side++) {
            boolean left = side == 0;
            float converge = left ? profile.eyeConverge : -profile.eyeConverge;
            float perEyeX = left ? profile.eyeLeftOffsetX : profile.eyeRightOffsetX;
            float perEyeY = left ? profile.eyeLeftOffsetY : profile.eyeRightOffsetY;
            float sx = centerX + (left ? -spacing : spacing) + converge + perEyeX;
            float sy = centerY + perEyeY;
            float open = left ? openL : openR;

            if (profile.drawSclera) {
                rect(graphics, ox, oy, scale, sx - halfW, sy - halfH * open, sx + halfW, sy + halfH * open,
                        left ? profile.scleraColor : profile.scleraColorRight);
            }
            float irisHalfW = halfW * profile.irisScale;
            float irisHalfH = halfH * profile.irisScale * open;
            float slackX = Math.max(0.0F, halfW - irisHalfW);
            float slackY = Math.max(0.0F, halfH * open - irisHalfH);
            float ix = sx + Mth.clamp(gazeX, -slackX, slackX);
            float iy = sy + Mth.clamp(gazeY, -slackY, slackY);

            if (profile.hasEyeArt()) {
                drawEyeArt(graphics, ox, oy, scale, profile, left,
                        ix - irisHalfW, iy - irisHalfH, ix + irisHalfW, iy + irisHalfH);
            } else {
                rect(graphics, ox, oy, scale, ix - irisHalfW, iy - irisHalfH, ix + irisHalfW, iy + irisHalfH,
                        left ? profile.eyeColor : profile.eyeColorRight);
                if (profile.pupilScale > 0.05F) {
                    float pw = irisHalfW * profile.pupilScale;
                    float ph = irisHalfH * profile.pupilScale;
                    rect(graphics, ox, oy, scale, ix - pw, iy - ph, ix + pw, iy + ph,
                            left ? profile.pupilColor : profile.pupilColorRight);
                }
            }

            if (profile.drawBrows) {
                drawBrow(graphics, ox, oy, scale, profile, face, sx, sy - halfH, left);
            }
        }
    }

    private static void drawEyeArt(GuiGraphics graphics, int ox, int oy, int scale, FaceProfile profile,
                                   boolean left, float x0, float y0, float x1, float y1) {
        int size = profile.eyeArtSize;
        float stepX = (x1 - x0) / size;
        float stepY = (y1 - y0) / size;
        boolean mirror = !left && profile.mirrorRightEye && !profile.hasSeparateRightEye();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int argb = profile.eyePixel(left, mirror ? size - 1 - x : x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                float px = x0 + x * stepX;
                float py = y0 + y * stepY;
                rect(graphics, ox, oy, scale, px, py, px + stepX, py + stepY, argb);
            }
        }
    }

    private static void drawBrow(GuiGraphics graphics, int ox, int oy, int scale, FaceProfile profile,
                                 FaceState face, float eyeCenterX, float eyeTop, boolean left) {
        int steps = 4;
        float halfLength = profile.browLength * 0.5F;
        float raise = Math.max(0.0F, face.brow) * 0.8F;
        float baseY = eyeTop - 0.7F + profile.browOffsetY - raise;
        float tilt = Math.max(0.0F, -face.brow) * profile.browTilt;
        float stepW = (halfLength * 2.0F) / steps;

        for (int i = 0; i < steps; i++) {
            float x0 = eyeCenterX - halfLength + stepW * i;
            float towardCenter = (i + 0.5F) / steps;
            if (left) {
                towardCenter = 1.0F - towardCenter;
            }
            float y = baseY + tilt * towardCenter;
            rect(graphics, ox, oy, scale, x0, y, x0 + stepW, y + profile.browThickness, profile.lineColor);
        }
    }

    private static void drawMouth(GuiGraphics graphics, int ox, int oy, int scale,
                                  FaceProfile profile, FaceState face) {
        float halfW = 1.6F * profile.mouthScale;
        float open = Mth.clamp(face.mouthOpen, 0.0F, 1.0F);
        float halfH = (0.3F + open * 1.4F) * profile.mouthScale;
        float cx = 4.0F + profile.mouthOffsetX;
        float cy = MOUTH_ROW + profile.mouthOffsetY - face.mouthSmile * 0.3F;

        int inner = open > 0.05F ? profile.mouthInnerColor : profile.lineColor;
        rect(graphics, ox, oy, scale, cx - halfW, cy - halfH, cx + halfW, cy + halfH, inner);
        if (open > 0.35F) {
            rect(graphics, ox, oy, scale, cx - halfW, cy - halfH, cx + halfW, cy - halfH + halfH * 0.5F,
                    profile.teethColor);
        }
    }

    /** Skin-pixel rectangle to screen, clipped to the face box. */
    private static void rect(GuiGraphics graphics, int ox, int oy, int scale,
                             float x0, float y0, float x1, float y1, int argb) {
        int left = ox + Math.round(Mth.clamp(x0, 0.0F, BOX) * scale);
        int top = oy + Math.round(Mth.clamp(y0, 0.0F, BOX) * scale);
        int right = ox + Math.round(Mth.clamp(x1, 0.0F, BOX) * scale);
        int bottom = oy + Math.round(Mth.clamp(y1, 0.0F, BOX) * scale);
        if (right > left && bottom > top) {
            graphics.fill(left, top, right, bottom, argb);
        }
    }
}
