package com.pokewing.efmocap;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registry for EFMocap's entity types (currently just the cinematic camera). */
public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EFMocap.MOD_ID);

    public static final RegistryObject<EntityType<CameraEntity>> CAMERA =
            ENTITIES.register("camera", () -> EntityType.Builder
                    .<CameraEntity>of(CameraEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .noSummon()
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(0)
                    .build("camera"));

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}
