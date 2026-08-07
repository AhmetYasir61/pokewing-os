# Plugin compatibility notes

Youer 26.2 implements the Paper/Purpur API on top of NeoForge 26.2. Two things about Minecraft 26.x
break plugins that were built for the 1.21 era, and both are handled here.

## 1. Plugins that only understand `1.x` version strings

**Symptom**

```
[ERROR]: Error occurred while enabling ItemsAdder v4.0.16 (Is it up to date?)
java.lang.NullPointerException: Cannot invoke "beer.devs.fastnbt.nms.Version.ordinal()"
  because the return value of "beer.devs.fastnbt.nms.Version.get()" is null
```

**Cause**

Plugins — and the NMS helper libraries they bundle, such as FastNBT / LoneLibs — resolve the running
server by matching `Bukkit.getBukkitVersion()` or `Bukkit.getMinecraftVersion()` against a fixed
table of `1.x` releases. On a `26.2` server nothing matches, the lookup returns `null`, and the
plugin dereferences it during `onEnable`.

**What Youer does**

`com.mohistmc.youer.compat.PluginVersionCompat` reports a legacy version to plugins. By default
`Bukkit.getBukkitVersion()` returns `1.21.5-R0.1-SNAPSHOT` and `Bukkit.getMinecraftVersion()` returns
`1.21.5`. The server logs which version it is reporting just before plugins load.

`Versioning.getCurrentApiVersion()` is deliberately **not** shimmed, so a plugin's declared
`api-version` is still validated against the real `26.2`.

**Configuration** — `youer-config/compat.properties`, or the `youer.compat.legacy-version` system
property, which wins:

```properties
# Report a specific version to plugins
legacy-version=1.21.5

# Or report the real version and let plugins fend for themselves
# legacy-version=false
```

**Limits, worth being clear about:** this gets a plugin past the version check. It does not make the
plugin's NMS code work. Anything that reflects into obfuscated `net.minecraft.server` internals
against 1.21 names will still fail on 26.2 — it just fails later, and more specifically. Reporting a
version the plugin has never seen is also no better than reporting `26.2`, so pick a value the plugin
actually supports. The real fix is a plugin build that targets 26.x.

## 2. Plugins built against Adventure 4.x

**Symptom**

```
java.lang.NoSuchMethodError: 'net.kyori.adventure.text.serializer.gson.GsonComponentSerializer$Builder
  net.kyori.adventure.text.serializer.gson.GsonComponentSerializer$Builder.legacyHoverEventSerializer(
    net.kyori.adventure.text.serializer.gson.LegacyHoverEventSerializer)'
```

**Cause**

Adventure 5.0 removed two pieces of 4.x API:

- the marker interface `net.kyori.adventure.text.serializer.gson.LegacyHoverEventSerializer`, a
  deprecated subtype of `...serializer.json.LegacyHoverEventSerializer`, and
- the `default` overload `GsonComponentSerializer.Builder#legacyHoverEventSerializer(gson.LegacyHoverEventSerializer)`,
  which widened its argument and delegated to the `json` overload.

Youer 26.2 ships Adventure 5.2.0, so a plugin compiled against 4.x hits `NoSuchMethodError` the first
time it builds a component serializer.

**What Youer does**

`buildSrc`'s `AdventureGsonCompatPatcher` rewrites `adventure-text-serializer-gson` on the way into
the server jar, adding the interface back and re-adding the default method with the same body 4.x
had. Everything else in the artifact is copied through unchanged. The launcher extracts the patched
artifact into `libraries/` like any other, so there is nothing to install by hand.

This is verified by `tools/adventure-compat-test/check.sh`, which reproduces the `NoSuchMethodError`
against the stock artifact and then shows the same call succeeding against the patched one. It runs
on every push.

Note this applies to the server jar (`:neoforge:youerJar`). A NeoForge-style installer build resolves
its libraries from Maven at install time and would fetch the stock artifact.

## Building

The build needs **JDK 25**. `gradle/gradle-daemon-jvm.properties` pins the Gradle daemon to it, so
`./gradlew` works regardless of what is on `PATH` — provided a JDK 25 is installed or can be
provisioned. Without that pin a JDK 21 daemon compiles `buildSrc` at source level 21 and fails on the
unnamed `_` variables NeoForge's build plugins use:

```
error: unnamed variables are a preview feature and are disabled by default.
```

```bash
cd youer
./gradlew :neoforge:youerJar
```

The jar lands in `projects/youer/build/libs/`.
