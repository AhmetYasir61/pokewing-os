package com.pokewing.pokeface.face;

import net.minecraft.network.FriendlyByteBuf;

/**
 * One OBJ model bolted onto the player: ears on the head, a tail on the body,
 * and so on.
 *
 * <p>Offsets are in model pixels (1/16 block) relative to the anchor bone's
 * pivot, rotations in degrees, so a model exported at Minecraft's own scale
 * drops in without arithmetic. Only this placement travels over the network —
 * the geometry itself is a local file, see {@code ModelLibrary}.
 */
public final class Attachment {

    public enum Anchor {
        HEAD, BODY;

        public Anchor next() {
            return this == HEAD ? BODY : HEAD;
        }

        public String translationKey() {
            return "pokeface.anchor." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public String model = "";
    public Anchor anchor = Anchor.HEAD;
    public float offsetX;
    public float offsetY;
    public float offsetZ;
    public float rotateX;
    public float rotateY;
    public float rotateZ;
    public float scale = 1.0F;
    public int tint = 0xFFFFFFFF;
    /** Draw fullbright, ignoring world light — the same treatment as the eyes. */
    public boolean glow;
    public boolean visible = true;

    public Attachment() {
    }

    public Attachment(String model, Anchor anchor) {
        this.model = model;
        this.anchor = anchor;
    }

    public Attachment copy() {
        Attachment a = new Attachment(this.model, this.anchor);
        a.offsetX = this.offsetX;
        a.offsetY = this.offsetY;
        a.offsetZ = this.offsetZ;
        a.rotateX = this.rotateX;
        a.rotateY = this.rotateY;
        a.rotateZ = this.rotateZ;
        a.scale = this.scale;
        a.tint = this.tint;
        a.glow = this.glow;
        a.visible = this.visible;
        return a;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.model, 64);
        buf.writeByte(this.anchor.ordinal());
        buf.writeFloat(this.offsetX);
        buf.writeFloat(this.offsetY);
        buf.writeFloat(this.offsetZ);
        buf.writeFloat(this.rotateX);
        buf.writeFloat(this.rotateY);
        buf.writeFloat(this.rotateZ);
        buf.writeFloat(this.scale);
        buf.writeInt(this.tint);
        buf.writeBoolean(this.glow);
        buf.writeBoolean(this.visible);
    }

    public static Attachment read(FriendlyByteBuf buf) {
        Attachment a = new Attachment();
        a.model = buf.readUtf(64);
        Anchor[] anchors = Anchor.values();
        a.anchor = anchors[(buf.readByte() & 0xFF) % anchors.length];
        a.offsetX = buf.readFloat();
        a.offsetY = buf.readFloat();
        a.offsetZ = buf.readFloat();
        a.rotateX = buf.readFloat();
        a.rotateY = buf.readFloat();
        a.rotateZ = buf.readFloat();
        a.scale = buf.readFloat();
        a.tint = buf.readInt();
        a.glow = buf.readBoolean();
        a.visible = buf.readBoolean();
        return a;
    }
}
