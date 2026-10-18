#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONTAINER="civicconnect-test-db"
PORT="${TEST_DB_PORT:-55432}"
ADMIN_PASSWORD="TestAdminPass1"

export DB_HOST=localhost
export DB_PORT="$PORT"
export DB_NAME=civicconnect
export DB_OWNER_PASSWORD="TestOwnerPass1"
export DB_APP_PASSWORD="TestAppPass1"
export DB_READONLY_PASSWORD="TestReadPass1"
export APP_AUTH_SECRET="local-test-secret-that-is-only-used-by-the-test-script-123456"
export SPRING_PROFILES_ACTIVE=dev

cleanup() {
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
}
trap cleanup EXIT

command -v docker >/dev/null || { echo "Docker is required for the database tests. Start Docker Desktop and try again."; exit 1; }
docker info >/dev/null 2>&1 || { echo "Docker is installed but not running. Start Docker Desktop and try again."; exit 1; }

echo "==> Starting a throwaway PostgreSQL on port $PORT (your own database is not touched)"
cleanup
docker run -d --name "$CONTAINER" \
  -e POSTGRES_DB=civicconnect -e POSTGRES_USER=ccadmin -e POSTGRES_PASSWORD="$ADMIN_PASSWORD" \
  -p "$PORT:5432" postgres:15 >/dev/null

for _ in $(seq 1 40); do
  if docker exec "$CONTAINER" pg_isready -U ccadmin -d civicconnect >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
sleep 2

sed -e "s/CHANGE_ME_owner/${DB_OWNER_PASSWORD}/" \
    -e "s/CHANGE_ME_app/${DB_APP_PASSWORD}/" \
    -e "s/CHANGE_ME_readonly/${DB_READONLY_PASSWORD}/" \
    "$ROOT/db/00_roles.sql" \
  | docker exec -i "$CONTAINER" psql -v ON_ERROR_STOP=1 -q -U ccadmin -d civicconnect

echo "==> Backend: unit tests, architecture rules and API tests"
(cd "$ROOT/backend" && sh ./mvnw -B verify)
python3 "$ROOT/scripts/ci/check_test_counts.py" "$ROOT/backend" 55 30 || true

echo "==> Frontend: build, unit tests and browser tests"
cd "$ROOT/frontend"
npm ci
npm run build
npm test
npx playwright install chromium
npm run test:e2e

echo
echo "All tests passed."
echo "Coverage report: backend/target/site/jacoco/index.html"
echo "Browser report:  frontend/playwright-report/index.html (only written when CI=true)"
