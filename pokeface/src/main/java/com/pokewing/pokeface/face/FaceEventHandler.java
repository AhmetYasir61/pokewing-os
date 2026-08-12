package com.pokewing.pokeface.face;

import com.pokewing.pokeface.PokeFaceConfig;
import com.pokewing.pokeface.client.PokeFaceClient;
import com.pokewing.pokeface.compat.EpicFightCompat;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;

/**
 * Feeds gameplay events into the local player's {@link FaceDirector}. Only the
 * client's own player is handled — every other player's face arrives over the
 * network, so reactions stay consistent for all viewers.
 */
public final class FaceEventHandler {

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (!isLocalPlayer(event.getEntity())) {
            return;
        }
        Player player = (Player) event.getEntity();
        PokeFaceClient.director().push(
                CombatReactions.onHurt(event.getAmount(), player.getMaxHealth()));
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!isLocalPlayer(event.getEntity())) {
            return;
        }
        PokeFaceClient.director().push(
                CombatReactions.onAttack(EpicFightCompat.isInBattleMode(event.getEntity())));
    }

    private static boolean isLocalPlayer(Object entity) {
        if (FMLEnvironment.dist != Dist.CLIENT || !PokeFaceConfig.enabled()
                || !PokeFaceConfig.combatReactions()) {
            return false;
        }
        return entity instanceof Player p && PokeFaceClient.isLocalPlayer(p);
    }
}
