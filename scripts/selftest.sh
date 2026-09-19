#!/usr/bin/env bash
# Boots a dev dedicated server for one band, runs /slashtracks selftest over RCON, prints the
# results and stops the server. Exit code 0 only if the self-test passed.
#   scripts/selftest.sh fabric|neoforge
# Needs versions/1.21.1-<loader>/run/server.properties with RCON enabled (see scripts/README).
set -u
LOADER="${1:-fabric}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/versions/1.21.1-$LOADER/run"
LOG="$RUN/selftest-console.log"
PORT=$(grep '^rcon.port=' "$RUN/server.properties" | cut -d= -f2)
PASS=$(grep '^rcon.password=' "$RUN/server.properties" | cut -d= -f2)
: "${JAVA_HOME:=/c/Users/slash/AppData/Roaming/PrismLauncher/java/java-runtime-delta}"
export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"

rm -rf "$RUN/selftest-world"
( cd "$ROOT" && ./gradlew ${OFFLINE---offline} ":versions:1.21.1-$LOADER:runServer" > "$LOG" 2>&1 ) &
SERVER=$!

for _ in $(seq 1 300); do
  grep -qE "RCON running" "$LOG" 2>/dev/null && break
  if grep -qE "BUILD FAILED|Crash|Exception in server tick" "$LOG" 2>/dev/null || ! kill -0 $SERVER 2>/dev/null; then
    echo "server failed to start"; tail -40 "$LOG"; exit 2
  fi
  sleep 2
done
python "$ROOT/scripts/rcon.py" "$PORT" "$PASS" slashtracks selftest

for _ in $(seq 1 600); do
  grep -qE "SELFTEST (PASSED|FAILED)" "$LOG" && break
  if grep -qE "Exception|Crash" "$LOG" || ! kill -0 $SERVER 2>/dev/null; then break; fi
  sleep 2
done
grep -E "\[selftest\]|Exception|at com\.slashtracks" "$LOG"
python "$ROOT/scripts/rcon.py" "$PORT" "$PASS" stop >/dev/null 2>&1
wait $SERVER
grep -q "SELFTEST PASSED" "$LOG"
