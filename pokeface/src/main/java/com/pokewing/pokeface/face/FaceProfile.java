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

    /** ARGB colours. {@link #eyeColor} is the left eye; the right has its own. */
    public int lineColor = 0xFF101010;
    public int eyeColor = 0xFF3A3A3A;
    public int eyeColorRight = 0xFF3A3A3A;
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
    /** Drive the mouth from the voice chat microphone when available. */
    public boolean voiceChatMouth = true;
    /** Use the webcam tracker when it is streaming. */
    public boolean useTracker = true;

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
        p.eyeColor = this.eyeColor;
        p.eyeColorRight = this.eyeColorRight;
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
        buf.writeInt(this.eyeColor);
        buf.writeInt(this.eyeColorRight);
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
        buf.writeVarInt(hasEyeArt() ? this.eyeArtSize : 0);
        if (hasEyeArt()) {
            for (int pixel : this.eyeArt) {
                buf.writeInt(pixel);
            }
        }
    }

    public boolean hasEyeArt() {
        return this.eyeArtSize > 0 && this.eyeArt != null
                && this.eyeArt.length == this.eyeArtSize * this.eyeArtSize;
    }

    public int eyePixel(int x, int y) {
        return this.eyeArt[y * this.eyeArtSize + x];
    }

    /**
     * Switches the eye canvas to {@code size}, resampling whatever is already
     * drawn with nearest neighbour so a sketch is not lost when going up or down
     * a resolution.
     */
    public void resizeEyeArt(int size) {
        int[] resampled = new int[size * size];
        if (hasEyeArt()) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int sx = x * this.eyeArtSize / size;
                    int sy = y * this.eyeArtSize / size;
                    resampled[y * size + x] = this.eyeArt[sy * this.eyeArtSize + sx];
                }
            }
        }
        this.eyeArtSize = size;
        this.eyeArt = resampled;
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
        p.eyeColor = buf.readInt();
        p.eyeColorRight = buf.readInt();
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
        int eyeSize = buf.readVarInt();
        // Clamped against the known sizes so a malformed packet cannot make the
        // client allocate an arbitrary array.
        if (eyeSize > 0 && eyeSize <= 32) {
            p.eyeArtSize = eyeSize;
            p.eyeArt = new int[eyeSize * eyeSize];
            for (int i = 0; i < p.eyeArt.length; i++) {
                p.eyeArt[i] = buf.readInt();
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
