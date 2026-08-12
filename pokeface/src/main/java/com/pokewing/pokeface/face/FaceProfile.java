package com.pokewing.pokeface.face;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pokewing.pokeface.PokeFace;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Per-player appearance: which style is worn, where the eyes and mouth sit on the
 * head, and the colours used for the mouth interior, teeth and the feature lines.
 *
 * <p>Offsets are in skin pixels relative to the 8x8 face region of the head
 * (top-left of the face box is 0,0), so tweaking them lines the overlay up with
 * any custom head texture the player is using.
 */
public final class FaceProfile {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** The face patch is the 8x8 front area of the head. */
    public static final int FACE_SIZE = 8;
    public static final int FACE_PIXELS = FACE_SIZE * FACE_SIZE;

    /** Cap on attachments, so a malformed packet cannot spin up a huge list. */
    public static final int MAX_ATTACHMENTS = 16;

    /** Selectable eye canvas resolutions. */
    public static final int[] EYE_ART_SIZES = {4, 8, 16, 32};

    public String style = FaceStyle.DEFAULT.key();
    /** Eye anchor offset in skin pixels, -3..3. */
    public float eyeOffsetX = 0.0F;
    public float eyeOffsetY = 0.0F;
    /** Horizontal gap between the two eyes, in skin pixels, 1..5. */
    public float eyeSpacing = 3.0F;
    public float eyeScale = 1.0F;
    /** Mouth anchor offset in skin pixels, -3..3. */
    /** Per-eye fine offsets, in skin pixels, on top of the shared eye offset. */
    public float eyeLeftOffsetX = 0.0F;
    public float eyeLeftOffsetY = 0.0F;
    public float eyeRightOffsetX = 0.0F;
    public float eyeRightOffsetY = 0.0F;
    /**
     * How far the two eyes turn inward, in skin pixels. A small convergence
     * reads as "looking at something in front of you" instead of two eyes
     * staring straight ahead in parallel.
     */
    public float eyeConverge = 0.35F;
    /** Mirror the right eye so the pair is symmetric rather than cloned. */
    public boolean mirrorRightEye = true;

    public float mouthOffsetX = 0.0F;
    public float mouthOffsetY = 0.0F;
    public float mouthScale = 1.0F;

    /**
     * ARGB colours. An eye is drawn in two parts: {@link #scleraColor} is the
     * white, which stays put, and {@link #eyeColor} / {@link #eyeColorRight} are
     * the irises, which move with the gaze inside it.
     */
    public int lineColor = 0xFF101010;
    public int scleraColor = 0xFFF4F1EA;
    public int scleraColorRight = 0xFFF4F1EA;
    public int eyeColor = 0xFF3A6BA5;
    public int eyeColorRight = 0xFF3A6BA5;
    public int pupilColor = 0xFF101010;
    public int pupilColorRight = 0xFF101010;

    /** Iris size as a fraction of the eye; the rest of the eye is sclera. */
    public float irisScale = 0.55F;
    /** Pupil size as a fraction of the iris. 0 disables the pupil. */
    public float pupilScale = 0.45F;
    /** Draw the white of the eye at all. Off gives the old flat-blob eye. */
    public boolean drawSclera = true;

    /** Brows. Thickness and length are in skin pixels. */
    public boolean drawBrows = true;
    public float browThickness = 0.5F;
    public float browLength = 2.6F;
    public float browOffsetY = 0.0F;
    /**
     * How steeply the brows tilt at full anger, in skin pixels of drop across the
     * brow. The inner ends come down toward the middle of the face, which is the
     * inverted-V that reads as angry.
     */
    public float browTilt = 1.4F;
    public int mouthInnerColor = 0xFF7A1F28;
    public int teethColor = 0xFFF2EAD8;

    /** Read the head pixels from the worn skin instead of the atlas background. */
    public boolean useSkinHead = true;
    /**
     * Hand-painted 8x8 patch drawn over the face area of the head, in ARGB, row
     * major. 0 means "leave the skin alone". This is what lets a player paint
     * their own skin tone over the eyes their skin already has, so the overlay
     * eyes do not collide with the painted-on ones.
     */
    public int[] facePixels = new int[FACE_PIXELS];
    public boolean paintEnabled = true;

