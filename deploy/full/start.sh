#!/usr/bin/env bash
set -euo pipefail

DB_NAME="${POSTGRES_DB:-smart_portfolio}"
DB_USER="${POSTGRES_USER:-portfolio}"
DB_PASSWORD="${POSTGRES_PASSWORD:-portfolio_secret}"
PGDATA="${PGDATA:-/var/lib/postgresql/data}"

if [[ ! "$DB_NAME" =~ ^[A-Za-z_][A-Za-z0-9_]*$ || ! "$DB_USER" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
    echo "POSTGRES_DB and POSTGRES_USER must be valid PostgreSQL identifiers." >&2
    exit 1
fi

DB_PASSWORD_SQL="${DB_PASSWORD//\'/\'\'}"

export DATABASE_URL="${DATABASE_URL:-postgres://${DB_USER}:${DB_PASSWORD}@127.0.0.1:5432/${DB_NAME}?sslmode=disable}"
export FRONTEND_URL="${FRONTEND_URL:-http://localhost:3000}"
export PORT="${PORT:-8080}"

mkdir -p "$PGDATA" /run/postgresql
chown -R postgres:postgres "$PGDATA" /run/postgresql
chmod 700 "$PGDATA"

if [ ! -s "$PGDATA/PG_VERSION" ]; then
    gosu postgres initdb -D "$PGDATA"
    {
        echo "listen_addresses = '127.0.0.1'"
        echo "unix_socket_directories = '/run/postgresql'"
    } >> "$PGDATA/postgresql.conf"
    {
        echo "local all all trust"
        echo "host all all 127.0.0.1/32 scram-sha-256"
    } >> "$PGDATA/pg_hba.conf"
fi

gosu postgres pg_ctl -D "$PGDATA" -w start

if ! psql -U postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='${DB_USER}'" | grep -q 1; then
    psql -U postgres -v ON_ERROR_STOP=1 -c "CREATE ROLE \"${DB_USER}\" LOGIN PASSWORD '${DB_PASSWORD_SQL}'"
else
    psql -U postgres -v ON_ERROR_STOP=1 -c "ALTER ROLE \"${DB_USER}\" WITH LOGIN PASSWORD '${DB_PASSWORD_SQL}'"
fi

if ! psql -U postgres -tAc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'" | grep -q 1; then
    psql -U postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE \"${DB_NAME}\" OWNER \"${DB_USER}\""
fi

psql -U postgres -v ON_ERROR_STOP=1 -d "$DB_NAME" -c "GRANT ALL PRIVILEGES ON DATABASE \"${DB_NAME}\" TO \"${DB_USER}\""

nginx

/app/server &
server_pid="$!"

stop_services() {
    kill -TERM "$server_pid" 2>/dev/null || true
    nginx -s quit 2>/dev/null || true
    gosu postgres pg_ctl -D "$PGDATA" -m fast -w stop 2>/dev/null || true
}

trap stop_services INT TERM

set +e
wait "$server_pid"
status="$?"
set -e
stop_services
exit "$status"
