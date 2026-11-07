#!/usr/bin/env bash
# Runs on the server via deploy.sh. Arguments are supplied by the local wrapper.
set -Eeuo pipefail
backend_dir=$1
stage_dir=$2
health_url=$3
cd "$backend_dir"
set -a
. ./blog.env
set +a
command -v mysql >/dev/null
export MYSQL_PWD=${DB_PASSWORD:-}
for migration in note.sql progress.sql migration_token_version.sql; do
  mysql --host="${DB_HOST:-127.0.0.1}" --user="${DB_USERNAME:?DB_USERNAME is required}" \
    "${DB_NAME:-xiaolai_blog}" < "$stage_dir/$migration"
done
unset MYSQL_PWD

backup="$stage_dir/blog.previous.jar"
cp blog-1.0.0.jar "$backup"
rollback() {
  code=$?
  trap - ERR
  echo 'Backend deployment failed; restoring previous jar.' >&2
  cp "$backup" blog-1.0.0.jar.rollback
  chown www:www blog-1.0.0.jar.rollback
  mv -f blog-1.0.0.jar.rollback blog-1.0.0.jar
  systemctl restart blog || true
  exit "$code"
}
trap rollback ERR
cp "$stage_dir/blog-1.0.0.jar" blog-1.0.0.jar.new
chown www:www blog-1.0.0.jar.new
chmod 644 blog-1.0.0.jar.new
mv -f blog-1.0.0.jar.new blog-1.0.0.jar
systemctl restart blog

healthy=false
for attempt in 1 2 3 4 5 6; do
  if systemctl is-active --quiet blog && curl --fail --silent --show-error --max-time 5 "$health_url" > /dev/null; then
    healthy=true
    break
  fi
  sleep 5
done
if [ "$healthy" != true ]; then
  echo "Remote health check failed: $health_url" >&2
  false
fi
trap - ERR
echo 'Backend deployed and remote health check passed.'
