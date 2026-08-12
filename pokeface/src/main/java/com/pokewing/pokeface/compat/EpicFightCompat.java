package com.pokewing.pokeface.compat;

import com.pokewing.pokeface.PokeFace;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Epic Fight access through reflection only. Verified against Epic Fight
 * 20.14.17 (1.20.1 Forge): {@code EpicFightCapabilities.getPlayerPatch(Player)}
 * and {@code PlayerPatch#getPlayerMode()}, whose enum is MINING or BATTLE.
 *
 * <p>Nothing here is required for the mod to work — every lookup degrades to
 * "not in battle mode" when a class or method is missing. Reflection (rather
 * than a compile-time dependency plus mixins) keeps this addon loading across
 * Epic Fight point releases, and it never patches EF's renderer, so it cannot
 * break Epic Fight, RealCamera or a Sinytra Connector setup.
 */
public final class EpicFightCompat {

    private static boolean resolved;
    private static boolean present;
    private static Method getPlayerPatch;
    private static Method getPlayerMode;

    private EpicFightCompat() {
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
        present = ModList.get() != null && ModList.get().isLoaded("epicfight");
        if (!present) {
            PokeFace.LOGGER.info("PokeFace: Epic Fight not installed - combat reactions use vanilla events.");
            return;
        }
        try {
            Class<?> caps = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> playerPatch = Class.forName(
                    "yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch");
            getPlayerPatch = caps.getMethod("getPlayerPatch", Player.class);
            getPlayerMode = playerPatch.getMethod("getPlayerMode");
            PokeFace.LOGGER.info("PokeFace: Epic Fight detected, combat reactions enabled.");
        } catch (Throwable t) {
            PokeFace.LOGGER.warn("PokeFace: Epic Fight present but its internals did not resolve ({}). "
                    + "Combat reactions fall back to vanilla events.", t.toString());
            getPlayerPatch = null;
            getPlayerMode = null;
        }
    }

    /** @return true when the player is in Epic Fight's battle mode right now. */
    public static boolean isInBattleMode(Player player) {
        resolve();
        if (!present || getPlayerPatch == null || getPlayerMode == null || player == null) {
            return false;
        }
        try {
            Object patch = getPlayerPatch.invoke(null, player);
            if (patch == null) {
                return false;
            }
            Object mode = getPlayerMode.invoke(patch);
            return mode != null && !mode.toString().equalsIgnoreCase("MINING");
        } catch (Throwable t) {
            return false;
        }
    }
}
