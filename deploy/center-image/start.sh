#!/bin/sh
set -eu

java -jar /app/center-server.jar &
java_pid=$!

cd /app/center-web/apps/center-web
node server.js &
node_pid=$!

shutdown() {
  kill -TERM "$node_pid" "$java_pid" 2>/dev/null || true
  wait "$node_pid" 2>/dev/null || true
  wait "$java_pid" 2>/dev/null || true
}

trap shutdown INT TERM

status=0
while kill -0 "$java_pid" 2>/dev/null && kill -0 "$node_pid" 2>/dev/null; do
  sleep 1
done

if ! kill -0 "$java_pid" 2>/dev/null; then
  wait "$java_pid" || status=$?
fi

if ! kill -0 "$node_pid" 2>/dev/null; then
  wait "$node_pid" || status=$?
fi

shutdown
exit "$status"
