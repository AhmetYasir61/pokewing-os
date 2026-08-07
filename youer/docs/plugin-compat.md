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

Two things are deliberately **not** shimmed. `Versioning.getCurrentApiVersion()` keeps returning the
real version, so a plugin's declared `api-version` is still validated honestly. So does
`Bukkit.getVersion()` — that is the human-facing string behind `/version` and the startup banner, and
a server that lies there is a server nobody can debug.

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

**Symptoms**

```
java.lang.NoSuchMethodError: 'net.kyori.adventure.text.serializer.gson.GsonComponentSerializer$Builder
  net.kyori.adventure.text.serializer.gson.GsonComponentSerializer$Builder.legacyHoverEventSerializer(
    net.kyori.adventure.text.serializer.gson.LegacyHoverEventSerializer)'

java.lang.NoSuchFieldError: Class net.kyori.adventure.text.serializer.json.JSONOptions$HoverEventValueMode
  does not have member field '...JSONOptions$HoverEventValueMode MODERN_ONLY'
```

**Cause**

Adventure 5 is a breaking release, and Youer 26.2 ships 5.2.0. Call sites resolve by name *and*
descriptor, so a rename or a type change is a hard error at class initialization — nothing the
plugin can guard against. Three of these bite plugins built against 4.x:

| Adventure 4.x | Adventure 5.x |
|---|---|
| `GsonComponentSerializer.Builder#legacyHoverEventSerializer(gson.LegacyHoverEventSerializer)` | only the `json.LegacyHoverEventSerializer` overload survives; the `gson` interface is gone |
| `JSONOptions.HoverEventValueMode.MODERN_ONLY` / `LEGACY_ONLY` / `BOTH` | renamed to `CAMEL_CASE` / `VALUE_FIELD` / `ALL` |
| `ClickEvent.Action` constants, `ClickEvent#clickEvent(Action, String)` | `Action` became a generic class hierarchy, so each constant is typed as its own subclass and the factory takes a `Payload` |

**What Youer does**

`buildSrc`'s `AdventureCompatPatcher` rewrites `adventure-api`,
`adventure-text-serializer-json` and `adventure-text-serializer-gson` on the way into the server jar,
restoring each removed member as a redirect to whatever replaced it. The renames were confirmed
against the two versions' own javadoc, which kept the original `@since` tags. Everything else in each
artifact is copied through unchanged, and a patch is skipped when the artifact already exposes the
4.x member — so running the patcher against an Adventure 4.x jar is a no-op rather than a corruption.

The `ClickEvent.Action` constants are re-declared under their original `Action` type alongside the
5.x fields. Two fields may share a name when their descriptors differ, which the JVM allows and Java
source does not, so both the 4.x and 5.x call sites link against the same instances.

The launcher extracts the patched artifacts into `libraries/` like any other, so there is nothing to
install by hand.

This is verified by `tools/adventure-compat-test/check.sh`, which for each shim compiles a caller
against Adventure 4.17.0, reproduces the failure against the stock artifacts, and shows the same call
succeeding against the patched ones. It runs on every push.

Note this applies to the server jar (`:neoforge:youerJar`). A NeoForge-style installer build resolves
its libraries from Maven at install time and would fetch the stock artifacts.

**Limits.** These shims fix linkage, one member at a time, for the members a plugin actually
references. They do not make a 4.x plugin an Adventure 5 plugin. A plugin that reflects over
`Action.values()`, switches on the old enum, or reaches API this patcher does not cover will still
break. The durable fix is a plugin build that targets Adventure 5.

## Building

The build needs **JDK 25**. `gradle/gradle-daemon-jvm.properties` pins the Gradle daemon to it, so
`./gradlew` works regardless of what is on `PATH` — provided a JDK 25 is installed or can be
provisioned. Without that pin a JDK 21 daemon compiles `buildSrc` at source level 21 and fails on the
unnamed `_` variables NeoForge's build plugins use:

```
error: unnamed variables are a preview feature and are disabled by default.
```

`setup` has to run first. It decompiles Minecraft, applies the patches under `patches/`, and syncs
the result into `projects/youer/src/main/java` — a second source directory the main source set picks
up alongside the NeoForge/Bukkit/Paper sources at the repository root. Skip it and `compileJava` has
no `net.minecraft` on its classpath, which surfaces as thousands of `package net.minecraft.* does not
exist` errors rather than anything that names the real cause.

```bash
cd youer
./gradlew :neoforge:setup      # decompiles Minecraft; slow on a cold cache
./gradlew :neoforge:youerJar
```

The jar lands in `projects/youer/build/libs/`.

The build stamps the jar name, `YouerVersion` and `versions/youer.txt` with the abbreviated commit
id. Running from an extracted archive instead of a clone used to fail configuration with
`One of setGitDir or setWorkTree must be called`, because gradleutils reads that id from git. The
build now checks for a checkout first and falls back to `nogit`; catching the exception was not
enough, since gradleutils' value source failing is itself recorded as a configuration cache problem.
Pass `-Pyouer_build_id=<id>` to stamp something specific.
