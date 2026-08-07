package com.mohistmc.youer.compat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Reports a legacy {@code 1.x} version string to plugins that cannot parse the {@code 26.x} scheme
 * Minecraft moved to after 1.21.
 *
 * <p>A large amount of plugin code — and the NMS helper libraries plugins bundle, such as FastNBT /
 * LoneLibs — resolves the running server by matching {@link org.bukkit.Bukkit#getBukkitVersion()} or
 * {@link org.bukkit.Bukkit#getMinecraftVersion()} against a fixed table of {@code 1.x} releases. On a
 * {@code 26.2} server nothing matches, the lookup yields {@code null}, and the plugin dies during
 * {@code onEnable} with a NullPointerException it never guards against. ItemsAdder 4.0.16 fails this
 * way at {@code beer.devs.fastnbt.nms.Version.get()}.
 *
 * <p>When enabled, this reports {@link #legacyVersion()} in place of the real one. Plugin
 * {@code api-version} validation is deliberately left alone: {@code Versioning.getCurrentApiVersion()}
 * keeps returning the true version so declared API levels are still checked honestly.
 *
 * <p>Configuration is read from a system property first, then {@code youer-config/compat.properties}:
 * <ul>
 *   <li>{@code youer.compat.legacy-version} / {@code legacy-version} — the version to report, or
 *       {@code false} / {@code off} / {@code none} to report the real version instead.</li>
 * </ul>
 *
 * <p>This runs while {@code CraftServer}'s fields are still initializing, long before the Bukkit
 * configuration system exists, so it touches nothing but the JDK.
 */
public final class PluginVersionCompat {
    /** Reported when the compat layer is on and nothing else is configured. */
    public static final String DEFAULT_LEGACY_VERSION = "1.21.5";

    private static final String PROPERTY = "youer.compat.legacy-version";
    private static final String KEY = "legacy-version";
    private static final Path CONFIG = Path.of("youer-config", "compat.properties");

    private static volatile String legacyVersion;
    private static volatile boolean resolved;

    private PluginVersionCompat() {}

    /**
     * The {@code 1.x} version reported to plugins, or {@code null} when the compat layer is disabled
     * and the real version should be used.
     */
    public static String legacyVersion() {
        if (!resolved) {
            synchronized (PluginVersionCompat.class) {
                if (!resolved) {
                    legacyVersion = resolve();
                    resolved = true;
                }
            }
        }
        return legacyVersion;
    }

    /** Whether plugins are being told a legacy version rather than the real one. */
    public static boolean enabled() {
        return legacyVersion() != null;
    }

    /**
     * The Bukkit version string plugins see, e.g. {@code 1.21.5-R0.1-SNAPSHOT}.
     *
     * @param real the version Youer actually implements, returned when compat is off
     */
    public static String bukkitVersion(String real) {
        String legacy = legacyVersion();
        return legacy == null ? real : legacy + "-R0.1-SNAPSHOT";
    }

    /**
     * The Minecraft version string plugins see, e.g. {@code 1.21.5}.
     *
     * @param real the version the server is actually running, returned when compat is off
     */
    public static String minecraftVersion(String real) {
        String legacy = legacyVersion();
        return legacy == null ? real : legacy;
    }

    private static String resolve() {
        String configured = System.getProperty(PROPERTY);
        if (configured == null) {
            configured = fromConfigFile();
        }
        if (configured == null) {
            return DEFAULT_LEGACY_VERSION;
        }

        configured = configured.trim();
        return switch (configured.toLowerCase(Locale.ROOT)) {
            case "false", "off", "none", "disabled", "" -> null;
            case "true", "on", "default", "enabled" -> DEFAULT_LEGACY_VERSION;
            default -> configured;
        };
    }

    private static String fromConfigFile() {
        if (!Files.isRegularFile(CONFIG)) {
            return null;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG)) {
            properties.load(in);
        } catch (IOException e) {
            // Nothing usable to log to this early; fall through to the default.
            return null;
        }
        return properties.getProperty(KEY);
    }
}
