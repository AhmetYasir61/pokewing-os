package com.pokewing.efbbs;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Epic Fight -> BBS animation bridge.
 *
 * <p>This mod does NOT hook into Epic Fight's live combat renderer. Instead it
 * reads Epic Fight's loaded {@code AnimationClip} data (per-joint keyframes) and
 * writes it out as Blockbench-format animation JSON that BBS mod can play back
 * on a rigged actor. That keeps it decoupled: it never touches other mods'
 * rendering, and Epic Fight itself is an optional dependency.</p>
 */
@Mod(EpicFightBBSBridge.MOD_ID)
public class EpicFightBBSBridge {
    public static final String MOD_ID = "efbbs";
    public static final Logger LOG = LogUtils.getLogger();

    public EpicFightBBSBridge() {
        MinecraftForge.EVENT_BUS.register(this);
        LOG.info("[efbbs] Epic Fight -> BBS bridge loaded. Epic Fight present: {}",
                isEpicFightLoaded());
    }

    public static boolean isEpicFightLoaded() {
        return ModList.get() != null && ModList.get().isLoaded("epicfight");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ExportCommand.register(event.getDispatcher());
    }
}
