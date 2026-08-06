package com.pokewing.efmocap;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * EFMocap — Epic Fight-native machinima toolkit. See DESIGN.md for the full
 * architecture and roadmap.
 */
@Mod(EFMocap.MOD_ID)
public class EFMocap {
    public static final String MOD_ID = "efmocap";
    public static final Logger LOG = LogUtils.getLogger();

    public EFMocap() {
        ModEntities.register(FMLJavaModLoadingContext.get().getModEventBus());
        LOG.info("[efmocap] loaded. Epic Fight present: {}", isEpicFightLoaded());
    }

    public static boolean isEpicFightLoaded() {
        return ModList.get() != null && ModList.get().isLoaded("epicfight");
    }
}
