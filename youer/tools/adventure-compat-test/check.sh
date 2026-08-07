#!/usr/bin/env bash
#
# Regression test for the Adventure gson legacy-hover compat patch.
#
#   1. builds AdventureGsonCompatPatcher from buildSrc
#   2. patches the real adventure-text-serializer-gson artifact Youer bundles
#   3. compiles LegacyHoverEventCaller against Adventure 4.17.0, so its call site carries the
#      gson-package descriptor that ItemsAdder 4.0.16 uses
#   4. asserts the caller fails against the stock artifact and succeeds against the patched one
#
# Run from anywhere: tools/adventure-compat-test/check.sh
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root="$(cd "$here/../.." && pwd)"
work="${TMPDIR:-/tmp}/youer-adventure-compat.$$"
mkdir -p "$work"
trap 'rm -rf "$work"' EXIT

# Versions must track projects/youer/build.gradle (adventure) and gradle.properties (asm).
adventure_new="$(sed -n 's/.*libraries("net.kyori:adventure-text-serializer-gson:\([^"]*\)").*/\1/p' "$root/projects/youer/build.gradle" | head -1)"
adventure_old=4.17.0
asm="$(sed -n 's/^asm_version=//p' "$root/gradle.properties" | head -1)"
central=https://repo1.maven.org/maven2

fetch() { # <maven path> <dest>
  curl -sSfL -o "$2" "$central/$1"
}

echo "==> Adventure under test: $adventure_new (compat baseline $adventure_old, asm $asm)"

mkdir -p "$work/libs4" "$work/libs5"
fetch "org/ow2/asm/asm/$asm/asm-$asm.jar" "$work/asm.jar"

for spec in \
  "adventure-api:$adventure_old:libs4/api.jar" \
  "adventure-key:$adventure_old:libs4/key.jar" \
  "adventure-text-serializer-json:$adventure_old:libs4/json.jar" \
  "adventure-text-serializer-gson:$adventure_old:libs4/gson.jar" \
  "examination-api:1.3.0:libs4/examination.jar" \
  "option:1.0.0:libs4/option.jar" \
  "adventure-api:$adventure_new:libs5/api.jar" \
  "adventure-key:$adventure_new:libs5/key.jar" \
  "adventure-text-serializer-json:$adventure_new:libs5/json.jar" \
  "adventure-text-serializer-commons:$adventure_new:libs5/commons.jar" \
  "examination-api:1.3.0:libs5/examination.jar" \
  "option:1.1.0:libs5/option.jar"
do
  IFS=: read -r name version dest <<<"$spec"
  group=net/kyori
  fetch "$group/$name/$version/$name-$version.jar" "$work/$dest"
done

fetch "com/google/code/gson/gson/2.11.0/gson-2.11.0.jar" "$work/libs4/gsonlib.jar"
cp "$work/libs4/gsonlib.jar" "$work/libs5/gsonlib.jar"
fetch "net/kyori/adventure-text-serializer-gson/$adventure_new/adventure-text-serializer-gson-$adventure_new.jar" "$work/stock.jar"

echo "==> Building the patcher and patching the artifact"
mkdir -p "$work/patcher"
javac -cp "$work/asm.jar" -d "$work/patcher" \
  "$root/buildSrc/src/main/java/com/mohistmc/youer/gradle/AdventureGsonCompatPatcher.java"
java -cp "$work/patcher:$work/asm.jar" \
  com.mohistmc.youer.gradle.AdventureGsonCompatPatcher "$work/stock.jar" "$work/patched.jar"

echo "==> Compiling the ItemsAdder-style caller against Adventure $adventure_old"
cp4="$(printf '%s:' "$work"/libs4/*.jar)"
cp5="$(printf '%s:' "$work"/libs5/*.jar)"
mkdir -p "$work/caller"
javac -nowarn -cp "$cp4" -d "$work/caller" "$here/LegacyHoverEventCaller.java"

echo "==> Stock artifact must still fail (this is the bug being fixed)"
if java -cp "$work/caller:$cp5$work/stock.jar" LegacyHoverEventCaller >"$work/stock.log" 2>&1; then
  echo "FAIL: expected NoSuchMethodError against stock adventure $adventure_new, but it succeeded" >&2
  exit 1
fi
if ! grep -q "NoSuchMethodError" "$work/stock.log"; then
  echo "FAIL: stock artifact failed for an unexpected reason:" >&2
  cat "$work/stock.log" >&2
  exit 1
fi
echo "    reproduced: $(grep -m1 NoSuchMethodError "$work/stock.log")"

echo "==> Patched artifact must succeed"
if ! java -cp "$work/caller:$cp5$work/patched.jar" LegacyHoverEventCaller; then
  echo "FAIL: patched adventure $adventure_new still rejects the legacy call" >&2
  exit 1
fi

echo "==> PASS"
