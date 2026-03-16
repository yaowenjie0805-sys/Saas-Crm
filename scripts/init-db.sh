#!/usr/bin/env bash
set -euo pipefail

DB_USER="${1:-root}"
DB_PASSWORD="${2:-root}"
DB_NAME="${3:-crm_local}"
DB_HOST="${4:-127.0.0.1}"
DB_PORT="${5:-3306}"

echo "[db:init] Checking MySQL connectivity ${DB_HOST}:${DB_PORT} ..."
mysql --protocol=TCP -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASSWORD}" -e "SELECT 1;" >/dev/null

echo "[db:init] Creating database '${DB_NAME}' with user '${DB_USER}'"
mysql --protocol=TCP -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASSWORD}" \
  -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

export DB_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
export DB_USER="${DB_USER}"
export DB_PASSWORD="${DB_PASSWORD}"
export SPRING_PROFILES_ACTIVE="dev"
export LEAD_IMPORT_LISTENER_ENABLED="false"
export LEAD_IMPORT_MQ_DECLARE_ENABLED="false"
export LEAD_IMPORT_MQ_PUBLISH_ENABLED="false"

echo "[db:init] Running Flyway migrations + seed data ..."
mvn -f backend/pom.xml -DskipTests spring-boot:run \
  "-Dspring-boot.run.arguments=--spring.main.web-application-type=none --app.init-only=true"

echo "[db:init] Done (database + migration + seed)."
