package com.pokewing.efmocap;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * EFMocap — Epic Fight-native machinima toolkit. See DESIGN.md for the full
 * architecture and roadmap. This class is the mod entry point; feature systems
 * (recorder, clone actors, camera, export) register themselves as they land.
 */
@Mod(EFMocap.MOD_ID)
public class EFMocap {
    public static final String MOD_ID = "efmocap";
    public static final Logger LOG = LogUtils.getLogger();

    public EFMocap() {
        LOG.info("[efmocap] loaded. Epic Fight present: {}", isEpicFightLoaded());
        // Phase 1 client systems are wired up in ClientSystems (Dist.CLIENT).
    }

    public static boolean isEpicFightLoaded() {
        return ModList.get() != null && ModList.get().isLoaded("epicfight");
    }
}
