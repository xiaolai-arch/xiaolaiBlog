# 项目检查记录

修复状态：以下 12 组问题已完成代码修复和回归验证。本文保留初次检查时的发现，具体实现、验证范围及发布迁移见 [REPAIR_NOTES.md](REPAIR_NOTES.md)。

2026-11-08 又完成一次全项目复审，确认并修复了公开接口响应过大、路径泄漏、分页边界、摘要缺失、定时计数、账号存储配额、评论父级完整性、后台 HTML 清理和首页 iframe 资源占用等问题。详见 [REPAIR_NOTES.md](REPAIR_NOTES.md) 的“2026-11-08 全项目复审修复”。

检查日期：2026-11-07。范围：当前工作区源码、前端生产构建、现有后端测试、本地浏览器笔记流程。未访问生产数据库，未执行部署，未修改业务代码。

## 项目概况

- 后端：Java 21、Spring Boot 3.2.5、Spring Security、JWT、MyBatis-Plus、MySQL。
- 前端：Vue 3、TypeScript、Vite、Pinia、Element Plus；胶片与拍立得页面采用 React，3D 画廊采用 Three.js。
- 功能：博客文章、分类和标签、评论审核、媒体和音乐管理、3D 展厅、私人笔记、力扣/四级/Agent 学习进度。
- 发布：deploy.sh 经 SSH 上传前后端产物，重启 systemd 服务；仓库提供部分 Nginx 配置片段和独立 SQL 脚本。

## 验证结果

- `blog-frontend` 下 `npm run build` 成功，包含 vue-tsc 类型检查和 Vite 生产构建。
- 使用 IDEA 自带 Maven 和本地 Java 21 执行 `mvn test`：13 项测试通过，0 失败、0 错误、0 跳过。
- 测试主要覆盖评论 HTML 清理、评论限流和 RSS；未覆盖下列笔记、进度同步和文章恢复问题。前端 package.json 没有测试脚本。
- 在本地生产预览中，用两篇测试笔记复现正文串写。通过同一轮 DOM 输入和点击事件模拟小于 600ms 的快速操作，B 的正文由 `AUDIT_BODY_B` 变为 `AUDIT_BODY_A`。
- 使用实际 Note 类与项目 Jackson 版本检查序列化：未查询正文的 Note 仍会输出 `"content":null`。
- 没有启动连接真实 MySQL 的完整后端，没有验证生产环境配置、实际账号、线上性能和依赖漏洞。下面除明确标注的复现外，均为源码检查结论。

优先级：P1 表示应优先修复的数据完整性或认证风险；P2 表示明确的功能、可用性或部署缺陷。

## 1. P1：自动保存可能把另一篇正文写入当前笔记

位置：`blog-frontend/src/views/front/MarkdownToolPage.vue:264`、`:277`、`:411`。

`scheduleSave()` 捕获笔记 ID，却等 600ms 后才从共享编辑器读取正文。此时若已切换笔记，读取的是另一篇正文。修改标题同样触发该逻辑，不必编辑正文即可覆盖内容。本地浏览器已复现，云端分支使用相同的取值方式。

另外，离开组件时会取消待保存计时器，但仅在本地模式落盘。云端模式下，最后 600ms 内尚未发出的修改会丢失，且没有本地待同步副本。

建议：输入时立即记录当前笔记的内容快照，按笔记 ID 管理保存队列；切换和离开前处理待保存内容；失败时保留可恢复草稿。

## 2. P1：云端笔记可能显示为空，随后被空正文覆盖

位置：`src/main/java/com/xiaolai/blog/controller/front/NoteController.java:32`；`blog-frontend/src/views/front/MarkdownToolPage.vue:225`、`:264`、`:403`。

列表查询不读取 content，但返回完整 Note 实体。当前 Jackson 配置仍序列化 `content: null`。前端仅在 `content === undefined` 时拉取正文，并在 `ensureContent()` 中将 null 当成已加载内容直接返回，因此现有云端笔记会跳过正文请求。若用户修改标题，保存逻辑又会将空编辑器内容提交到后端。

还存在独立的缓存恢复问题：切换云端笔记先清空编辑器；对于已经缓存正文的笔记，`ensureContent()` 直接返回，没有将缓存正文放回编辑器。

建议：列表使用不含正文的专用 DTO；前端显式区分未加载、加载中、已加载和失败状态；切回已加载笔记时恢复缓存正文；正文未成功加载前禁止覆盖保存。

