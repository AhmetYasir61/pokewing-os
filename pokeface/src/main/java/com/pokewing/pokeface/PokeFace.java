package com.pokewing.pokeface;

import com.pokewing.pokeface.compat.EpicFightCompat;
import com.pokewing.pokeface.compat.VoiceChatCompat;
import com.pokewing.pokeface.net.PokeFaceNetwork;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PokeFace — facial and eye reactions for Epic Fight characters.
 *
 * <p>The face is driven, in priority order, by a webcam tracker, by gameplay
 * reactions (Epic Fight combat included) and finally by a procedural idle
 * animator, so there is always something on the character's face.
 */
@Mod(PokeFace.MOD_ID)
public final class PokeFace {

    public static final String MOD_ID = "pokeface";
    public static final Logger LOGGER = LoggerFactory.getLogger("PokeFace");

    public PokeFace() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, PokeFaceConfig.SPEC, "pokeface-client.toml");

        modBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(new com.pokewing.pokeface.face.FaceEventHandler());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.pokewing.pokeface.client.PokeFaceClient.init(modBus);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        PokeFaceNetwork.register();
        event.enqueueWork(() -> {
            LOGGER.info("PokeFace ready. Epic Fight: {}, Simple Voice Chat: {}",
                    EpicFightCompat.isLoaded() ? "yes" : "no",
                    VoiceChatCompat.isLoaded() ? "yes" : "no");
        });
    }
}
