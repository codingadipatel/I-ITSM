#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ ! -f .env ]; then python3 scripts/setup-env.py; fi
set -a; source .env; set +a
python3 - <<'PY'
import socket
for port in (8080,5173):
    with socket.socket() as sock:
        if sock.connect_ex(('127.0.0.1',port))==0:
            raise SystemExit(f'Port {port} is already in use. Stop the existing application first.')
PY
./scripts/local-db.sh start
mvn -q -f backend/pom.xml -DskipTests package
if [ ! -d frontend/node_modules ]; then npm --prefix frontend ci; fi
cp backend/target/i-itsm-1.0.0.jar .runtime/application.jar
JAVA_BIN=java
if [ -n "${JAVA_HOME:-}" ]; then JAVA_BIN="$JAVA_HOME/bin/java"; fi
nohup "$JAVA_BIN" -jar "$PWD/.runtime/application.jar" > .runtime/backend.log 2>&1 < /dev/null &
echo $! > .runtime/backend.pid
ready=false
for attempt in $(seq 1 60); do
 if curl -fsS http://127.0.0.1:8080/api/health > /dev/null 2>&1; then ready=true; break; fi
 sleep 1
done
if [ "$ready" != true ]; then echo 'Backend did not become ready; inspect .runtime/backend.log'; exit 1; fi
nohup node "$PWD/frontend/node_modules/vite/bin/vite.js" --host 127.0.0.1 --port 5173 --strictPort --config "$PWD/frontend/vite.config.ts" "$PWD/frontend" > .runtime/frontend.log 2>&1 < /dev/null &
echo $! > .runtime/frontend.pid
for attempt in $(seq 1 30); do
 if curl -fsS http://127.0.0.1:5173 > /dev/null 2>&1; then
  echo 'I-ITSM ready: http://127.0.0.1:5173'
  echo 'Logs: .runtime/backend.log and .runtime/frontend.log'
  echo 'Stop: ./scripts/stop-local.sh (add --database to stop PostgreSQL too)'
  exit 0
 fi
 sleep 1
done
echo 'Frontend did not become ready; inspect .runtime/frontend.log'
exit 1
