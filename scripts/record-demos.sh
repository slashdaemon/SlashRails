#!/usr/bin/env bash
# Records the store-gallery demo takes, one client launch per scene, then builds the gallery assets.
# Opens a 1920x1080 client window per scene — ask other sessions first (see CLAUDE.md).
#   scripts/record-demos.sh [scene ...]      default: click ride-vanilla ride-smooth preview
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FF="${FFMPEG:-/c/Users/slash/tools/ffmpeg/bin/ffmpeg.exe}"
R="$ROOT/versions/1.21.1-fabric/run-demo"
: "${JAVA_HOME:=/c/Users/slash/AppData/Roaming/PrismLauncher/java/java-runtime-delta}"
export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"
SCENES=("$@"); [ ${#SCENES[@]} -eq 0 ] && SCENES=(click ride-vanilla ride-smooth preview)
FFW="$(cygpath -m "$FF" 2>/dev/null || echo "$FF")"

for scene in "${SCENES[@]}"; do
  # Fresh copy of the stage world for every take, so each starts identically.
  rm -rf "$R/saves/demo-world"
  cp -r "$R/stage-world" "$R/saves/demo-world"
  rm -f "$R/saves/demo-world/session.lock"
  echo "=== $scene"
  ( cd "$ROOT" && ./gradlew ${OFFLINE---offline} :versions:1.21.1-fabric:runDemo -Pdemo="$scene" -Pffmpeg="$FFW" ) \
    > "$R/record-$scene.log" 2>&1
  grep -aE "\[demo\]" "$R/record-$scene.log" | sed 's/.*\[demo\] /  /'
done
"$ROOT/scripts/make-gallery.sh" "$FF"
