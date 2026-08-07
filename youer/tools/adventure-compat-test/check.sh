#!/usr/bin/env bash
#
# Regression test for the Adventure 4.x compatibility patches.
#
# For each shim it:
#   1. compiles a caller against Adventure 4.17.0, so the call sites carry the 4.x descriptors
#      the way a plugin like ItemsAdder does
#   2. asserts the caller fails against the stock Adventure 5 artifacts (the bug being fixed)
#   3. asserts it succeeds against the patched ones
#
# Run from anywhere: tools/adventure-compat-test/check.sh
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root="$(cd "$here/../.." && pwd)"
work="$(mktemp -d "${TMPDIR:-/tmp}/youer-adventure-compat.XXXXXX")"
trap 'rm -rf "$work"' EXIT

# Versions track projects/youer/build.gradle (adventure) and gradle.properties (asm).
new="$(sed -n 's/.*libraries("net.kyori:adventure-api:\([^"]*\)").*/\1/p' "$root/projects/youer/build.gradle" | head -1)"
old=4.17.0
asm="$(sed -n 's/^asm_version=//p' "$root/gradle.properties" | head -1)"
central=https://repo1.maven.org/maven2

fetch() { curl -sSfL -o "$2" "$central/$1"; }
kyori() { fetch "net/kyori/$1/$2/$1-$2.jar" "$3"; }

echo "==> Adventure under test: $new (compat baseline $old, asm $asm)"
mkdir -p "$work/old" "$work/new" "$work/stock" "$work/patched"
fetch "org/ow2/asm/asm/$asm/asm-$asm.jar" "$work/asm.jar"
fetch "com/google/code/gson/gson/2.11.0/gson-2.11.0.jar" "$work/old/gsonlib.jar"
cp "$work/old/gsonlib.jar" "$work/new/gsonlib.jar"

# Compile-time classpath: Adventure 4.x, where every member under test still exists.
for m in adventure-api adventure-key adventure-text-serializer-json adventure-text-serializer-gson; do
  kyori "$m" "$old" "$work/old/$m.jar"
done
kyori examination-api 1.3.0 "$work/old/examination.jar"
kyori option 1.0.0 "$work/old/option.jar"

# Runtime classpath: Adventure 5.x. The three artifacts the patcher touches are kept separate so each
# can be swapped between its stock and patched copy.
for m in adventure-key adventure-nbt adventure-text-serializer-commons; do
  kyori "$m" "$new" "$work/new/$m.jar"
done
kyori examination-api 1.3.0 "$work/new/examination.jar"
kyori option 1.1.0 "$work/new/option.jar"
fetch "org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar" "$work/new/jspecify.jar"
for m in adventure-api adventure-text-serializer-json adventure-text-serializer-gson; do
  kyori "$m" "$new" "$work/stock/$m.jar"
done

echo "==> Building the patcher"
mkdir -p "$work/patcher"
javac -cp "$work/asm.jar" -d "$work/patcher" \
  "$root/buildSrc/src/main/java/com/mohistmc/youer/gradle/AdventureCompatPatcher.java"

patch_module() { # <module> <artifact>
  java -cp "$work/patcher:$work/asm.jar" com.mohistmc.youer.gradle.AdventureCompatPatcher \
    "$1" "$work/stock/$2.jar" "$work/patched/$2.jar" >/dev/null
}
patch_module API adventure-api
patch_module JSON_SERIALIZER adventure-text-serializer-json
patch_module GSON_SERIALIZER adventure-text-serializer-gson

cp4="$(printf '%s:' "$work"/old/*.jar)"
base5="$(printf '%s:' "$work"/new/*.jar)"

run_case() { # <caller class> <expected failure substring>
  local caller="$1" expected="$2"
  echo
  echo "==> $caller"
  mkdir -p "$work/$caller"
  javac -nowarn -cp "$cp4" -d "$work/$caller" "$here/$caller.java"

  local stock="$base5$work/stock/adventure-api.jar:$work/stock/adventure-text-serializer-json.jar:$work/stock/adventure-text-serializer-gson.jar"
  local patched="$base5$work/patched/adventure-api.jar:$work/patched/adventure-text-serializer-json.jar:$work/patched/adventure-text-serializer-gson.jar"

  if java -cp "$work/$caller:$stock" "$caller" >"$work/$caller.log" 2>&1; then
    echo "FAIL: expected $expected against stock adventure $new, but it succeeded" >&2
    exit 1
  fi
  if ! grep -q "$expected" "$work/$caller.log"; then
    echo "FAIL: stock artifacts failed for an unexpected reason:" >&2
    cat "$work/$caller.log" >&2
    exit 1
  fi
  echo "    reproduced: $(grep -m1 "$expected" "$work/$caller.log" | sed 's/^[[:space:]]*//')"

  if ! java -cp "$work/$caller:$patched" "$caller"; then
    echo "FAIL: patched adventure $new still rejects the 4.x call sites" >&2
    exit 1
  fi
}

run_case LegacyHoverEventCaller NoSuchMethodError
run_case AdventureApiCaller NoSuchFieldError

echo
echo "==> PASS"
