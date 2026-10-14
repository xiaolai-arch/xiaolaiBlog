# -*- coding: utf-8 -*-
"""把 Obsidian 知识库的 A/B/C 三组笔记清洗后导入博客（全部建为草稿，status=0）。

用法： python tools/import_vault_notes.py [--dry-run]

清洗内容：
  1. 剥掉 YAML frontmatter（含重复 frontmatter 的合并痕迹）
  2. 删除「关联笔记」等只对知识库有意义的小节、首页/索引导航行
  3. [[wikilink|别名]] -> 别名（纯文本，不生成死链）
  4. 自动生成摘要（第一段正文，约 100 字）
"""
import json
import os
import pathlib
import re
import sys
import urllib.error
import urllib.request

DRY_RUN = "--dry-run" in sys.argv

BASE = "http://www.xiaolai.cyou"
VAULT = pathlib.Path(r"C:\Users\34191\Documents\Java全栈")
ADMIN_USER = "admin"
# 密码不进仓库：运行前 export BLOG_ADMIN_PASS=xxx（Windows: set BLOG_ADMIN_PASS=xxx）
ADMIN_PASS = os.environ.get("BLOG_ADMIN_PASS", "")
if not ADMIN_PASS and not DRY_RUN:
    sys.exit("请先设置环境变量 BLOG_ADMIN_PASS 再运行")


# ---------------------------------------------------------------- API
def api(method, path, body=None, token=None):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    req.add_header("User-Agent", "vault-importer/1.0")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.load(resp)
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", "ignore")
        raise RuntimeError(f"{method} {path} -> HTTP {e.code}: {detail[:300]}") from None


# ---------------------------------------------------------------- 清洗
FRONTMATTER = re.compile(r"\A---\r?\n.*?\r?\n---\r?\n?", re.S)
SECTION_DROP = re.compile(r"^#{1,6}\s*.*(?:关联笔记|相关笔记|相关文章).*$")
NAV_RAW = re.compile(r"^\s*(?:[-*]\s*)?\[\[[^\]]*(?:首页|索引|目录)[^\]]*\]\]\s*$")
NAV_TEXT = re.compile(r"^\s*(?:[-*]\s*)?(?:←\s*返回|返回首页|返回顶部)\s*$")
WIKILINK = re.compile(r"!?\[\[([^\]|]+)(?:\|([^\]]+))?\]\]")


def strip_frontmatter(text):
    text = text.lstrip("﻿")
    while True:
        # 相邻的多段 frontmatter 之间可能只隔空行，逐段剥掉
        text = text.lstrip("\r\n")
        m = FRONTMATTER.match(text)
        if not m:
            return text
        text = text[m.end():]


