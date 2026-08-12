package com.pokewing.pokeface.api;

import com.pokewing.pokeface.client.ClientFaceStore;
import com.pokewing.pokeface.client.PokeFaceClient;
import com.pokewing.pokeface.face.CharacterLibrary;
import com.pokewing.pokeface.face.Expression;
import com.pokewing.pokeface.face.FaceState;

import net.minecraft.client.Minecraft;

import java.util.UUID;

/**
 * The stable entry point other mods use to record and replay faces.
 *
 * <p>It exists so a filming tool can capture what a performer's face was doing
 * each tick and put it back on a stand-in later. Everything is expressed as
 * primitives — a packed long and an expression ordinal — so a caller can bind to
 * this reflectively and keep working across changes to the internals, which is
 * exactly how {@code EFMocap} uses it: no compile-time dependency in either
 * direction, and neither mod requires the other to be installed.
 *
 * <p>Client side only.
 */
public final class PokeFaceApi {

    private PokeFaceApi() {
    }

    /** Blendshapes of the local player's face right now, packed into 8 bytes. */
    public static long sampleLocalPacked() {
        return PokeFaceClient.director().current().pack();
    }

    /** The expression tile the local face is on, as an {@link Expression} ordinal. */
    public static int sampleLocalExpression() {
        return PokeFaceClient.director().current().expression.ordinal();
    }

    /**
     * Drives another entity's face — a replay clone, typically — from a captured
     * sample. The face renders through the normal path, so it follows the head
     * bone and Epic Fight animations like any other.
     *
     * @param player   the entity's UUID
     * @param packed   a value from {@link #sampleLocalPacked()}
     * @param expression an ordinal from {@link #sampleLocalExpression()}
     */
    public static void pushFace(UUID player, long packed, int expression) {
        if (player == null) {
            return;
        }
        Expression[] values = Expression.values();
        int index = Math.floorMod(expression, values.length);
        ClientFaceStore.put(player, FaceState.unpack(packed, values[index]));
    }

    /**
     * Gives an entity the local player's face profile — eye colours, brows, the
     * painted patch, attachments. Without this a clone would wear the captured
     * expressions on a default face.
     */
    public static void copyLocalProfileTo(UUID player) {
        if (player != null) {
            ClientFaceStore.putProfile(player, PokeFaceClient.localProfile().copy());
        }
    }

    /**
     * Names of the saved characters, so a filming tool can offer them as a cast
     * list rather than making the user retype names.
     */
    public static java.util.List<String> characterNames() {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (CharacterLibrary.Character character : CharacterLibrary.all()) {
            names.add(character.name);
        }
        return names;
    }

    /**
     * Dresses an entity in a saved character's whole look — eye colours and
     * style, brows, the painted face, the ears and tail attachments — rather than
     * in whatever the local player happens to be wearing. This is what lets each
     * clone in a scene be its own character.
     *
     * @return false when no character by that name is saved
     */
    public static boolean applyCharacterTo(UUID player, String characterName) {
        if (player == null || characterName == null || characterName.isEmpty()) {
            return false;
        }
        for (CharacterLibrary.Character character : CharacterLibrary.all()) {
            if (character.name.equalsIgnoreCase(characterName)) {
                ClientFaceStore.putProfile(player, character.profile.copy());
                return true;
            }
        }
        return false;
    }

    /** Forgets an entity's face, e.g. when a clone is despawned. */
    public static void clear(UUID player) {
        if (player != null) {
            ClientFaceStore.put(player, null);
        }
    }

    /** True once the mod is far enough along to answer the calls above. */
    public static boolean isReady() {
        return Minecraft.getInstance() != null && PokeFaceClient.director() != null;
    }
}
