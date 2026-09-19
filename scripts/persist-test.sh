#!/usr/bin/env bash
# Smoothed runs must survive a server restart: smooth one run, stop, start again, count runs.
#   scripts/persist-test.sh fabric|neoforge        (headless dev server, RCON as in selftest.sh)
set -u
LOADER="${1:-fabric}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/versions/1.21.1-$LOADER/run"
LOG="$RUN/persist-console.log"
PORT=$(grep '^rcon.port=' "$RUN/server.properties" | cut -d= -f2)
PASS=$(grep '^rcon.password=' "$RUN/server.properties" | cut -d= -f2)
: "${JAVA_HOME:=/c/Users/slash/AppData/Roaming/PrismLauncher/java/java-runtime-delta}"
export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"
rcon() { python "$ROOT/scripts/rcon.py" "$PORT" "$PASS" "$@"; }

boot() {
  ( cd "$ROOT" && ./gradlew ${OFFLINE---offline} ":versions:1.21.1-$LOADER:runServer" > "$LOG" 2>&1 ) &
  SERVER=$!
  for _ in $(seq 1 300); do
    grep -q "RCON running" "$LOG" 2>/dev/null && return 0
    kill -0 $SERVER 2>/dev/null || { echo "server exited"; tail -30 "$LOG"; exit 2; }
    sleep 2
  done
  echo "server did not start"; exit 2
}
halt() { rcon stop >/dev/null 2>&1; wait $SERVER; }

rm -rf "$RUN/selftest-world"
boot
echo "before restart: $(rcon execute positioned 0 -60 0 run slashtracks testtrack arc 12 smooth)"
BEFORE=$(rcon slashtracks list)
echo "before restart: $BEFORE"
rcon save-all flush >/dev/null
halt
boot
AFTER=$(rcon slashtracks list)
echo "after restart:  $AFTER"
halt
[ "$BEFORE" = "$AFTER" ] && [[ "$AFTER" == 1\ smoothed* ]] && echo "PERSIST PASSED" || { echo "PERSIST FAILED"; exit 1; }
