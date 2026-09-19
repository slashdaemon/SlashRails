#!/usr/bin/env bash
# Multiplayer sync check (Fabric): a real client joins the dev server and logs how many smoothed
# runs it has. Checks the join snapshot, a live add broadcast and a live remove broadcast.
# Opens a client window — ask other sessions before running (see CLAUDE.md).
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/versions/1.21.1-fabric/run"
SLOG="$RUN/mp-server.log"; CLOG="$ROOT/versions/1.21.1-fabric/run-client/mp-client.log"
PORT=$(grep '^rcon.port=' "$RUN/server.properties" | cut -d= -f2)
PASS=$(grep '^rcon.password=' "$RUN/server.properties" | cut -d= -f2)
: "${JAVA_HOME:=/c/Users/slash/AppData/Roaming/PrismLauncher/java/java-runtime-delta}"
export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"
rcon() { python "$ROOT/scripts/rcon.py" "$PORT" "$PASS" "$@"; }
waitfor() { # file pattern seconds
  for _ in $(seq 1 "$3"); do grep -qE "$2" "$1" 2>/dev/null && return 0; sleep 1; done
  echo "TIMEOUT waiting for: $2"; return 1
}
stop_client() {
  powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe' OR Name='javaw.exe'\" | Where-Object { \$_.CommandLine -match 'slashtracks.mptest' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }" >/dev/null 2>&1
}
finish() { rcon stop >/dev/null 2>&1; stop_client; wait; exit "$1"; }

rm -rf "$RUN/selftest-world"
( cd "$ROOT" && ./gradlew ${OFFLINE---offline} :versions:1.21.1-fabric:runServer > "$SLOG" 2>&1 ) &
waitfor "$SLOG" "RCON running" 300 || finish 2
echo "A: $(rcon execute positioned 0 -60 0 run slashtracks testtrack arc 12 smooth)"

( cd "$ROOT" && ./gradlew ${OFFLINE---offline} :versions:1.21.1-fabric:runMpTest > "$CLOG" 2>&1 ) &
waitfor "$CLOG" "\[mptest\] runs=1 " 300 || finish 1
echo "join snapshot: $(grep -o '\[mptest\] runs=.*' "$CLOG" | tail -1)"
rcon gamemode spectator SlashTester >/dev/null
rcon tp SlashTester 14 -38 18 0 90 >/dev/null

echo "B: $(rcon execute positioned 0 -60 30 run slashtracks testtrack staircase 3 smooth)"
waitfor "$CLOG" "\[mptest\] runs=2 " 60 || finish 1
echo "live add: $(grep -o '\[mptest\] runs=.*' "$CLOG" | tail -1)"
waitfor "$CLOG" "slashtracks-mp-0.png" 30

rcon setblock 5 -60 30 minecraft:air >/dev/null
waitfor "$CLOG" "\[mptest\] runs=1 ids=1:" 60 || finish 1
echo "live remove: $(grep -o '\[mptest\] runs=.*' "$CLOG" | tail -1)"
waitfor "$CLOG" "slashtracks-mp-1.png" 30
echo "MP PASSED"
finish 0
