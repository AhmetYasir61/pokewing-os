package com.pokewing.pokeface.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pokewing.pokeface.PokeFace;
import com.pokewing.pokeface.face.FaceProfile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bakes the hand-painted face patch into the player's actual skin texture.
 *
 * <p>Drawing the patch as a floating plane in front of the head was never the
 * same thing as editing the skin: it only covered the head's front face, sat at
 * a depth that could fight with hats and armour, and vanished anywhere the
 * overlay was not drawn. Here the skin image itself is rewritten — the painted
 * pixels replace the 8x8 face region, and the matching hat-layer pixels are
 * cleared so eyes drawn on the overlay layer cannot show through.
 *
 * <p>The composited image is registered under the player's own skin
 * {@link ResourceLocation}, which is a public {@link TextureManager} operation:
 * no mixin, no renderer replacement, and it is per player, so other players'
 * skins are untouched. The untouched original is kept so the edit can be undone
 * the moment the patch is cleared or disabled.
 */
public final class SkinOverride {

    /** Face region of a 64x64 skin: the head's front face. */
    private static final int FACE_U = 8;
    private static final int FACE_V = 8;
    /** Hat layer sits one head-width to the right of the base head. */
    private static final int HAT_U = 40;
    private static final int HAT_V = 8;

    private static final Map<UUID, Entry> ENTRIES = new HashMap<>();

    private SkinOverride() {
    }

    private static final class Entry {
        ResourceLocation location;
        NativeImage original;
        int appliedHash;
        boolean active;
    }

    /**
     * Applies (or removes) the override for a player. Cheap to call every frame:
     * the texture is only rebuilt when the painted pixels actually change.
     */
    public static void sync(AbstractClientPlayer player, FaceProfile profile) {
        if (!RenderSystem.isOnRenderThread()) {
            return;
        }
        Entry entry = ENTRIES.computeIfAbsent(player.getUUID(), id -> new Entry());
        ResourceLocation skin = player.getSkinTextureLocation();
        if (skin == null) {
            return;
        }
        // Either a painted face or a chosen skin means the worn texture is not
        // the account's own any more.
        boolean painting = profile.paintEnabled && hasPaint(profile);
        boolean wearingSkin = profile.skin != null && !profile.skin.isEmpty()
                && SkinLibrary.has(profile.skin);
        boolean wanted = painting || wearingSkin;
        int hash = wanted ? Arrays.hashCode(profile.normalisedPixels())
                * 31 + (wearingSkin ? profile.skin.hashCode() : 0) : 0;

        if (!skin.equals(entry.location)) {
            // The player changed skin: drop the cached original, it belongs to
            // the previous texture.
            restore(entry);
            entry.location = skin;
            entry.original = null;
            entry.appliedHash = -1;
        }
        if (wanted && hash == entry.appliedHash && entry.active) {
            return;
        }
        if (!wanted) {
            if (entry.active) {
                restore(entry);
            }
            entry.appliedHash = 0;
            return;
        }

        try {
            // A chosen skin replaces the base outright; otherwise the account's
            // own texture is read back off the GPU and painted onto.
            NativeImage base = wearingSkin ? SkinLibrary.image(profile.skin) : null;
            if (base == null) {
                if (entry.original == null) {
                    entry.original = readTexture(skin);
                }
                base = entry.original;
            } else if (entry.original == null) {
                entry.original = readTexture(skin);
            }
            if (base == null) {
                return;
            }
            NativeImage composited = copyOf(base);
            if (painting) {
                paint(composited, profile);
            }
            Minecraft.getInstance().getTextureManager().register(skin, new DynamicTexture(composited));
            entry.appliedHash = hash;
            entry.active = true;
        } catch (Throwable t) {
            // No overlay fallback on purpose: the paint is meant to BE the skin.
            // If it cannot be baked, the skin is simply left as it is.
            PokeFace.LOGGER.warn("PokeFace: could not bake the face patch into the skin: {}", t.toString());
            entry.active = false;
            entry.appliedHash = hash;
        }
    }

    /** True when the skin currently carries the baked paint; used for status text. */
    public static boolean isActive(UUID player) {
        Entry entry = ENTRIES.get(player);
        return entry != null && entry.active;
    }

    public static void forget(UUID player) {
        Entry entry = ENTRIES.remove(player);
        if (entry != null) {
            restore(entry);
        }
    }

    public static void clear() {
        ENTRIES.values().forEach(SkinOverride::restore);
        ENTRIES.clear();
    }

    private static boolean hasPaint(FaceProfile profile) {
        for (int pixel : profile.normalisedPixels()) {
            if ((pixel >>> 24) != 0) {
                return true;
            }
        }
        return false;
    }

    private static void restore(Entry entry) {
        if (entry.location != null && entry.original != null && entry.active) {
            try {
                Minecraft.getInstance().getTextureManager()
                        .register(entry.location, new DynamicTexture(copyOf(entry.original)));
            } catch (Throwable ignored) {
                // Nothing useful to do; the vanilla skin loader will re-register
                // the real texture on the next skin refresh.
            }
        }
        entry.active = false;
    }

    /** Reads the currently bound skin back off the GPU. */
    private static NativeImage readTexture(ResourceLocation location) {
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        AbstractTexture texture = manager.getTexture(location, null);
        if (texture == null) {
            return null;
        }
        RenderSystem.bindTexture(texture.getId());
        int width = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        if (width < 64 || height < 32) {
            return null;
        }
        NativeImage image = new NativeImage(width, height, false);
        image.downloadTexture(0, false);
        return image;
    }

    private static NativeImage copyOf(NativeImage source) {
        NativeImage copy = new NativeImage(source.getWidth(), source.getHeight(), false);
        copy.copyFrom(source);
        return copy;
    }

    private static void paint(NativeImage image, FaceProfile profile) {
        int size = FaceProfile.FACE_SIZE;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int argb = profile.pixel(x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                image.setPixelRGBA(FACE_U + x, FACE_V + y, toAbgr(argb));
                if (HAT_U + x < image.getWidth() && HAT_V + y < image.getHeight()) {
                    // Clear the hat layer above a painted pixel, otherwise a skin
                    // that draws its eyes on the overlay keeps showing them.
                    image.setPixelRGBA(HAT_U + x, HAT_V + y, 0);
                }
            }
        }
    }

    /** NativeImage stores pixels as ABGR; the profile stores ARGB. */
    private static int toAbgr(int argb) {
        int a = argb >>> 24 & 0xFF;
        int r = argb >>> 16 & 0xFF;
        int g = argb >>> 8 & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }
}
