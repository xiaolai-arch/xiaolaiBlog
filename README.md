# 小赖博客

个人博客与学习工具站。后端使用 Java 21、Spring Boot、MyBatis-Plus 和 MySQL，前端使用 Vue 3、TypeScript、Vite，部分展示页面使用 React 与 Three.js。

主要功能包括文章、分类、标签、评论、媒体与音乐管理、3D 画廊、私人 Markdown 笔记，以及力扣、英语和 Agent 学习进度同步。

## 本地启动

需要 Java 21、Maven 3.9+、Node.js 22.12+、npm 和 MySQL 8+。

1. 在本机 MySQL 中导入基础表和历史迁移，以下命令仅用于**首次初始化空数据库**。`schema.sql` 会创建并使用 `xiaolai_blog`，不要在已有数据库重复导入。

   ```bash
   mysql -u root -p < sql/schema.sql
   for migration in user_role.sql migration_gallery.sql music.sql gallery_frame.sql note.sql progress.sql migration_token_version.sql; do
     mysql -u root -p xiaolai_blog < "sql/$migration"
   done
   ```

2. 使用本机数据库凭据和独立 JWT 密钥启动后端。密钥至少 32 字节，可以用 `openssl rand -hex 32` 生成；不要提交真实密钥。

   ```bash
   export SPRING_DATASOURCE_USERNAME=your_local_user
   export SPRING_DATASOURCE_PASSWORD=your_local_password
   export JWT_SECRET=your_random_secret_of_at_least_32_bytes
   mvn spring-boot:run
   ```

   数据库默认地址为 `localhost:3306/xiaolai_blog`，可通过 `SPRING_DATASOURCE_URL` 覆盖。后端端口为 `8081`。

3. 在另一终端启动前端：

   ```bash
   cd blog-frontend
   npm ci
   npm run dev
   ```

   打开 `http://localhost:3000`，开发服务器将 `/api` 和 `/uploads` 转发到后端。基础 SQL 包含初始管理员记录，首次使用时应设置自己的管理员密码。

## 测试与构建

```bash
# 后端单元测试、H2 接口回归和可执行 jar
mvn -B verify

# 前端数据安全测试、TypeScript 检查和生产构建
cd blog-frontend
npm ci
npm test
npm run build
cd ..

# 部署成功、失败中止及回滚测试（模拟服务，不连接服务器）
python3 -m unittest discover -s tools/tests -v
bash -n deploy.sh tools/deploy_backend.sh tools/deploy_frontend.sh tools/verify_mysql.sh
```

真实 MySQL 回归使用一个运行中的独立测试 MySQL 服务。指定的用户需要创建、删除测试数据库的权限：

```bash
export MYSQL_TEST_HOST=127.0.0.1
export MYSQL_TEST_PORT=3306
export MYSQL_TEST_USER=your_test_user
export MYSQL_TEST_PASSWORD=your_test_password
bash tools/verify_mysql.sh
```

脚本创建随机命名的 `xiaolai_test_*` 数据库，导入实际建表与迁移 SQL，验证旧用户表升级及重复迁移，再运行认证、权限、注册、文章、笔记与进度同步接口回归。并发用例同时提交两个设备的学习进度，验证事务合并不会丢失其中一份更新。脚本退出时删除自己创建的测试数据库。不要将这些环境变量指向生产 MySQL。Maven 不在 PATH 中时，可通过 `MVN` 指定其可执行文件的绝对路径。

GitHub Actions 会在推送到 `main`、创建/更新 PR 或手动触发时执行：

- Java 21 的完整测试和打包。
- 独立 MySQL 8.0 的建表、迁移和接口回归。
- Node.js 22 的前端测试、类型检查、依赖安全检查与构建。
- 部署脚本语法和回滚测试。

工作流没有生产凭据，也不会自动部署。

## 发布

缺陷与修复对应关系见 [PROJECT_REVIEW.md](PROJECT_REVIEW.md) 和 [REPAIR_NOTES.md](REPAIR_NOTES.md)。

发布前需要备份生产数据库、上传目录和服务器环境配置，并检查备份可读取；保存当前 jar、前端首页和实际服务端口。GitHub 源码仓库不包含用户数据备份。数据库备份可以使用 `mysqldump --single-transaction --routines --triggers`，密码通过受保护的客户端配置提供；备份应保存到服务器之外，定期在独立数据库演练恢复。

服务器应在 `BACKEND_DIR/blog.env` 中提供 `DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`，可按需设置 `DB_HOST`、`DB_NAME`。Java 应用的连接地址由 Spring 配置决定；自定义数据库地址时也要配置 `SPRING_DATASOURCE_URL`，使其与迁移目标一致。保持环境文件仅授权账号可读。

确认测试和备份后，手动发布：

```bash
SERVER=your_ssh_host \
BACKEND_DIR=/your/backend/path \
FRONTEND_DIR=/your/frontend/path \
SITE_URL=https://your.domain \
BACKEND_HEALTH_URL=http://127.0.0.1:8081/api/v1/rss \
bash deploy.sh all
```

此脚本针对已有的 `blog` systemd 服务、`www:www` 文件属主和 Nginx 静态目录。首次安装服务器时需要先配置这些服务及旧版本文件。脚本先发布后端，在迁移成功后才替换 jar；后端健康检查失败会恢复旧 jar。前端保留旧 hash 资源，最后原子替换首页；前端健康检查失败会恢复旧首页。前端发布失败时，已成功发布的后端会保留，请按报错处理并重试。

本次令牌版本迁移会使旧登录状态失效，需要重新登录；退出登录和修改密码会注销同一账号的全部会话。已经被旧代码覆盖的数据需要从历史备份恢复。发布后应验证页面访问、登录、私人笔记保存和学习进度同步，并监控 HTTP 错误率、后端日志及磁盘空间。生产服务器配置、实际网络与备份恢复仍需在目标环境验收。
