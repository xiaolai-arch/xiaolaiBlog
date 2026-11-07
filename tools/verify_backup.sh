#!/usr/bin/env bash
# Restore a backup into a temporary, socket-only MySQL instance, never the running database.
set -Eeuo pipefail
umask 077
backup_dir=$1
migrations_dir=$2
mysqld_bin=${MYSQLD_BIN:-/www/server/mysql/bin/mysqld}
mysql_basedir=${MYSQL_BASEDIR:-/www/server/mysql}
mysql_os_user=${MYSQL_OS_USER:-mysql}
export PATH="$mysql_basedir/bin:$PATH"
for tool in mysql mysqladmin mysqlcheck; do command -v "$tool" >/dev/null; done
(cd "$backup_dir" && sha256sum -c SHA256SUMS >/dev/null)
test -x "$mysqld_bin"
recovery_dir=$(mktemp -d /var/tmp/xiaolai-restore.XXXXXX)
socket="$recovery_dir/mysql.sock"
cleanup() {
  if [ -S "$socket" ]; then MYSQL_PWD= mysqladmin --protocol=SOCKET --socket="$socket" --user=root shutdown || true; fi
  if [ -n "${mysql_pid:-}" ] && kill -0 "$mysql_pid" 2>/dev/null; then kill "$mysql_pid"; fi
  if [ -n "${mysql_pid:-}" ]; then wait "$mysql_pid" || true; fi
  rm -rf -- "$recovery_dir"
}
trap cleanup EXIT
chown "$mysql_os_user" "$recovery_dir"
"$mysqld_bin" --no-defaults --initialize-insecure --user="$mysql_os_user" --basedir="$mysql_basedir" --datadir="$recovery_dir/data" --log-error="$recovery_dir/initialize.log"
"$mysqld_bin" --no-defaults --user="$mysql_os_user" --basedir="$mysql_basedir" --datadir="$recovery_dir/data" \
  --socket="$socket" --skip-networking --skip-log-bin --performance-schema=OFF --innodb-buffer-pool-size=64M \
  --innodb-log-file-size=16M --max-connections=10 --log-error="$recovery_dir/server.log" --pid-file="$recovery_dir/mysql.pid" &
mysql_pid=$!
ready=false
for attempt in {1..30}; do
  if MYSQL_PWD= mysqladmin --protocol=SOCKET --socket="$socket" --user=root ping >/dev/null 2>&1; then ready=true; break; fi
  kill -0 "$mysql_pid" 2>/dev/null || { cat "$recovery_dir/server.log" >&2; exit 1; }
  sleep 1
done
test "$ready" = true
export MYSQL_PWD=
mysql_args=(--protocol=SOCKET --socket="$socket" --user=root --default-character-set=utf8mb4)
mysql "${mysql_args[@]}" -e 'CREATE DATABASE xiaolai_restore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'
gzip -dc "$backup_dir/database.sql.gz" | mysql "${mysql_args[@]}" xiaolai_restore
# Check every restored table before testing the release migrations.
mysqlcheck "${mysql_args[@]}" --check xiaolai_restore
before=$(mysql "${mysql_args[@]}" --batch --skip-column-names xiaolai_restore -e 'SELECT COUNT(*) FROM `user`; SELECT COUNT(*) FROM article; SELECT COUNT(*) FROM note; SELECT COUNT(*) FROM user_progress;')
for attempt in 1 2; do
  for migration in note.sql progress.sql migration_token_version.sql; do
    mysql "${mysql_args[@]}" xiaolai_restore < "$migrations_dir/$migration"
  done
done
after=$(mysql "${mysql_args[@]}" --batch --skip-column-names xiaolai_restore -e 'SELECT COUNT(*) FROM `user`; SELECT COUNT(*) FROM article; SELECT COUNT(*) FROM note; SELECT COUNT(*) FROM user_progress;')
test "$before" = "$after"
test "$(mysql "${mysql_args[@]}" --batch --skip-column-names xiaolai_restore -e 'SELECT COUNT(*) FROM `user` WHERE token_version IS NULL')" = 0
printf 'RESTORE_VERIFIED: backup restored, tables checked and repeatable migrations preserve row counts.\n'
