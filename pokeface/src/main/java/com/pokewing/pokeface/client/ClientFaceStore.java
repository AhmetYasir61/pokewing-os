package com.pokewing.pokeface.client;

import com.pokewing.pokeface.face.FaceProfile;
import com.pokewing.pokeface.face.FaceState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side cache of every visible player's face state and appearance. */
public final class ClientFaceStore {

    private static final Map<UUID, FaceState> STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, FaceProfile> PROFILES = new ConcurrentHashMap<>();

    private ClientFaceStore() {
    }

    public static void put(UUID id, FaceState state) {
        if (state == null) {
            STATES.remove(id);
            return;
        }
        STATES.put(id, state);
    }

    public static FaceState get(UUID id) {
        return STATES.get(id);
    }

    public static void putProfile(UUID id, FaceProfile profile) {
        PROFILES.put(id, profile);
    }

    public static FaceProfile profile(UUID id) {
        return PROFILES.get(id);
    }

    public static void clear() {
        STATES.clear();
        PROFILES.clear();
    }
}
