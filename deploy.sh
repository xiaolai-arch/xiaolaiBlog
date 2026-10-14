#!/usr/bin/env bash
# One-command deploy for xiaolaiBlog.
# Usage (Git Bash):  bash deploy.sh backend | frontend | all
#   backend  : mvn package, upload jar, restart systemd service (about 5s downtime)
#   frontend : npm run build, upload dist (no downtime)
#   all      : both, backend first
set -euo pipefail

SERVER=root@140.143.170.29
BACKEND_DIR=/www/wwwroot/backend
FRONTEND_DIR=/www/wwwroot/fronted

export JAVA_HOME="/c/tools/jdk-21.0.12.1+1"
export PATH="$JAVA_HOME/bin:/c/tools/apache-maven-3.9.16/bin:$PATH"

cd "$(dirname "$0")"

deploy_backend() {
  echo "==> backend: tests + package"
  mvn -B test package
  echo "==> backend: upload"
  scp target/blog-1.0.0.jar "$SERVER:$BACKEND_DIR/blog-1.0.0.jar.new"
  scp sql/note.sql sql/progress.sql "$SERVER:/tmp/"
  ssh "$SERVER" "cd $BACKEND_DIR && \
    if command -v mysql >/dev/null 2>&1; then \
      set -a && . ./blog.env && set +a && \
      mysql -h 127.0.0.1 -u\"\${SPRING_DATASOURCE_USERNAME:-root}\" \"\${SPRING_DATASOURCE_PASSWORD:+-p\$SPRING_DATASOURCE_PASSWORD}\" xiaolai_blog < /tmp/note.sql && echo 'note.sql applied' || echo 'WARN: note.sql failed, apply manually'; \
      mysql -h 127.0.0.1 -u\"\${SPRING_DATASOURCE_USERNAME:-root}\" \"\${SPRING_DATASOURCE_PASSWORD:+-p\$SPRING_DATASOURCE_PASSWORD}\" xiaolai_blog < /tmp/progress.sql && echo 'progress.sql applied' || echo 'WARN: progress.sql failed, apply manually'; \
    else echo 'WARN: no mysql client on server, apply sql/*.sql manually'; fi && \
    cp blog-1.0.0.jar blog-1.0.0.jar.bak-\$(date +%Y%m%d-%H%M) && \
    mv blog-1.0.0.jar.new blog-1.0.0.jar && \
    chown www:www blog-1.0.0.jar && chmod 755 blog-1.0.0.jar && \
    systemctl restart blog && sleep 5 && systemctl is-active blog"
  curl -sf -o /dev/null -w "RSS: %{http_code}\n" http://www.xiaolai.cyou/api/v1/rss
}

deploy_frontend() {
  echo "==> frontend: build"
  ( cd blog-frontend && npm run build )
  echo "==> frontend: upload"
  tar czf /tmp/dist.tgz -C blog-frontend/dist .
  scp /tmp/dist.tgz "$SERVER:/tmp/"
  ssh "$SERVER" "rm -rf $FRONTEND_DIR/dist.new && \
    mkdir -p $FRONTEND_DIR/dist.new && \
    tar xzf /tmp/dist.tgz -C $FRONTEND_DIR/dist.new && \
    rm -rf $FRONTEND_DIR/dist && \
    mv $FRONTEND_DIR/dist.new $FRONTEND_DIR/dist && \
    chown -R www:www $FRONTEND_DIR/dist && rm /tmp/dist.tgz && echo frontend deployed"
  curl -sf -o /dev/null -w "首页: %{http_code}\n" http://www.xiaolai.cyou/
  curl -sf -o /dev/null -w "相册页: %{http_code}\n" http://www.xiaolai.cyou/gallery
}

case "${1:-}" in
  backend)  deploy_backend ;;
  frontend) deploy_frontend ;;
  all)      deploy_backend && deploy_frontend ;;
  *) echo "用法: bash deploy.sh backend|frontend|all" >&2; exit 1 ;;
esac