    /**
     * Optional hand-drawn eye sprite. {@link #eyeArtSize} is the edge length in
     * pixels (one of {@link #EYE_ART_SIZES}); 0 means "use the built-in sprite
     * from the style atlas". The art is stretched across whatever the eye size
     * slider asks for, so the resolution chosen here is about detail, not scale.
     */
    public int eyeArtSize;
    public int[] eyeArt = new int[0];
    /**
     * The right eye's sprite. Empty means "use the left one", which is what
     * mirroring expects; drawing into it makes the two eyes independent.
     */
    public int[] eyeArtRight = new int[0];
    /** Drive the mouth from the voice chat microphone when available. */
    public boolean voiceChatMouth = true;
    /** Use the webcam tracker when it is streaming. */
    public boolean useTracker = true;

    /** Copies every left-eye colour onto the right eye. */
    public void mirrorEyeColors() {
        this.eyeColorRight = this.eyeColor;
        this.scleraColorRight = this.scleraColor;
        this.pupilColorRight = this.pupilColor;
    }

    public java.util.List<Attachment> normalisedAttachments() {
        if (this.attachments == null) {
            this.attachments = new java.util.ArrayList<>();
        }
        while (this.attachments.size() > MAX_ATTACHMENTS) {
            this.attachments.remove(this.attachments.size() - 1);
        }
        return this.attachments;
    }

    public FaceStyle styleEnum() {
        return FaceStyle.byName(this.style);
    }

