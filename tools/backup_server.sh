#!/usr/bin/env bash
# Run on the server: backend_dir frontend_dir backup_root uploads_dir [extra_uploads_dir]
set -Eeuo pipefail
umask 077
backend_dir=$1
frontend_dir=$2
backup_root=$3
uploads_dir=$4
extra_uploads_dir=${5:-}
for tool in mysql mysqldump gzip tar sha256sum flock python3; do command -v "$tool" >/dev/null; done
mkdir -p "$backup_root"
chmod 700 "$backup_root"
exec 9>"$backup_root/.backup.lock"
flock -n 9 || { echo 'Another backup is running.' >&2; exit 1; }
set -a
. "$backend_dir/blog.env"
set +a
db_name=${DB_NAME:-xiaolai_blog}
[[ "$db_name" =~ ^[A-Za-z0-9_]+$ ]] || { echo 'Invalid database name.' >&2; exit 1; }
export MYSQL_PWD=${DB_PASSWORD:?DB_PASSWORD is required}
mysql_args=(--host="${DB_HOST:-127.0.0.1}" --port="${DB_PORT:-3306}" --user="${DB_USERNAME:?DB_USERNAME is required}" --default-character-set=utf8mb4)
# --single-transaction only guarantees a consistent data snapshot for InnoDB.
non_transactional=$(mysql "${mysql_args[@]}" --batch --skip-column-names "$db_name" -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE' AND engine<>'InnoDB'")
test "$non_transactional" = 0 || { echo 'Non-InnoDB tables require a maintenance backup.' >&2; exit 1; }
backup_id="backup-$(date -u +%Y%m%dT%H%M%SZ)-$$"
partial="$backup_root/.$backup_id.partial"
final="$backup_root/$backup_id"
mkdir "$partial"
trap 'echo "Backup failed; incomplete files remain at $partial" >&2' ERR
# No --databases: the dump can be restored into a separate recovery database.
mysqldump "${mysql_args[@]}" --single-transaction --quick --no-tablespaces --routines --triggers --events --hex-blob --set-gtid-purged=OFF "$db_name" | gzip > "$partial/database.sql.gz"
unset MYSQL_PWD
gzip -t "$partial/database.sql.gz"
test -s "$partial/database.sql.gz"
tar czf "$partial/uploads.tgz" -C "$uploads_dir" .
if [ -n "$extra_uploads_dir" ] && [ "$extra_uploads_dir" != "$uploads_dir" ] && [ -d "$extra_uploads_dir" ]; then
  tar czf "$partial/legacy-uploads.tgz" -C "$extra_uploads_dir" .
fi
tar czf "$partial/frontend.tgz" -C "$frontend_dir/dist" .
cp "$backend_dir/blog-1.0.0.jar" "$partial/blog.previous.jar"
cp "$backend_dir/blog.env" "$partial/blog.env"
systemctl cat blog > "$partial/blog.service.txt"
nginx -T > "$partial/nginx.conf.txt" 2> "$partial/nginx-check.txt"
for archive in "$partial"/*.tgz; do tar tzf "$archive" >/dev/null; done
printf 'created_utc=%s\ndatabase=%s\nuploads=%s\nfrontend=%s\n' "$(date -u +%FT%TZ)" "$db_name" "$uploads_dir" "$frontend_dir" > "$partial/manifest.txt"
(cd "$partial" && sha256sum ./*.gz ./*.tgz ./*.jar ./*.env ./*.txt > SHA256SUMS && sha256sum -c SHA256SUMS >/dev/null)
mv "$partial" "$final"
trap - ERR
python3 - "$backup_root" "${BACKUP_KEEP:-14}" <<'PY'
import pathlib, re, shutil, sys
root = pathlib.Path(sys.argv[1]).resolve()
keep = int(sys.argv[2])
if keep < 2:
    raise SystemExit('BACKUP_KEEP must be at least 2')
backups = sorted((p for p in root.iterdir() if re.fullmatch(r'backup-\d{8}T\d{6}Z-\d+', p.name)
                  and not p.is_symlink() and p.is_dir() and (p / 'SHA256SUMS').is_file()
                  and not (p / 'PINNED').exists()), key=lambda p: p.name, reverse=True)
for old in backups[keep:]:
    shutil.rmtree(str(old))
PY
printf 'BACKUP_PATH=%s\n' "$final"
