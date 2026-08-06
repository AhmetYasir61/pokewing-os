package com.pokewing.efmocap;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * A client-side clone actor: a fake {@link RemotePlayer} added to the client
 * level so Epic Fight's client renderer patches and draws it. Each replay tick
 * we set its transform and, when the recorded animation id changes, tell its
 * Epic Fight patch to play that animation. Everything is client-only -- nothing
 * is sent to the server.
 */
public final class CloneActor {
    /** Epic Fight's biped death animation. */
    public static final String DEATH_ANIM = "epicfight:biped/living/death";
    /** Ticks of death animation to play before the body just lies there. */
    private static final int DEATH_ANIM_TICKS = 40;

    private final CloneEntity entity;
    private final int fakeId;
    private MocapFrame prev;
    private boolean dead;
    private int deathAge;
    private boolean carried;

    private CloneActor(CloneEntity entity, int fakeId) {
        this.entity = entity;
        this.fakeId = fakeId;
    }

    /** Spawn a clone cast as {@code character} (may be null for the default look). */
    public static CloneActor spawn(String name, Character character,
                                   double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return null;

        // Empty profile name -> no visible name tag above the clone.
        GameProfile profile = new GameProfile(UUID.randomUUID(), "");
        CloneEntity clone = new CloneEntity(level, profile);
        clone.character = character;
        clone.setCustomNameVisible(false);
        int id = FakeIds.next();
        clone.setId(id);
        clone.setNoGravity(true);
        clone.noPhysics = true;
        clone.setPos(x, y, z);
        clone.setOldPosAndRot();
        level.addPlayer(id, clone);
        EFMocap.LOG.info("[efmocap] spawned clone '{}' id={} at {},{},{}", name, id, x, y, z);
        return new CloneActor(clone, id);
    }

    /**
     * Apply one recorded frame. The renderer draws entities by interpolating
     * "previous tick" -> "current" with the frame's partialTick, so the previous
     * frame is written into the *O / *Old fields and the new frame into the live
     * ones. (Writing the same value into both — as an earlier version did —
     * pins the clone for a whole tick and reads as stuttering.)
     */
    public void apply(MocapFrame f) {
        if (dead) { tickCorpse(); return; }

        MocapFrame p = prev != null ? prev : f;

        // Previous tick state (render lerp source).
        entity.xo = p.x; entity.yo = p.y; entity.zo = p.z;
        entity.xOld = p.x; entity.yOld = p.y; entity.zOld = p.z;
        entity.yRotO = p.yRot; entity.xRotO = p.xRot;
        entity.yHeadRotO = p.yRot;
        entity.yBodyRotO = p.yBodyRot;

        // Current tick state (render lerp target).
        entity.setPos(f.x, f.y, f.z);
        entity.setYRot(f.yRot);
        entity.setYHeadRot(f.yRot);
        entity.yBodyRot = f.yBodyRot;
        entity.setXRot(f.xRot);

        ItemUtil.apply(entity, f);

        // Force the exact recorded animation every tick so Epic Fight's own
        // living motion on the moving clone can't override the replay, and feed
        // the previous elapsed time so the pose interpolates smoothly.
        if (f.animId >= 0) {
            EpicFightBridge.forceAnimation(EpicFightBridge.getPatch(entity),
                    f.animId, p.animId == f.animId ? p.elapsed : f.elapsed, f.elapsed);
        }

        prev = f;
    }

    /** Drop interpolation history (used when a replay loops or is re-seeked). */
    public void resetInterpolation() {
        prev = null;
    }

    public boolean isDead() { return dead; }

    /**
     * Kill the actor: play the death animation from where it stands, then hold
     * the final pose. The clone is deliberately NOT removed — the body stays in
     * frame so the death reads on camera.
     */
    public void die() {
        if (dead) return;
        dead = true;
        deathAge = 0;
        EFMocap.LOG.info("[efmocap] actor died at tick, staying as a corpse");
    }

    /** Bring the actor back for another run of the scene. */
    public void revive() {
        dead = false;
        deathAge = 0;
        carried = false;
        entity.decayed = false;
        prev = null;
    }

    /**
     * Advance the death animation, then freeze it. Position is held wherever the
     * actor fell — the recording keeps running, but a corpse doesn't walk.
     */
    private void tickCorpse() {
        float prevElapsed = Math.min(deathAge, DEATH_ANIM_TICKS) * 0.05f;
        deathAge++;
        float elapsed = Math.min(deathAge, DEATH_ANIM_TICKS) * 0.05f;

        // Flesh goes, bones stay.
        if (!entity.decayed && deathAge >= Settings.decayTicks) {
            entity.decayed = true;
            EFMocap.LOG.info("[efmocap] a corpse decayed to bones");
        }

        // A carried body is positioned by the carrier, not by itself.
        if (!carried) {
            entity.xo = entity.getX(); entity.yo = entity.getY(); entity.zo = entity.getZ();
            entity.xOld = entity.getX(); entity.yOld = entity.getY(); entity.zOld = entity.getZ();
            entity.yRotO = entity.getYRot(); entity.xRotO = entity.getXRot();
            entity.yBodyRotO = entity.yBodyRot;
        }

        int id = EpicFightBridge.animationIdByKey(DEATH_ANIM);
        if (id >= 0) {
            EpicFightBridge.forceAnimation(EpicFightBridge.getPatch(entity), id, prevElapsed, elapsed);
        }
    }

    /**
     * Place a corpse on someone's back. The carrier calls this every tick, so
     * the body rides along and can be set down anywhere — a grave, a cart, a pyre.
     */
    public void setCarriedPose(double carrierX, double carrierY, double carrierZ, float carrierYaw) {
        double yaw = Math.toRadians(carrierYaw);
        // Just behind the carrier's shoulders.
        double bx = carrierX + Math.sin(yaw) * 0.32;
        double bz = carrierZ - Math.cos(yaw) * 0.32;
        double by = carrierY + 1.05;

        entity.xo = entity.getX(); entity.yo = entity.getY(); entity.zo = entity.getZ();
        entity.xOld = entity.getX(); entity.yOld = entity.getY(); entity.zOld = entity.getZ();
        entity.yRotO = entity.getYRot();
        entity.yBodyRotO = entity.yBodyRot;

        entity.setPos(bx, by, bz);
        // Lying across the back, perpendicular to the carrier.
        float across = carrierYaw + 90f;
        entity.setYRot(across);
        entity.setYHeadRot(across);
        entity.yBodyRot = across;
    }

    public void setCarried(boolean c) { carried = c; }
    public boolean isCarried() { return carried; }
    public boolean isDecayed() { return entity.decayed; }

    /** Where the body currently lies — used to drop it and to find it again. */
    public double x() { return entity.getX(); }
    public double y() { return entity.getY(); }
    public double z() { return entity.getZ(); }
    public float yaw() { return entity.yBodyRot; }

    /** Set the body down here, flat on the ground. */
    public void placeAt(double px, double py, double pz, float yaw) {
        entity.setPos(px, py, pz);
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.yBodyRot = yaw;
        entity.xo = px; entity.yo = py; entity.zo = pz;
        entity.xOld = px; entity.yOld = py; entity.zOld = pz;
        entity.yRotO = yaw; entity.yBodyRotO = yaw;
    }

    public void despawn() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.level.removeEntity(fakeId, Entity.RemovalReason.DISCARDED);
        }
        entity.remove(Entity.RemovalReason.DISCARDED);
    }

    public CloneEntity entity() { return entity; }
}
