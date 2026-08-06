package com.pokewing.efmocap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Shouldering the dead. Look at a body, press the carry key, and it rides on
 * your back until you set it down — so you can carry the fallen to a grave and
 * bury them on camera.
 *
 * <p>Carrying is stamped into the take you're recording, so when the scene
 * replays, the clone carries the same body at the same moment and the funeral
 * plays back like everything else.</p>
 */
public final class CarrySystem {
    public static final CarrySystem INSTANCE = new CarrySystem();

    /** Take name of the body the local player is carrying ("" = none). */
    private String carriedTake = "";

    private CarrySystem() {}

    public boolean isCarrying() { return !carriedTake.isEmpty(); }
    public String carriedTake() { return carriedTake; }

    /** Pick up the nearest corpse in reach, or set down the one you're holding. */
    public void toggle() {
        if (isCarrying()) {
            drop();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;

        String take = ReplayDirector.INSTANCE.nearestCorpseTake(
                p.getX(), p.getY(), p.getZ(), Settings.carryRange);
        if (take == null) {
            ClientSystems.msg("§7[efmocap] yakında ceset yok");
            return;
        }
        carriedTake = take;
        ReplayDirector.INSTANCE.setCarried(take, true);
        ClientSystems.msg("§a[efmocap] '" + take + "' sırtlandı — bırakmak için tekrar bas");
    }

    public void drop() {
        if (!isCarrying()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p != null) {
            // Lay the body down at the carrier's feet.
            ReplayDirector.INSTANCE.placeCorpse(carriedTake,
                    p.getX(), p.getY(), p.getZ(), p.getYRot() + 90f);
        }
        ReplayDirector.INSTANCE.setCarried(carriedTake, false);
        ClientSystems.msg("§e[efmocap] '" + carriedTake + "' bırakıldı");
        carriedTake = "";
    }

    /** Keep the carried body on the player's back. Called each client tick. */
    public void tick() {
        if (!isCarrying()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) { carriedTake = ""; return; }
        boolean stillThere = ReplayDirector.INSTANCE.carryTo(carriedTake,
                p.getX(), p.getY(), p.getZ(), p.yBodyRot);
        if (!stillThere) carriedTake = "";  // scene restarted and revived them
    }

    public void reset() {
        carriedTake = "";
    }
}
