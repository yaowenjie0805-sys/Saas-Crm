#!/usr/bin/env bash
set -euo pipefail

DB_USER="${1:-root}"
DB_PASSWORD="${2:-root}"
DB_NAME="${3:-crm_local}"
DB_HOST="${4:-127.0.0.1}"
DB_PORT="${5:-3306}"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

BACKUP_DIR="$REPO_ROOT/backups"
mkdir -p "$BACKUP_DIR"
TS="$(date +%Y-%m-%d_%H%M%S)"
BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_backup_${TS}.sql"

echo "[flyway:repair] Checking MySQL connectivity ${DB_HOST}:${DB_PORT} ..."
mysql --protocol=TCP -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASSWORD}" -e "SELECT 1;" >/dev/null

echo "[flyway:repair] Creating backup: ${BACKUP_FILE}"
mysqldump --protocol=TCP -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASSWORD}" \
  --databases "${DB_NAME}" --single-transaction --routines --triggers --events > "${BACKUP_FILE}"

echo "[flyway:repair] Syncing resources to target/classes ..."
mvn -Dmaven.repo.local="${REPO_ROOT}/.m2repo" \
  -gs "${REPO_ROOT}/.mvn/global-settings.xml" \
  -f "${REPO_ROOT}/apps/api/pom.xml" \
  resources:resources

echo "[flyway:repair] Running Flyway repair ..."
mvn -Dmaven.repo.local="${REPO_ROOT}/.m2repo" \
  -gs "${REPO_ROOT}/.mvn/global-settings.xml" \
  -f "${REPO_ROOT}/apps/api/pom.xml" \
  org.flywaydb:flyway-maven-plugin:9.22.3:repair \
  "-Dflyway.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
  "-Dflyway.user=${DB_USER}" \
  "-Dflyway.password=${DB_PASSWORD}" \
  "-Dflyway.locations=classpath:db/migration"

echo "[flyway:repair] Running Flyway migrate ..."
mvn -Dmaven.repo.local="${REPO_ROOT}/.m2repo" \
  -gs "${REPO_ROOT}/.mvn/global-settings.xml" \
  -f "${REPO_ROOT}/apps/api/pom.xml" \
  org.flywaydb:flyway-maven-plugin:9.22.3:migrate \
  "-Dflyway.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
  "-Dflyway.user=${DB_USER}" \
  "-Dflyway.password=${DB_PASSWORD}" \
  "-Dflyway.locations=classpath:db/migration"

FAILED_COUNT="$(mysql --protocol=TCP -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASSWORD}" -D "${DB_NAME}" -N -B -e "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0;")"
LATEST_VERSION="$(mysql --protocol=TCP -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASSWORD}" -D "${DB_NAME}" -N -B -e "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1;")"

echo "[flyway:repair] Done."
echo "[flyway:repair] Backup: ${BACKUP_FILE}"
echo "[flyway:repair] Failed rows in flyway_schema_history: ${FAILED_COUNT}"
echo "[flyway:repair] Latest version: ${LATEST_VERSION}"
