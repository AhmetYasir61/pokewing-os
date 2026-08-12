package com.pokewing.pokeface.compat;

import com.pokewing.pokeface.PokeFace;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Simple Voice Chat integration: the mouth opens by however loud a player is
 * actually talking.
 *
 * <p>Verified against Simple Voice Chat 2.6.22 (1.20.1 Forge). SVC keeps a
 * client-side {@code TalkCache} — the same one that drives its speaker icon —
 * exposing {@code isTalking(UUID)} and {@code getPlayerAudioLevel(UUID)}. Reading
 * that shared cache rather than only the local microphone is what makes remote
 * players lip-sync too, and it keeps the dependency fully optional: every
 * failure path reports "not speaking".
 */
public final class VoiceChatCompat {

    private static boolean resolved;
    private static boolean present;
    private static Method getClient;          // ClientManager.getClient()
    private static Method getTalkCache;       // ClientVoicechat#getTalkCache()
    private static Method isTalking;          // TalkCache#isTalking(UUID)
    private static Method getAudioLevel;      // TalkCache#getPlayerAudioLevel(UUID)

    private VoiceChatCompat() {
    }

    public static boolean isLoaded() {
        resolve();
        return present;
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        present = ModList.get() != null && ModList.get().isLoaded("voicechat");
        if (!present) {
            PokeFace.LOGGER.info("PokeFace: Simple Voice Chat not installed - mouth lip-sync disabled.");
            return;
        }
        try {
            Class<?> clientManager = Class.forName("de.maxhenkel.voicechat.voice.client.ClientManager");
            Class<?> clientVoicechat = Class.forName("de.maxhenkel.voicechat.voice.client.ClientVoicechat");
            Class<?> talkCache = Class.forName("de.maxhenkel.voicechat.voice.client.TalkCache");
            getClient = clientManager.getMethod("getClient");
            getTalkCache = clientVoicechat.getMethod("getTalkCache");
            isTalking = talkCache.getMethod("isTalking", UUID.class);
            getAudioLevel = talkCache.getMethod("getPlayerAudioLevel", UUID.class);
            PokeFace.LOGGER.info("PokeFace: Simple Voice Chat detected, mouth lip-sync enabled.");
        } catch (Throwable t) {
            PokeFace.LOGGER.warn("PokeFace: voice chat present but its internals did not resolve ({}). "
                    + "The mouth falls back to the tracker/idle drivers.", t.toString());
            isTalking = null;
        }
    }

    private static Object talkCache() throws Exception {
        Object client = getClient.invoke(null);
        return client == null ? null : getTalkCache.invoke(client);
    }

    /** @return true while this player is transmitting voice. */
    public static boolean isSpeaking(UUID player) {
        resolve();
        if (!present || isTalking == null || player == null) {
            return false;
        }
        try {
            Object cache = talkCache();
            return cache != null && Boolean.TRUE.equals(isTalking.invoke(cache, player));
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Mouth-open value in [0,1] for a player, from SVC's own audio level.
     *
     * <p>SVC reports a smoothed level that sits low for normal speech, so it is
     * scaled up and floored while talking — a mouth that never quite opens reads
     * as broken, and one held wide open reads as a scream.
     */
    public static float mouthOpenFor(UUID player, long gameTime, float partialTick) {
        if (!isSpeaking(player)) {
            return 0.0F;
        }
        double level = 0.0D;
        try {
            Object cache = talkCache();
            if (cache != null) {
                Object value = getAudioLevel.invoke(cache, player);
                if (value instanceof Number n) {
                    level = n.doubleValue();
                }
            }
        } catch (Throwable ignored) {
            // Fall through to the wobble below; the player is still talking.
        }
        float amplitude = (float) Math.min(1.0D, Math.max(0.0D, level) * 3.0D);
        if (amplitude < 0.15F) {
            // No usable level: synthesise a two-rate flap, deterministic per player
            // so every viewer sees the same mouth.
            double t = (gameTime + partialTick) * 0.45D + (player.hashCode() & 0xFF) * 0.13D;
            amplitude = (float) (0.45D + 0.25D * Math.sin(t) + 0.10D * Math.sin(t * 2.7D));
        }
        return Math.max(0.15F, Math.min(1.0F, amplitude));
    }
}
