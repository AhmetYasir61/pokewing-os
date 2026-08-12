package com.pokewing.pokeface.face;

import com.pokewing.pokeface.PokeFace;
import com.pokewing.pokeface.PokeFaceConfig;
import com.pokewing.pokeface.compat.EpicFightCompat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;

/**
 * Turns gameplay state into reactions: taking a hit hurts, landing an Epic Fight
 * combo looks focused, low health looks angry, and so on. This is what plays
 * when no webcam is configured, and it also colours the idle mood bias.
 */
public final class CombatReactions {

    public static final int PRIORITY_MANUAL = 100;
    public static final int PRIORITY_HURT = 80;
    public static final int PRIORITY_ATTACK = 60;
    public static final int PRIORITY_AMBIENT = 20;

    private CombatReactions() {
    }

    public static ReactionTrigger onHurt(float damage, float maxHealth) {
        float severity = FaceState.clamp(damage / Math.max(1.0F, maxHealth * 0.35F), 0.25F, 1.0F);
        return new ReactionTrigger(Expression.HURT, severity, 14 + (int) (severity * 20), PRIORITY_HURT);
    }

    public static ReactionTrigger onAttack(boolean epicFightCombo) {
        // Epic Fight combos read as "committed"; a plain swing is just focus.
        Expression e = epicFightCombo ? Expression.ANGRY : Expression.FOCUSED;
        return new ReactionTrigger(e, epicFightCombo ? 0.85F : 0.5F, epicFightCombo ? 30 : 16, PRIORITY_ATTACK);
    }

    /**
     * The slow-moving background mood the idle animator drifts toward. Epic Fight
     * combat mode is treated as "in a fight" even before any damage lands.
     */
    public static Expression moodFor(Player player) {
        if (player == null) {
            return Expression.NEUTRAL;
        }
        float healthFraction = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
        if (healthFraction < 0.3F) {
            return Expression.HURT;
        }
        if (EpicFightCompat.isInBattleMode(player) || hasNearbyThreat(player)) {
            return Expression.ANGRY;
        }
        if (player.getFoodData().getFoodLevel() <= 6) {
            return Expression.TIRED;
        }
        return Expression.NEUTRAL;
    }

    private static boolean hasNearbyThreat(Player player) {
        double r = PokeFaceConfig.threatRadius();
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(r))) {
            if (e != player && e instanceof net.minecraft.world.entity.monster.Enemy) {
                return true;
            }
        }
        return false;
    }

    public static void logOnce(String message) {
        PokeFace.LOGGER.info("PokeFace: {}", message);
    }
}