    public FaceProfile copy() {
        FaceProfile p = new FaceProfile();
        p.style = this.style;
        p.eyeOffsetX = this.eyeOffsetX;
        p.eyeOffsetY = this.eyeOffsetY;
        p.eyeSpacing = this.eyeSpacing;
        p.eyeScale = this.eyeScale;
        p.mouthOffsetX = this.mouthOffsetX;
        p.mouthOffsetY = this.mouthOffsetY;
        p.mouthScale = this.mouthScale;
        p.lineColor = this.lineColor;
        p.scleraColor = this.scleraColor;
        p.scleraColorRight = this.scleraColorRight;
        p.pupilColor = this.pupilColor;
        p.pupilColorRight = this.pupilColorRight;
        p.eyeColor = this.eyeColor;
        p.eyeColorRight = this.eyeColorRight;
        p.irisScale = this.irisScale;
        p.pupilScale = this.pupilScale;
        p.drawSclera = this.drawSclera;
        p.drawBrows = this.drawBrows;
        p.browThickness = this.browThickness;
        p.browLength = this.browLength;
        p.browOffsetY = this.browOffsetY;
        p.browTilt = this.browTilt;
        p.eyeLeftOffsetX = this.eyeLeftOffsetX;
        p.eyeLeftOffsetY = this.eyeLeftOffsetY;
        p.eyeRightOffsetX = this.eyeRightOffsetX;
        p.eyeRightOffsetY = this.eyeRightOffsetY;
        p.eyeConverge = this.eyeConverge;
        p.mirrorRightEye = this.mirrorRightEye;
        p.mouthInnerColor = this.mouthInnerColor;
        p.teethColor = this.teethColor;
        p.useSkinHead = this.useSkinHead;
        p.paintEnabled = this.paintEnabled;
        p.facePixels = this.facePixels.clone();
        p.eyeArtSize = this.eyeArtSize;
        p.eyeArt = this.eyeArt.clone();
        p.eyeArtRight = this.eyeArtRight.clone();
        p.eyeGlow = this.eyeGlow;
        p.eyeGlowSpread = this.eyeGlowSpread;
        p.attachments = new java.util.ArrayList<>();
        for (Attachment attachment : this.attachments) {
            p.attachments.add(attachment.copy());
        }
        p.voiceChatMouth = this.voiceChatMouth;
        p.useTracker = this.useTracker;
        return p;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.style, 32);
        buf.writeFloat(this.eyeOffsetX);
        buf.writeFloat(this.eyeOffsetY);
        buf.writeFloat(this.eyeSpacing);
        buf.writeFloat(this.eyeScale);
        buf.writeFloat(this.mouthOffsetX);
        buf.writeFloat(this.mouthOffsetY);
        buf.writeFloat(this.mouthScale);
        buf.writeInt(this.lineColor);
        buf.writeInt(this.scleraColor);
        buf.writeInt(this.scleraColorRight);
        buf.writeInt(this.pupilColor);
        buf.writeInt(this.pupilColorRight);
        buf.writeInt(this.eyeColor);
        buf.writeInt(this.eyeColorRight);
        buf.writeFloat(this.irisScale);
        buf.writeFloat(this.pupilScale);
        buf.writeBoolean(this.drawSclera);
        buf.writeBoolean(this.drawBrows);
        buf.writeFloat(this.browThickness);
        buf.writeFloat(this.browLength);
        buf.writeFloat(this.browOffsetY);
        buf.writeFloat(this.browTilt);
        buf.writeFloat(this.eyeLeftOffsetX);
        buf.writeFloat(this.eyeLeftOffsetY);
        buf.writeFloat(this.eyeRightOffsetX);
        buf.writeFloat(this.eyeRightOffsetY);
        buf.writeFloat(this.eyeConverge);
        buf.writeBoolean(this.mirrorRightEye);
        buf.writeInt(this.mouthInnerColor);
        buf.writeInt(this.teethColor);
        buf.writeBoolean(this.useSkinHead);
        buf.writeBoolean(this.paintEnabled);
        for (int pixel : normalisedPixels()) {
            buf.writeInt(pixel);
        }
        buf.writeBoolean(this.eyeGlow);
        buf.writeFloat(this.eyeGlowSpread);
        java.util.List<Attachment> list = normalisedAttachments();
        buf.writeVarInt(list.size());
        for (Attachment attachment : list) {
            attachment.write(buf);
        }
        buf.writeVarInt(hasEyeArt() ? this.eyeArtSize : 0);
        if (hasEyeArt()) {
            for (int pixel : this.eyeArt) {
                buf.writeInt(pixel);
            }
            boolean separate = hasSeparateRightEye();
            buf.writeBoolean(separate);
            if (separate) {
                for (int pixel : this.eyeArtRight) {
                    buf.writeInt(pixel);
                }
            }
        }
    }

    public boolean hasEyeArt() {
        return this.eyeArtSize > 0 && this.eyeArt != null
                && this.eyeArt.length == this.eyeArtSize * this.eyeArtSize;
    }

    /** The right eye falls back to the left sprite until it is drawn into. */
    public int[] eyeArtFor(boolean left) {
        if (left) {
            return this.eyeArt;
        }
        return this.eyeArtRight != null && this.eyeArtRight.length == this.eyeArt.length
                ? this.eyeArtRight : this.eyeArt;
    }

    public int eyePixel(boolean left, int x, int y) {
        return eyeArtFor(left)[y * this.eyeArtSize + x];
    }

    /** True once the right eye has pixels of its own. */
    public boolean hasSeparateRightEye() {
        if (this.eyeArtRight == null || this.eyeArtRight.length != this.eyeArt.length) {
            return false;
        }
        for (int pixel : this.eyeArtRight) {
            if ((pixel >>> 24) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Switches the eye canvas to {@code size}, resampling whatever is already
     * drawn with nearest neighbour so a sketch is not lost when going up or down
     * a resolution.
     */
    public void resizeEyeArt(int size) {
        int[] left = resample(this.eyeArt, size);
        int[] right = resample(this.eyeArtRight, size);
        this.eyeArtSize = size;
        this.eyeArt = left;
        this.eyeArtRight = right;
    }

    private int[] resample(int[] source, int size) {
        int[] out = new int[size * size];
        if (hasEyeArt() && source != null && source.length == this.eyeArtSize * this.eyeArtSize) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int sx = x * this.eyeArtSize / size;
                    int sy = y * this.eyeArtSize / size;
                    out[y * size + x] = source[sy * this.eyeArtSize + sx];
                }
            }
        }
        return out;
    }

    /** Copies the left eye's drawing onto the right, mirrored horizontally. */
    public void mirrorEyeArtToRight() {
        if (!hasEyeArt()) {
            return;
        }
        int size = this.eyeArtSize;
        int[] out = new int[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                out[y * size + x] = this.eyeArt[y * size + (size - 1 - x)];
            }
        }
        this.eyeArtRight = out;
    }

    /** Guards against a hand-edited JSON with a wrong-sized pixel array. */
    public int[] normalisedPixels() {
        if (this.facePixels == null || this.facePixels.length != FACE_PIXELS) {
            this.facePixels = new int[FACE_PIXELS];
        }
        return this.facePixels;
    }

    public int pixel(int x, int y) {
        return normalisedPixels()[y * FACE_SIZE + x];
    }

    public void setPixel(int x, int y, int argb) {
        normalisedPixels()[y * FACE_SIZE + x] = argb;
    }

    public static FaceProfile read(FriendlyByteBuf buf) {
        FaceProfile p = new FaceProfile();
        p.style = buf.readUtf(32);
        p.eyeOffsetX = buf.readFloat();
        p.eyeOffsetY = buf.readFloat();
        p.eyeSpacing = buf.readFloat();
        p.eyeScale = buf.readFloat();
        p.mouthOffsetX = buf.readFloat();
        p.mouthOffsetY = buf.readFloat();
        p.mouthScale = buf.readFloat();
        p.lineColor = buf.readInt();
        p.scleraColor = buf.readInt();
        p.scleraColorRight = buf.readInt();
        p.pupilColor = buf.readInt();
        p.pupilColorRight = buf.readInt();
        p.eyeColor = buf.readInt();
        p.eyeColorRight = buf.readInt();
        p.irisScale = buf.readFloat();
        p.pupilScale = buf.readFloat();
        p.drawSclera = buf.readBoolean();
        p.drawBrows = buf.readBoolean();
        p.browThickness = buf.readFloat();
        p.browLength = buf.readFloat();
        p.browOffsetY = buf.readFloat();
        p.browTilt = buf.readFloat();
        p.eyeLeftOffsetX = buf.readFloat();
        p.eyeLeftOffsetY = buf.readFloat();
        p.eyeRightOffsetX = buf.readFloat();
        p.eyeRightOffsetY = buf.readFloat();
        p.eyeConverge = buf.readFloat();
        p.mirrorRightEye = buf.readBoolean();
        p.mouthInnerColor = buf.readInt();
        p.teethColor = buf.readInt();
        p.useSkinHead = buf.readBoolean();
        p.paintEnabled = buf.readBoolean();
        for (int i = 0; i < FACE_PIXELS; i++) {
            p.facePixels[i] = buf.readInt();
        }
        p.eyeGlow = buf.readBoolean();
        p.eyeGlowSpread = buf.readFloat();
        int attachmentCount = Math.min(buf.readVarInt(), MAX_ATTACHMENTS);
        for (int i = 0; i < attachmentCount; i++) {
            p.attachments.add(Attachment.read(buf));
        }
        int eyeSize = buf.readVarInt();
        // Clamped against the known sizes so a malformed packet cannot make the
        // client allocate an arbitrary array.
        if (eyeSize > 0 && eyeSize <= 32) {
            p.eyeArtSize = eyeSize;
            p.eyeArt = new int[eyeSize * eyeSize];
            for (int i = 0; i < p.eyeArt.length; i++) {
                p.eyeArt[i] = buf.readInt();
            }
            if (buf.readBoolean()) {
                p.eyeArtRight = new int[p.eyeArt.length];
                for (int i = 0; i < p.eyeArtRight.length; i++) {
                    p.eyeArtRight[i] = buf.readInt();
                }
            }
        }
        return p;
    }

    public static FaceProfile load(Path file) {
        try {
            if (Files.exists(file)) {
                return GSON.fromJson(Files.readString(file), FaceProfile.class);
            }
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not read face profile ({}), using defaults.", e.toString());
        }
        return new FaceProfile();
    }

    public void save(Path file) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(this));
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not write face profile: {}", e.toString());
        }
    }
}