## 3. P1：学习进度缓存没有按账号隔离

位置：`blog-frontend/src/views/front/LeetCodePage.vue:118`、`:273`；`blog-frontend/src/views/front/CET4Page.vue:218`；`blog-frontend/src/views/front/AgentSprintPage.vue:278`；`blog-frontend/src/stores/auth.ts:43`。

完成状态和打卡日志使用固定 localStorage 键。退出登录仅删除 token 和 user。账号 A 使用后，账号 B 在同一浏览器登录，会将 A 留下的本地进度与 B 云端进度合并；满足上传条件或继续打卡后，还会写入 B 的云端账号。这是浏览器侧账号隔离缺陷；后端正常笔记/进度查询本身包含用户归属条件。

建议：所有私人学习缓存按稳定用户 ID 分区；游客数据使用独立空间，登录后通过明确的迁移操作处理。

## 4. P1：管理员修改密码后，旧 JWT 仍有管理权限

位置：`src/main/java/com/xiaolai/blog/service/impl/UserServiceImpl.java:45`；`src/main/java/com/xiaolai/blog/security/JwtAuthenticationFilter.java:32`；`src/main/java/com/xiaolai/blog/controller/admin/AuthController.java`。

修改密码只更新密码哈希。JWT 校验仅检查签名、有效期，并信任令牌里的角色；没有密码版本、撤销时间或令牌撤销校验。退出接口也仅返回成功。因此，已经泄漏的管理员令牌在修改密码之后仍可使用，默认有效期为 24 小时；数据库中的角色调整也不会立即影响旧令牌。

建议：保存用户 tokenVersion 或 credentialsChangedAt，并在认证时校验；修改密码时使旧令牌失效。根据退出语义决定是否增加单个令牌撤销机制。

## 5. P2：首次登录后，本地学习进度可能没有上传

位置：`blog-frontend/src/views/front/LeetCodePage.vue:273`、`:299`；`blog-frontend/src/views/front/CET4Page.vue:218`；`blog-frontend/src/views/front/AgentSprintPage.vue:278`。

合并逻辑将“合并结果”与“合并前本地数据”比较，决定是否上传。若本地已有完成记录、云端为空，合并结果等于本地数据，不会触发上传；界面却已经设置 cloudActive。换设备后这些记录不存在，直到用户后续操作触发写入才可能补齐。

力扣日志还有确定的无效条件：先 `saveLcLog(mergedLog)`，再比较 `mergedLog.length !== getLcLog().length`，此时两者必然相等，上传分支不会执行。

建议：分别比较合并结果与云端、本地快照，并分别同步；日志比较必须保留写入前的快照。多设备取消勾选还需要版本或删除记录，单纯取并集会恢复旧设备上的已完成状态。

## 6. P2：文章恢复接口返回成功，但无法恢复软删除文章

位置：`src/main/java/com/xiaolai/blog/service/impl/ArticleServiceImpl.java:201`；`src/main/java/com/xiaolai/blog/entity/Article.java`。

Article 使用 `@TableLogic`。`restore()` 调用通用 `selectById()`，该查询自动排除已软删除记录，因此目标文章返回 null，恢复操作不执行。控制器仍返回成功。通用 `updateById()` 同样会附加未删除条件，不适合用于恢复已删除记录。

建议：使用专门的恢复 SQL，例如按 ID 和 `deleted=1` 更新为 0，并检查影响行数；为删除→恢复流程增加数据库集成测试。

## 7. P2：文章详情跳转后内容不更新，上一篇/下一篇也未接通

位置：`blog-frontend/src/views/front/ArticleDetail.vue:148`、`:162`；`blog-frontend/src/layouts/FrontLayout.vue`；`src/main/java/com/xiaolai/blog/mapper/ArticleMapper.java:22`。

详情只在 onMounted 加载，没有监听 slug 改变，router-view 也没有按路由设置 key。通过“相关文章”从文章 A 跳到 B，Vue Router 会复用组件，地址变化但正文和评论仍属于 A。此时评论提交却读取新的路由 slug，可能将针对 A 的评论提交到 B。

上一篇/下一篇的 prev、next 始终为 null，没有调用导航接口；即使接通，后端相邻文章查询只选择 id/title，没有返回前端链接需要的 slug。

