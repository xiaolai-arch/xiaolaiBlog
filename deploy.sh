#!/usr/bin/env bash
# Usage: bash deploy.sh backend|frontend|all
# Optional overrides: SERVER, BACKEND_DIR, FRONTEND_DIR, BACKEND_HEALTH_URL, SITE_URL.
set -euo pipefail
SERVER=${SERVER:-root@140.143.170.29}
BACKEND_DIR=${BACKEND_DIR:-/www/wwwroot/backend}
FRONTEND_DIR=${FRONTEND_DIR:-/www/wwwroot/fronted}
BACKEND_HEALTH_URL=${BACKEND_HEALTH_URL:-http://127.0.0.1:8081/api/v1/rss}
SITE_URL=${SITE_URL:-https://www.xiaolai.cyou}
# Keep the existing Git Bash tool locations available without overriding a configured JDK.
if [ -z "${JAVA_HOME:-}" ] && [ -d /c/tools/jdk-21.0.12.1+1 ]; then
  export JAVA_HOME=/c/tools/jdk-21.0.12.1+1
  export PATH="$JAVA_HOME/bin:/c/tools/apache-maven-3.9.16/bin:$PATH"
fi
cd "$(dirname "$0")"
release_id="release-$(date +%Y%m%d-%H%M%S)-$$"
for directory in "$BACKEND_DIR" "$FRONTEND_DIR"; do
  [[ "$directory" =~ ^/[A-Za-z0-9_./-]+$ ]] || { echo 'Deployment directories must be absolute shell-safe paths.' >&2; exit 1; }
done
[[ "$BACKEND_HEALTH_URL" =~ ^http://127\.0\.0\.1:[0-9]+/api/v1/rss$ ]] || { echo 'Invalid backend health URL.' >&2; exit 1; }
[[ "$SITE_URL" =~ ^https?://[A-Za-z0-9.:-]+$ ]] || { echo 'Invalid site URL.' >&2; exit 1; }

deploy_backend() {
  mvn -B verify
  local stage="$BACKEND_DIR/$release_id"
  ssh "$SERVER" mkdir -p "$stage"
  scp target/blog-1.0.0.jar sql/note.sql sql/progress.sql sql/migration_token_version.sql "$SERVER:$stage/"
  ssh "$SERVER" bash -s -- "$BACKEND_DIR" "$stage" "$BACKEND_HEALTH_URL" < tools/deploy_backend.sh
}

deploy_frontend() {
  (cd blog-frontend && npm test && npm run build)
  local archive
  archive=$(mktemp /tmp/xiaolai-dist.XXXXXX)
  tar czf "$archive" -C blog-frontend/dist .
  local stage="$FRONTEND_DIR/$release_id"
  ssh "$SERVER" mkdir -p "$stage"
  scp "$archive" "$SERVER:$stage/dist.tgz"
  unlink "$archive"
  ssh "$SERVER" bash -s -- "$FRONTEND_DIR" "$stage" "$SITE_URL" < tools/deploy_frontend.sh
}

case "${1:-}" in
  backend) deploy_backend ;;
  frontend) deploy_frontend ;;
  all) deploy_backend; deploy_frontend ;;
  *) echo '用法: bash deploy.sh backend|frontend|all' >&2; exit 1 ;;
esac
