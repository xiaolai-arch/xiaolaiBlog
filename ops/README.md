# 生产运维记录

2026-11-07 已在现有服务器安装并验收以下项目。服务器为 CentOS、Java 21.0.2、MySQL 5.7.44；应用使用 prod 配置。服务与数据库地址的实际情况优先于仓库开发默认值。

## 服务与发布

- systemd 服务：`blog`，运行账号 `www`。
- 后端：`/www/wwwroot/backend/blog-1.0.0.jar`，监听 **8080**。
- 环境文件：`/www/wwwroot/backend/blog.env`，权限 `600 root:root`。
- 前端：`/www/wwwroot/fronted/dist`。
- Nginx 站点：`/www/server/panel/vhost/nginx/140.143.170.29.conf`。
- 上传文件：`/data/blog/uploads`；旧目录 `/www/wwwroot/backend/uploads` 也已备份。
- 当前网站：`http://www.xiaolai.cyou`。服务器只监听 HTTP 80，尚无 TLS 证书和 HTTPS 443 监听。HTTPS 默认发布检查曾触发回滚，后按实际 HTTP 地址重新发布成功。启用 HTTPS 仍需配置证书、续期及确认云端和本机 443 入站规则；完成后同步更新巡检地址。

按当前服务器配置发布：

```bash
BACKEND_HEALTH_URL=http://127.0.0.1:8080/api/v1/rss \
SITE_URL=http://www.xiaolai.cyou \
bash deploy.sh all
```

`MVN` 可指定 Maven 绝对路径。发布前端会执行 `npm ci`、依赖审计、测试和构建。已有前端资源与后端 jar 的发布回滚逻辑保留。

本次发现 prod 环境仍配置仓库默认 JWT 密钥，已在服务器本机生成并替换为独立随机密钥，未输出、下载或提交新密钥。新增启动校验：prod/production 使用仓库默认密钥时拒绝启动。现有会话已失效，用户需重新登录。

## 备份与恢复

备份脚本安装于 `/opt/xiaolai-ops/backup_server.sh`，备份根目录为 `/var/backups/xiaolai-blog`（权限 700），每天凌晨 04:15 左右执行：

```bash
systemctl list-timers xiaolai-backup.timer
systemctl start xiaolai-backup.service
journalctl -u xiaolai-backup.service -n 40
```

备份包含一致性数据库导出、两个上传目录、前端完整文件、旧 jar、环境文件、systemd 与 Nginx 配置和 SHA-256 校验。失败不会发布为成功备份；MySQL 导出不需要 PROCESS 权限。默认保留最近 14 个成功备份，存在 `PINNED` 文件的备份长期保留；失败的临时目录不自动删除，需检查原因。备份包含私密数据，应保持权限限制，不要提交 Git。

发布前的完整备份：`/var/backups/xiaolai-blog/backup-20261107T104424Z-11354`，已标记 PINNED。本次已用该备份在独立、仅使用 Unix socket 的 MySQL 5.7 实例恢复，14 张表检查通过；重复执行本次迁移后，用户、文章、笔记、学习进度行数保持一致。临时实例和数据目录已清理。

复跑恢复演练：

```bash
bash /opt/xiaolai-ops/verify_backup.sh \
  /var/backups/xiaolai-blog/backup-20261107T104424Z-11354 \
  /opt/xiaolai-ops/migrations
```

该命令需要 root，用服务器现有 MySQL 二进制创建临时实例，最大连接数 10、缓冲池 64MB，不连接运行中的生产数据库。完整备份已复制到本机私有目录 `/Users/Admin/.local/share/xiaolai-blog/backups/`；每日自动备份目前保存在服务器，本机副本的持续定时拉取尚未配置。

## 巡检与验收

`xiaolai-health.timer` 每五分钟运行只读检查，覆盖首页、画廊、笔记页面、RSS、数据库支持的文章列表，以及未登录访问私人笔记必须返回 401。失败返回非零，systemd 记录 failed 状态和具体检查结果：

