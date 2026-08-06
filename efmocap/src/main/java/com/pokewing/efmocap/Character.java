package com.pokewing.efmocap;

import java.util.ArrayList;
import java.util.List;

/**
 * A castable character: a skin, a body model variant, and any cosmetic
 * attachments. Takes are recorded "as" a character, and clones replaying that
 * take render with it.
 */
public class Character {
    public String name = "character";
    /** PNG filename under {@code config/efmocap/skins} ("" = default skin). */
    public String skin = "";
    /** Alex-style thin arms. */
    public boolean slim = false;
    /**
     * Skin used once this character's corpse has decayed ("" = the bundled
     * skeleton texture). Also a PNG under {@code config/efmocap/skins}.
     */
    public String decaySkin = "";
    public List<Attachment> attachments = new ArrayList<>();

    public Character() {}

    public Character(String name) { this.name = name; }

    public String describe() {
        return name + (skin.isEmpty() ? " (varsayılan skin)" : " [" + skin + "]")
                + (attachments.isEmpty() ? "" : " +" + attachments.size() + " ek");
    }
}
