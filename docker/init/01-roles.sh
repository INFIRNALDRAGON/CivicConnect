#!/bin/bash
# Runs automatically ONCE, the first time the container starts with an empty volume.
# Reuses db/00_roles.sql (single source of truth) and swaps the placeholder
# passwords for the values in your local .env file, so no secret is committed.
# Use letters and digits only in the .env passwords (they pass through sed).
set -eo pipefail

SQL=/sql/00_roles.sql
if [ ! -f "$SQL" ]; then
  echo "ERROR: $SQL is missing or is a folder. Make sure db/00_roles.sql exists in the repo" >&2
  echo "       as a FILE, then run: docker compose down -v && docker compose up -d" >&2
  exit 1
fi

sed -e "s/CHANGE_ME_owner/${DB_OWNER_PASSWORD}/" \
    -e "s/CHANGE_ME_app/${DB_APP_PASSWORD}/" \
    -e "s/CHANGE_ME_readonly/${DB_READONLY_PASSWORD}/" \
    "$SQL" \
  | psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB"