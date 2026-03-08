#!/usr/bin/env bash
set -euo pipefail

DB_USER="${1:-root}"
DB_PASSWORD="${2:-root}"
DB_NAME="${3:-crm_local}"

echo "[db:init] Creating database '${DB_NAME}' with user '${DB_USER}'"
mysql -u"${DB_USER}" -p"${DB_PASSWORD}" -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
echo "[db:init] Done"
