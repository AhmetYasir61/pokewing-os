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
    private final RemotePlayer entity;
    private final int fakeId;
    private MocapFrame prev;

    private CloneActor(RemotePlayer entity, int fakeId) {
        this.entity = entity;
        this.fakeId = fakeId;
    }

    /** Spawn a clone at the given position, or null if no client level. */
    public static CloneActor spawn(String name, double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return null;

        // Empty profile name -> no visible name tag above the clone.
        GameProfile profile = new GameProfile(UUID.randomUUID(), "");
        RemotePlayer clone = new RemotePlayer(level, profile);
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

    public void despawn() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.level.removeEntity(fakeId, Entity.RemovalReason.DISCARDED);
        }
        entity.remove(Entity.RemovalReason.DISCARDED);
    }

    public RemotePlayer entity() { return entity; }
}
