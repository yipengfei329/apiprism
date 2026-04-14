#!/usr/bin/env bash
set -euo pipefail

java -jar /app/center-server.jar &
java_pid=$!

cd /app/center-web
node server.js &
node_pid=$!

shutdown() {
  kill -TERM "$node_pid" "$java_pid" 2>/dev/null || true
  wait "$node_pid" 2>/dev/null || true
  wait "$java_pid" 2>/dev/null || true
}

trap shutdown INT TERM

set +e
wait -n "$java_pid" "$node_pid"
status=$?
set -e

shutdown
exit "$status"
