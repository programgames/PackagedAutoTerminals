#!/usr/bin/env bash
# Starts a dedicated server with `./gradlew runServer`, waits until it is ready, then stops it.
# A client class reached on the server side stops the start with a NoClassDefFoundError: this
# script is the only proof that the jar runs on a dedicated server.
#
#   bash tools/server-smoke.sh                           waits at most 300 seconds
#   SERVER_SMOKE_TIMEOUT=600 bash tools/server-smoke.sh
#
# Exits with 1 when the server is not ready in time, or when run/logs/latest.log carries a
# NoClassDefFoundError or an exception that names packagedautoterminals.

set -u
cd "$(dirname "$0")/.."

limit="${SERVER_SMOKE_TIMEOUT:-300}"
log=run/logs/latest.log

mkdir -p run
# Without it, the server stops at once (DedicatedServer.init).
echo 'eula=true' > run/eula.txt
# A ready line left by an earlier run would end the wait at once.
rm -f "$log"

# ForgeGradle wires the standard input of Gradle to the server console, so `stop` goes there.
# `nogui`: without it, the server opens a window on any machine that has a screen.
# A coproc writes into a pipe that nobody reads here: a full pipe would stall Gradle.
exec 3>&1
coproc SERVER { ./gradlew runServer --args=nogui --no-daemon >&3 2>&3; }
console="${SERVER[1]}"
gradle_pid=$SERVER_PID

timed_out=0
deadline=$((SECONDS + limit))
until grep -q 'Done (' "$log" 2>/dev/null; do
  kill -0 "$gradle_pid" 2>/dev/null || break
  if [ "$SECONDS" -ge "$deadline" ]; then
    timed_out=1
    break
  fi
  sleep 2
done

if kill -0 "$gradle_pid" 2>/dev/null; then
  echo stop >&"$console"
fi
if [ "$timed_out" = 1 ]; then
  # A server stuck in its start never reads `stop`.
  for pid in $("$JAVA_HOME/bin/jps" -l | awk '$2 == "GradleStartServer" { print $1 }'); do
    taskkill //F //PID "$pid" >/dev/null 2>&1 || kill -9 "$pid"
  done
fi
wait "$gradle_pid"

status=0
if [ "$timed_out" = 1 ]; then
  echo "server-smoke: FAILED, the server was not ready after ${limit} seconds." >&2
  status=1
elif ! grep -q 'Done (' "$log" 2>/dev/null; then
  echo "server-smoke: FAILED, the server stopped before it was ready." >&2
  status=1
fi

problems="$(grep -nE 'NoClassDefFoundError|^[[:space:]]+at fr\.julien\.packagedautoterminals\.|(Exception|Error).*packagedautoterminals|packagedautoterminals.*(Exception|Error)' "$log" 2>/dev/null)"
if [ -n "$problems" ]; then
  echo "server-smoke: FAILED, ${log} carries these lines:" >&2
  echo "$problems" >&2
  status=1
fi

[ "$status" = 0 ] && echo "server-smoke: the dedicated server started and stopped."
exit "$status"