建议：监听 slug 并重新加载文章、评论、目录和相关推荐，处理快速切换时的旧请求；接通导航接口并返回 slug。

## 8. P2：注册 DTO 校验没有生效

位置：`src/main/java/com/xiaolai/blog/controller/auth/RegisterController.java:35`、`:54`。

DTO 上声明了 NotBlank 和 Size，但入口参数缺少 `@Valid`。缺少 username 时直接 trim 导致 500；短用户名、超长字段绕过声明的约束，可能写入不符合产品规则的数据或触发数据库错误。注解写密码至少 6 位，手写逻辑要求 8 位，规则也不一致。

建议：启用 DTO 校验并统一约束；增加注册频率限制；将并发重名产生的唯一键冲突转为明确的业务错误。

## 9. P2：GitHub 失败缓存未生效，重复请求会持续占用线程

位置：`src/main/java/com/xiaolai/blog/controller/front/GithubActivityController.java:77`、`:107`、`:120`。

失败结果缓存为空列表，但缓存命中条件要求数据非空。若没有磁盘兜底，后续请求即使在 10 分钟失败缓存期内，仍会同步调用 GitHub。每次最多三次、每次 15 秒超时，并额外等待，外部服务故障会反复占用请求线程。冷启动路径没有对同一个 key 合并并发请求。

建议：负缓存有效期内直接返回明确的降级结果；同一个 key 只保留一个抓取任务，并限制并发、用户参数和缓存容量。

## 10. P2：力扣接口降级时混合所有用户的私人日志

位置：`src/main/java/com/xiaolai/blog/controller/front/GithubActivityController.java` 中 leetcode 方法的 `if (!any)` 分支。

官方接口失败时，兜底查询仅筛选 `prog_key=lc-log`，没有 userId 条件，会汇总所有注册用户的打卡日期，并通过公开接口返回。结果既不对应请求的力扣用户，也不再局限于博主的数据；普通用户的记录还可以影响公共展示。

建议：明确公开展示账号与本站用户的映射，只读取指定且允许公开的用户日志，或者在外部服务失败时返回该公开账号的缓存。

## 11. P2：搜索、分类和标签在内存中分页，缺少分页边界

位置：`src/main/java/com/xiaolai/blog/service/impl/ArticleServiceImpl.java:42`；`src/main/resources/mapper/ArticleMapper.xml`；`src/main/java/com/xiaolai/blog/config/MyBatisPlusConfig.java`。

这三个分支先查询全部匹配文章及正文，再通过 stream skip/limit 截取当前页，数据增长后每个小分页请求仍会读取大量数据库数据并占用内存。返回记录还逐篇查标签。page/size 未做正数和上限校验，负数可导致 stream 异常，过大的 size 会扩大负载。

建议：数据库分页与独立 count 查询；列表使用摘要 DTO；限制分页范围，并批量查询标签。

## 12. P2：部署健康检查检查了本机，失败后仍可成功退出

位置：`deploy.sh:34`。

SSH 部署命令结束后，RSS 检查在执行脚本的本机访问 `127.0.0.1:8080`，不是检查远端服务器。六次失败后没有显式失败退出，因此可能误报发布成功。如果本机恰好有其他服务返回 200，也可能误判。

此外，数据库迁移错误被降为 WARN 后继续发布；前端发布先删除旧 dist，再移动新目录，存在短暂不可用窗口，也没有保留旧 hash 资源供已经打开的页面加载懒加载模块。

建议：在远端或通过实际站点地址执行健康检查，确认生产监听端口；失败时明确中止并回滚。数据库迁移失败应阻断依赖该迁移的发布；前端采用版本目录和原子切换，保留一段时间的旧静态资源。

## 建议修复顺序

1. 笔记保存和正文加载：先阻止数据覆盖，再补自动保存、切换、离开和失败恢复测试。
2. 用户数据隔离与同步：按用户区分本地缓存，修正上传判定和公共日志兜底范围。
3. 认证撤销：修改密码后旧令牌失效，并补权限相关集成测试。
4. 文章恢复、详情路由切换、注册参数验证。
5. 部署健康检查、分页和外部接口降级。

前端构建还报告了 Sass 旧 API、React `use client` 指令和大 chunk 警告；构建没有因此失败。3D 页面 JS 产物约 609kB，gzip 约 161kB，应结合实际页面性能测试决定是否进一步拆包，不将体积警告直接等同于线上性能故障。
