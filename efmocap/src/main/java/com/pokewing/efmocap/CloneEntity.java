package com.pokewing.efmocap;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * The clone body. Subclassing {@link RemotePlayer} keeps it an
 * {@code AbstractClientPlayer}, so Epic Fight still patches and animates it,
 * while letting a character override the skin and arm model.
 */
public class CloneEntity extends RemotePlayer {
    /** Character this clone is cast as (may be null). */
    public Character character;

    public CloneEntity(ClientLevel level, GameProfile profile) {
        super(level, profile);
    }

    @Override
    public ResourceLocation getSkinTextureLocation() {
        if (character != null && !character.skin.isEmpty()) {
            ResourceLocation rl = CharacterLibrary.INSTANCE.texture(character.skin);
            if (rl != null) return rl;
        }
        return super.getSkinTextureLocation();
    }

    @Override
    public String getModelName() {
        if (character != null) return character.slim ? "slim" : "default";
        return super.getModelName();
    }
}
