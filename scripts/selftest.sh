#!/usr/bin/env bash
# Boots a dev dedicated server for one band, runs /slashrails selftest over RCON, prints the
# results and stops the server. Exit code 0 only if the self-test passed.
#   scripts/selftest.sh <band>          e.g. 1.21.2-fabric; "fabric"/"neoforge" mean the 1.21.1 bands
# A band without a run/ dir gets one from scripts/dev-server.properties (RCON on, flat world).
# Don't run Gradle builds while this is running: they can kill the dev server's daemon.
set -u
BAND="${1:-fabric}"
case "$BAND" in fabric|neoforge) BAND="1.21.1-$BAND" ;; esac
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/versions/$BAND/run"
LOG="$RUN/selftest-console.log"
if [ ! -f "$RUN/server.properties" ]; then
  mkdir -p "$RUN"
  cp "$ROOT/scripts/dev-server.properties" "$RUN/server.properties"
  echo "eula=true" > "$RUN/eula.txt"
fi
PORT=$(grep '^rcon.port=' "$RUN/server.properties" | cut -d= -f2)
PASS=$(grep '^rcon.password=' "$RUN/server.properties" | cut -d= -f2)
: "${JAVA_HOME:=/c/Users/slash/AppData/Roaming/PrismLauncher/java/java-runtime-delta}"
export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"

rm -rf "$RUN/selftest-world"
( cd "$ROOT" && ./gradlew ${OFFLINE---offline} ":versions:$BAND:runServer" > "$LOG" 2>&1 ) &
SERVER=$!

for _ in $(seq 1 300); do
  grep -qE "RCON running" "$LOG" 2>/dev/null && break
  if grep -qE "BUILD FAILED|Crash|Exception in server tick" "$LOG" 2>/dev/null || ! kill -0 $SERVER 2>/dev/null; then
    echo "server failed to start"; tail -40 "$LOG"; exit 2
  fi
  sleep 2
done
python "$ROOT/scripts/rcon.py" "$PORT" "$PASS" slashrails selftest

for _ in $(seq 1 600); do
  grep -qE "SELFTEST (PASSED|FAILED)" "$LOG" && break
  if grep -qE "Exception in server tick|---- Minecraft Crash Report" "$LOG" || ! kill -0 $SERVER 2>/dev/null; then break; fi
  sleep 2
done
grep -E "\[selftest\]|Exception|at com\.slashrails" "$LOG"
python "$ROOT/scripts/rcon.py" "$PORT" "$PASS" stop >/dev/null 2>&1
wait $SERVER
grep -q "SELFTEST PASSED" "$LOG"
