package org.bukkit.craftbukkit.util;

import com.mohistmc.youer.compat.PluginVersionCompat;

public final class Versioning {

    // Youer - the version Youer actually implements, before any plugin compat shim
    public static final String REAL_VERSION = "26.2";

    public static String getBukkitVersion() {
        return PluginVersionCompat.bukkitVersion(REAL_VERSION); // Youer - legacy version compat
    }

    public static String getCurrentApiVersion() {
        // Youer - never shimmed: plugin api-version declarations are validated against this
        return REAL_VERSION;
    }
}
