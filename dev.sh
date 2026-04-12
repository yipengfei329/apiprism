#!/usr/bin/env bash
# 本地开发启动脚本，按依赖顺序启动所有服务
# 启动顺序: center-server → (center-web + java-demo-service)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$ROOT_DIR/.dev-logs"
mkdir -p "$LOG_DIR"

# 子进程 PID 列表，用于退出时清理
PIDS=()

cleanup() {
  echo ""
  echo "[dev] Shutting down all services..."
  for pid in "${PIDS[@]}"; do
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
    fi
  done
  wait 2>/dev/null || true
  echo "[dev] All services stopped."
}
trap cleanup EXIT INT TERM

# 等待 HTTP 端口就绪
wait_for_port() {
  local port=$1
  local name=$2
  local max_attempts=${3:-60}
  local attempt=0
  echo "[dev] Waiting for $name on port $port..."
  while ! curl -sf "http://localhost:$port/actuator/health" >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [ $attempt -ge $max_attempts ]; then
      echo "[dev] ERROR: $name failed to start within $((max_attempts * 2))s"
      echo "[dev] Check logs: $LOG_DIR/${name}.log"
      exit 1
    fi
    sleep 2
  done
  echo "[dev] $name is ready on port $port"
}

echo "========================================"
echo "  APIPrism Dev Environment"
echo "========================================"
echo ""

# --- 阶段 1: 启动 center-server ---
echo "[dev] Starting center-server (port 8080)..."
"$ROOT_DIR/gradlew" -p "$ROOT_DIR" :apps:center-server:bootRun --no-daemon \
  > "$LOG_DIR/center-server.log" 2>&1 &
PIDS+=($!)

wait_for_port 8080 "center-server" 90

# --- 阶段 2: center-server 就绪后，并行启动 center-web 和 demo-service ---
echo "[dev] Starting center-web (port 3000)..."
(cd "$ROOT_DIR/apps/center-web" && pnpm dev) \
  > "$LOG_DIR/center-web.log" 2>&1 &
PIDS+=($!)

echo "[dev] Starting java-demo-service (port 8081)..."
"$ROOT_DIR/gradlew" -p "$ROOT_DIR" :examples:java-demo-service:bootRun --no-daemon \
  > "$LOG_DIR/java-demo-service.log" 2>&1 &
PIDS+=($!)

wait_for_port 8081 "java-demo-service" 90

# 等待 center-web 启动（它没有 actuator，用简单 HTTP 检查）
echo "[dev] Waiting for center-web on port 3000..."
attempt=0
while ! curl -sf "http://localhost:3000" >/dev/null 2>&1; do
  attempt=$((attempt + 1))
  if [ $attempt -ge 30 ]; then
    echo "[dev] WARNING: center-web may not be ready yet, check logs: $LOG_DIR/center-web.log"
    break
  fi
  sleep 2
done
if [ $attempt -lt 30 ]; then
  echo "[dev] center-web is ready on port 3000"
fi

echo ""
echo "========================================"
echo "  All services started!"
echo "  center-server:     http://localhost:8080"
echo "  center-web:        http://localhost:3000"
echo "  java-demo-service: http://localhost:8081"
echo "  Logs: $LOG_DIR/"
echo "  Press Ctrl+C to stop all services"
echo "========================================"

# 保持脚本运行，等待任意子进程退出
wait