```bash
systemctl status xiaolai-health.service
journalctl -u xiaolai-health.service -n 50
python3 /opt/xiaolai-ops/check_site.py http://www.xiaolai.cyou
```

尚未连接邮件、短信或聊天工具等外部通知渠道。失败信息目前在服务器日志中。

本次临时验收账号验证通过注册、后台权限限制、笔记保存和归属隔离、两设备并发进度、取消完成、改密令牌失效、退出登录令牌失效，以及公开文章/音乐/画框接口。临时用户、笔记、进度和登录记录均已删除，未修改已有用户密码或业务内容。

需要复跑有写操作的验收时：

```bash
set -a
. /www/wwwroot/backend/blog.env
set +a
python3 /opt/xiaolai-ops/acceptance_server.py
```

验收脚本会短暂创建两个 `qa_` 随机账号，只提升其中一个临时账号以验证改密，结束时清理自己创建的数据。不要将该脚本作为高频巡检运行。

## 本次功能升级与 HTTPS（2026-11-07 晚）

文章写作、笔记管理、账号设置、系列阅读与学习计划升级已发布，详见 [功能说明](../FEATURE_UPGRADE.md)。发布前备份为 `/var/backups/xiaolai-blog/backup-20261107T121208Z-3614`；已在独立 socket-only MySQL 5.7 实例恢复并两次执行 `migration_features.sql`，保留既有行数。

本次服务端与本机构建 SHA-256 一致：

- 后端 jar：`bd1bf0d034330b009db978b0f84eb8abb7222e044e2e671bfa043ef4f9b8691d`
- 前端 index：`a6075e610ffda4521068691d77a541ca0782e21aef22149d8066c8d561dee6ae`

更新后的验收脚本不再提升临时账号权限，直接验证普通用户改密，并检查笔记版本恢复、回收站和学习计划。发布后运行通过，自己创建的用户、笔记、版本、计划、进度与登录记录均已清理。

HTTPS 已取得 `www.xiaolai.cyou` 的 Let's Encrypt 证书，有效期至 **2027-02-05 11:18:10 UTC**。私钥及链位于 `/etc/nginx/ssl/www.xiaolai.cyou/`，仅在服务器保存。ACME 客户端为 `/root/.acme.sh/acme.sh`，源码取自 acmesh-official/acme.sh 提交 `7e87a310fda61afe92e3db6ac29f81efdf53b8c0`。`xiaolai-cert-renew.timer` 每日 03:30 加随机延迟检查续期，已启用并手动执行成功；签发使用 HTTP webroot，续期后验证并重载 Nginx。

Nginx 已监听 443，服务器本机使用正确域名和证书校验访问 HTTPS 首页与 RSS 均为 200。firewalld 已允许 HTTPS。**外部网络访问 443 仍然超时**，需要检查腾讯云安全组/上游入站策略；当前凭据只可 SSH，未修改云端控制台。为保持网站可访问，没有启用强制 HTTPS 跳转，HTTP 首页和 API 继续为 200；巡检和发布仍用实际可达的 HTTP 地址。旧 Nginx 配置备份在 `/root/xiaolai-nginx-before-tls-20261107.conf`。

服务器没有 SMTP 配置，`/api/v1/auth/recovery` 实际返回 `enabled:false`。不要把已实现邮件接口描述成已开通找回服务。公网 HTTPS 与 SMTP 投递均验收成功后，再启用 `APP_PUBLIC_URL` 及邮件配置，并更新巡检、RSS 站点地址及 HTTP 跳转策略。

2026-11-08 复审修复新增 `sql/migration_review_fixes.sql`，发布脚本会自动执行。该迁移回填历史文章摘要、修复空 slug、清理伪造/孤立回复并增加评论父级外键，同时禁用旧版不可登录的管理员占位哈希。首次初始化不再内置固定管理员密码；按 README 使用环境变量生成 BCrypt 哈希。已有账号不受影响。
