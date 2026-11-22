#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
set -a; source .env; set +a
export TEST_DB_URL="${TEST_DB_URL:-jdbc:postgresql://127.0.0.1:55432/iitsm_test}"
export TEST_DB_USER="$DB_USER" TEST_DB_PASSWORD="$DB_PASSWORD"
cd backend
exec mvn test
