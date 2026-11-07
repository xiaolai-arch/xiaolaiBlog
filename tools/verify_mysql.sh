#!/usr/bin/env bash
# Create a disposable database, apply the real schema, test migrations and run API regressions.
set -euo pipefail
cd "$(dirname "$0")/.."
test_database="xiaolai_test_$(date +%Y%m%d%H%M%S)_$$"
test_host=${MYSQL_TEST_HOST:-127.0.0.1}
test_port=${MYSQL_TEST_PORT:-3306}
test_user=${MYSQL_TEST_USER:-root}
export MYSQL_PWD=${MYSQL_TEST_PASSWORD:-}
mysql_args=(--protocol=TCP --host="$test_host" --port="$test_port" --user="$test_user" --default-character-set=utf8mb4)
if [ -n "${MYSQL_TEST_SOCKET:-}" ]; then
  mysql_args=(--protocol=SOCKET --socket="$MYSQL_TEST_SOCKET" --user="$test_user" --default-character-set=utf8mb4)
fi
command -v mysql >/dev/null
command -v "${MVN:-mvn}" >/dev/null
mysql "${mysql_args[@]}" -e "CREATE DATABASE \`$test_database\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
cleanup() {
  mysql "${mysql_args[@]}" -e "DROP DATABASE IF EXISTS \`$test_database\`"
}
trap cleanup EXIT
# schema.sql names the production database; omit those two directives before importing.
sed -e '/^CREATE DATABASE /d' -e '/^USE /d' sql/schema.sql | mysql "${mysql_args[@]}" "$test_database"
for migration in user_role.sql migration_gallery.sql music.sql gallery_frame.sql note.sql progress.sql; do
  mysql "${mysql_args[@]}" "$test_database" < "sql/$migration"
done
# Exercise the upgrade from a pre-versioned user table, then repeat it to check idempotency.
mysql "${mysql_args[@]}" "$test_database" -e 'ALTER TABLE `user` DROP COLUMN token_version'
for attempt in 1 2; do
  for migration in migration_token_version.sql note.sql progress.sql; do
    mysql "${mysql_args[@]}" "$test_database" < "sql/$migration"
  done
done
test "$(mysql "${mysql_args[@]}" --batch --skip-column-names "$test_database" -e "SELECT COUNT(*) FROM \`user\` WHERE username='admin' AND token_version=0")" = 1
export TEST_DATABASE_URL="jdbc:mysql://$test_host:$test_port/$test_database?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
export TEST_DATABASE_DRIVER=com.mysql.cj.jdbc.Driver
export TEST_DATABASE_USERNAME=$test_user
export TEST_DATABASE_PASSWORD=${MYSQL_TEST_PASSWORD:-}
"${MVN:-mvn}" -B -Dtest=RegressionIntegrationTest test
echo 'MySQL schema, repeatable migrations and API regressions passed.'
