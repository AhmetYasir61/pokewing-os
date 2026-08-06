package com.pokewing.efmocap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

/**
 * Invisible, client-only entity used as the cinematic camera. Minecraft renders
 * the world from {@code Minecraft.setCameraEntity(...)}, so flying this entity
 * along a path flies the camera. Eye height is 0 so the view sits exactly on the
 * recorded keyframe position.
 */
public class CameraEntity extends Entity {
    public float roll;

    public CameraEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions dims) {
        return 0.0F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return false;
    }

    /** Never tick physics -- the director sets the transform explicitly. */
    @Override
    public void tick() {}
}