def clean(text):
    text = strip_frontmatter(text)
    out, skip = [], False
    for line in text.split("\n"):
        if SECTION_DROP.match(line):
            skip = True
            continue
        if skip:
            if re.match(r"^#{1,6}\s", line):
                skip = False
            else:
                continue
        if NAV_RAW.match(line) or NAV_TEXT.match(line):
            continue

        def rep(m):
            target, alias = m.group(1), m.group(2)
            name = target.split("#")[0].split("/")[-1].removesuffix(".md").strip()
            return (alias or name).strip()

        out.append(WIKILINK.sub(rep, line))
    text = "\n".join(out)
    text = re.sub(r"[​‌‍﻿]", "", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip() + "\n"


def demote_headings(text):
    """整体降一级标题（用于把第二篇笔记并入第一篇）。"""
    return re.sub(r"^(#{1,5})\s", lambda m: m.group(1) + "# ", text, flags=re.M)


def make_summary(md, limit=100):
    in_fence = False
    for line in md.split("\n"):
        s = line.strip()
        if s.startswith("```"):
            in_fence = not in_fence
            continue
        if in_fence or not s:
            continue
        if s.startswith(("#", "|", "---", "!")):
            continue
        if re.match(r"^(tags|created|source|title|date|updated)\s*:", s):
            continue
        s = re.sub(r"^>\s*", "", s)
        s = re.sub(r"[*`_#]", "", s)
        s = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", s)
        if len(s) < 8:
            continue
        if len(s) > limit:
            cut = max(s.rfind("。", 0, limit), s.rfind("；", 0, limit), s.rfind("，", 0, limit))
            s = s[:cut + 1] if cut > 20 else s[:limit] + "…"
        return s
    return ""


# ---------------------------------------------------------------- 导入清单
# (vault 相对路径, 标题, slug, 分类名, [标签])
MANIFEST = [
    # —— A 组：微服务与分布式（倒序创建，让 00 篇显示在系列最上面）——
    ("2-领域/微服务与分布式/102-Docker项目部署实战.md", "13、Docker 项目部署实战", "13-docker-项目部署实战", "微服务与分布式", ["docker", "devops"]),
    ("2-领域/微服务与分布式/101-Docker入门.md", "12、Docker 入门", "12-docker-入门", "微服务与分布式", ["docker"]),
    ("2-领域/微服务与分布式/100-JMeter 使用指南.md", "11、JMeter 使用指南", "11-jmeter-使用指南", "微服务与分布式", ["devops"]),
    ("2-领域/微服务与分布式/10-Redis 缓存实战.md", "10、Redis 缓存实战", "10-redis-缓存实战", "微服务与分布式", ["redis", "微服务"]),
    ("2-领域/微服务与分布式/09-Elasticsearch 搜索引擎.md", "09、Elasticsearch 搜索引擎", "09-elasticsearch-搜索引擎", "微服务与分布式", ["微服务"]),
    ("2-领域/微服务与分布式/08-消息队列 MQ.md", "08、消息队列 MQ", "08-消息队列-mq", "微服务与分布式", ["消息队列", "微服务"]),
    ("2-领域/微服务与分布式/07-多数据源 dynamic-datasource.md", "07、多数据源 dynamic-datasource", "07-多数据源-dynamic-datasource", "微服务与分布式", ["微服务"]),
    ("2-领域/微服务与分布式/06-分布式事务 Seata.md", "06、分布式事务 Seata", "06-分布式事务-seata", "微服务与分布式", ["微服务"]),
    ("2-领域/微服务与分布式/05-Sentinel 熔断降级.md", "05、Sentinel 熔断降级", "05-sentinel-熔断降级", "微服务与分布式", ["微服务"]),
    ("2-领域/微服务与分布式/04-OpenFeign 远程调用.md", "04、OpenFeign 远程调用", "04-openfeign-远程调用", "微服务与分布式", ["微服务"]),
    ("2-领域/微服务与分布式/03-Gateway 网关.md", "03、Gateway 网关", "03-gateway-网关", "微服务与分布式", ["微服务"]),
    ("2-领域/微服务与分布式/02-LoadBalancer 负载均衡.md", "02、LoadBalancer 负载均衡", "02-loadbalancer-负载均衡", "微服务与分布式", ["微服务"]),
    ("2-领域/微服务与分布式/01-Nacos 注册中心.md", "01、Nacos 注册中心", "01-nacos-注册中心", "微服务与分布式", ["微服务"]),
    ("2-领域/微服务与分布式/00-SpringCloud.md", "00、SpringCloud 总览", "00-springcloud-总览", "微服务与分布式", ["java", "微服务"]),

    # —— B 组：黑马头条项目实战（倒序创建，Day01 显示在最上面）——
    ("2-领域/Java/黑马头条/参考资料/延迟任务与线程池优化.md", "延迟任务与线程池优化", "延迟任务与线程池优化", "项目实战", ["java"]),
    ("2-领域/Java/黑马头条/参考资料/GitLab私服使用说明.md", "GitLab 私服使用说明", "gitlab-私服使用说明", "项目实战", ["devops"]),
    ("2-领域/Java/黑马头条/参考资料/MinIO分布式文件系统.md", "MinIO 分布式文件系统", "minio-分布式文件系统", "项目实战", ["devops"]),
    ("2-领域/Java/黑马头条/参考资料/Jenkins持续集成.md", "Jenkins 持续集成", "jenkins-持续集成", "项目实战", ["devops"]),
    ("__MERGE_DAY0405__", "黑马头条 Day04：评论、搜索与部署上线", "黑马头条-day04", "项目实战", ["项目实战", "java"]),
    ("2-领域/Java/黑马头条/Day03-文章发布与管理.md", "黑马头条 Day03：文章发布与管理", "黑马头条-day03", "项目实战", ["项目实战", "java"]),
    ("2-领域/Java/黑马头条/Day02-前端部署与网关.md", "黑马头条 Day02：前端部署与网关", "黑马头条-day02", "项目实战", ["项目实战", "java"]),
    ("2-领域/Java/黑马头条/Day01-环境搭建与登录.md", "黑马头条 Day01：环境搭建与登录", "黑马头条-day01", "项目实战", ["项目实战", "java"]),

    # —— C 组：Java 补充（入现有 Java 分类）——
    ("2-领域/Java/Java 学习路线.md", "Java 学习路线", "java-学习路线", "Java", ["java"]),
    ("2-领域/Java/苍穹外卖知识点精华.md", "苍穹外卖知识点精华", "苍穹外卖知识点精华", "Java", ["项目实战", "java"]),
    ("2-领域/Java/Spring生态/MyBatisPlus.md", "MyBatisPlus 使用指南", "mybatisplus-使用指南", "Java", ["java"]),
]

NEW_CATEGORIES = [
    ("微服务与分布式", "microservices", "SpringCloud、Nacos、Gateway、Seata、MQ、Docker 等微服务与分布式技术笔记"),
    ("项目实战", "projects", "实战项目的过程记录与复盘"),
]
NEW_TAGS = ["微服务", "java", "docker", "devops", "redis", "消息队列", "项目实战"]


def load_note(rel):
    if rel == "__MERGE_DAY0405__":
        day04 = clean((VAULT / "2-领域/Java/黑马头条/Day04-评论与搜索.md").read_text(encoding="utf-8"))
        day05 = clean((VAULT / "2-领域/Java/黑马头条/Day05-部署上线.md").read_text(encoding="utf-8"))
        return day04 + "\n\n---\n\n" + demote_headings(day05)
    return clean((VAULT / rel).read_text(encoding="utf-8"))


def main():
    token = api("POST", "/api/v1/admin/auth/login",
                {"username": ADMIN_USER, "password": ADMIN_PASS})["data"]["token"]
    print("登录成功")

    cat_ids = {c["name"]: c["id"] for c in api("GET", "/api/v1/admin/categories", token=token)["data"]}
    for name, slug, desc in NEW_CATEGORIES:
        if name not in cat_ids:
            if DRY_RUN:
                print(f"[dry-run] 新建分类 {name}")
                cat_ids[name] = -1
            else:
                r = api("POST", "/api/v1/admin/categories",
                        {"name": name, "slug": slug, "description": desc}, token=token)
                cat_ids[name] = r["data"]["id"]
                print(f"新建分类 {name} (id={cat_ids[name]})")

    tag_ids = {t["name"]: t["id"] for t in api("GET", "/api/v1/admin/tags", token=token)["data"]}
    for name in NEW_TAGS:
        if name not in tag_ids:
            if DRY_RUN:
                print(f"[dry-run] 新建标签 {name}")
                tag_ids[name] = -1
            else:
                r = api("POST", "/api/v1/admin/tags", {"name": name}, token=token)
                tag_ids[name] = r["data"]["id"]
                print(f"新建标签 {name} (id={tag_ids[name]})")

    existing = {a.get("slug") for a in api(
        "GET", "/api/v1/admin/articles?page=1&size=100", token=token)["data"]["list"]}

    ok = skipped = 0
    for rel, title, slug, cat, tags in MANIFEST:
        if slug in existing:
            print(f"跳过（slug 已存在）: {title}")
            skipped += 1
            continue
        content = load_note(rel)
        summary = make_summary(content)
        payload = {
            "title": title,
            "slug": slug,
            "summary": summary,
            "contentMd": content,
            "categoryId": cat_ids[cat],
            "status": 0,
            "isTop": 0,
            "tagIds": [tag_ids[t] for t in tags if t in tag_ids],
        }
        if DRY_RUN:
            print(f"[dry-run] {title}  ({len(content)} 字符)  摘要: {summary[:30]}…")
        else:
            r = api("POST", "/api/v1/admin/articles", payload, token=token)
            print(f"已建草稿 #{r['data']['id']}  {title}  ({len(content)} 字符)")
        ok += 1

    print(f"\n完成: 新建 {ok} 篇草稿, 跳过 {skipped} 篇")


if __name__ == "__main__":
    main()
