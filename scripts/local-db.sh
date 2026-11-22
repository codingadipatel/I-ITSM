#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ ! -f .env ]; then python3 scripts/setup-env.py; fi
set -a; source .env; set +a
mkdir -p .runtime
DATA="$PWD/.runtime/postgres"
case "${1:-start}" in
 start)
  if [ ! -f "$DATA/PG_VERSION" ]; then
   PASSFILE=$(mktemp "$PWD/.runtime/pgpass.XXXXXX")
   chmod 600 "$PASSFILE"
   trap 'rm -f "$PASSFILE"' EXIT
   printf '%s' "$DB_PASSWORD" > "$PASSFILE"
   initdb -D "$DATA" -U "$DB_USER" --auth-local=trust --auth-host=scram-sha-256 --pwfile="$PASSFILE" > .runtime/initdb.log
   rm -f "$PASSFILE"; trap - EXIT
  fi
  if ! pg_ctl -D "$DATA" status >/dev/null 2>&1; then pg_ctl -D "$DATA" -l .runtime/postgres.log -o '-p 55432 -h 127.0.0.1' start; fi
  export PGPASSWORD="$DB_PASSWORD"
  if ! psql -h 127.0.0.1 -p 55432 -U "$DB_USER" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='iitsm'" | grep -q 1; then createdb -h 127.0.0.1 -p 55432 -U "$DB_USER" iitsm; fi
  if ! psql -h 127.0.0.1 -p 55432 -U "$DB_USER" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='iitsm_test'" | grep -q 1; then createdb -h 127.0.0.1 -p 55432 -U "$DB_USER" iitsm_test; fi
  echo 'PostgreSQL ready at 127.0.0.1:55432 (iitsm and iitsm_test).'
  ;;
 stop) pg_ctl -D "$DATA" stop ;;
 *) echo 'Usage: scripts/local-db.sh start|stop'; exit 1 ;;
esac
