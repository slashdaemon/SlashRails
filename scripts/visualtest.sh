#!/usr/bin/env bash
# Opens a dev client for one band, runs the scripted VisualTest (fixtures, fixed viewpoints, a ride,
# the tool preview) and quits; screenshots land in <run>/screenshots/slashrails-*.png.
#   scripts/visualtest.sh <band> [world-dir]     e.g. 1.21.5-fabric, 26.3-neoforge
# The world must be a flat world of the band's own Minecraft version: by default the one the band's
# last self-test left in <run>/selftest-world. OPENS A CLIENT WINDOW — ask before running it on a
# shared machine (other sessions drive real input into Minecraft clients).
set -u
BAND="$1"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
case "$BAND" in
  26.*-*)
    MC="${BAND%-*}"; LOADER="${BAND##*-}"
    BUILD_DIR="$ROOT/versions/$MC"; TASK=":band-$LOADER:runVisualTest"; RUN="$BUILD_DIR/band-$LOADER/run"
    JAVA_HOME="${JAVA25_HOME:-/c/Users/slash/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon}" ;;
  *)
    BUILD_DIR="$ROOT"; TASK=":versions:$BAND:runVisualTest"; RUN="$ROOT/versions/$BAND/run" ;;
esac
: "${JAVA_HOME:=/c/Users/slash/AppData/Roaming/PrismLauncher/java/java-runtime-delta}"
export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"

WORLD="${2:-$RUN/selftest-world}"
[ -d "$WORLD" ] || { echo "no world at $WORLD (run scripts/selftest.sh $BAND first)"; exit 2; }
mkdir -p "$RUN/saves" "$RUN/screenshots"
rm -rf "$RUN/saves/visual-world"
cp -r "$WORLD" "$RUN/saves/visual-world"
rm -f "$RUN/saves/visual-world/session.lock"
rm -f "$RUN"/screenshots/slashrails-*.png
# An unfocused window would pause singleplayer; VisualTest also forces this in-game.
if [ -f "$RUN/options.txt" ]; then
  sed -i 's/^pauseOnLostFocus:.*/pauseOnLostFocus:false/' "$RUN/options.txt"
else
  echo "pauseOnLostFocus:false" > "$RUN/options.txt"
fi

LOG="$RUN/visualtest-console.log"
( cd "$BUILD_DIR" && ./gradlew ${OFFLINE---offline} "$TASK" > "$LOG" 2>&1 )
grep -E "\[visualtest\]|Exception|Mixin apply|FAILED" "$LOG" | grep -v "^\s*at " | head -40
ls "$RUN"/screenshots/slashrails-*.png 2>/dev/null
