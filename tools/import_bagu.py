# -*- coding: utf-8 -*-
"""把知识库的 23 篇八股文笔记合并成 4 篇「面试速背」文章，导入博客为草稿。

用法： export BLOG_ADMIN_PASS=xxx && python tools/import_bagu.py
"""
import os
import sys
import urllib.request

sys.path.insert(0, os.path.dirname(__file__))
from import_vault_notes import api, BASE, clean, demote_headings, make_summary  # noqa: E402

VAULT = r"C:\Users\34191\Documents\Java全栈\2-领域\Java\八股文"

GROUPS = [
    ("Java 面试八股速背 · 基础篇", "java-bagu-basics", ["01-Java集合", "02-Java并发", "03-JVM", "11-设计模式", "15-Java基础", "16-Java新特性"]),
    ("Java 面试八股速背 · 框架篇", "java-bagu-framework", ["06-Spring框架", "07-SpringBoot", "08-MyBatis", "09-SpringSecurity"]),
    ("Java 面试八股速背 · 中间件与分布式篇", "java-bagu-middleware", ["04-MySQL", "05-Redis", "20-消息队列", "19-分布式锁", "10-分布式理论", "12-八股-Nacos", "13-八股-OpenFeign", "14-八股-Gateway", "17-八股-Sentinel", "18-八股-Seata", "23-Elasticsearch"]),
    ("Java 面试八股速背 · 操作系统与网络篇", "java-bagu-os-network", ["21-计算机网络", "22-操作系统"]),
]

DRY_RUN = "--dry-run" in sys.argv


def load_note(stem: str) -> str:
    text = clean(open(f"{VAULT}\\{stem}.md", encoding="utf-8").read())
    # 去掉文件自身的 H1 标题（合并后用小节标题），其余标题降一级
    lines = text.split("\n")
    if lines and lines[0].startswith("# "):
        lines = lines[1:]
    return demote_headings("\n".join(lines).strip())


def main():
    if "--dry-run" in sys.argv:
        for title, slug, stems in GROUPS:
            body = f"> 由知识库 {len(stems)} 篇八股笔记合并整理，快速过一遍要点用。\n\n"
            for stem in stems:
                body += f"## {stem}\n\n{load_note(stem)}\n\n---\n\n"
            print(f"[dry-run] {title}  ({len(body)} 字符)  摘要: {make_summary(body)[:30]}…")
        return

    token = api("POST", "/api/v1/admin/auth/login",
                {"username": "admin", "password": os.environ["BLOG_ADMIN_PASS"]})["data"]["token"]
    cats = api("GET", "/api/v1/admin/categories", token=token)["data"]
    java_id = next(c["id"] for c in cats if c["name"] == "Java")
    tags = api("GET", "/api/v1/admin/tags", token=token)["data"]
    tag_id = next((t["id"] for t in tags if t["name"] == "面试"), None)
    if tag_id is None:
        tag_id = api("POST", "/api/v1/admin/tags", {"name": "面试"}, token=token)["data"]["id"]
        print(f"新建标签 面试 (id={tag_id})")

    existing = {a.get("slug") for a in api(
        "GET", "/api/v1/admin/articles?page=1&size=100", token=token)["data"]["list"]}

    for title, slug, stems in GROUPS:
        if slug in existing:
            print(f"跳过（已存在）: {title}")
            continue
        body = f"> 由知识库 {len(stems)} 篇八股笔记合并整理，快速过一遍要点用。\n\n"
        for stem in stems:
            body += f"## {stem}\n\n{load_note(stem)}\n\n---\n\n"
        summary = make_summary(body)
        api("POST", "/api/v1/admin/articles", {
            "title": title, "slug": slug, "summary": summary, "contentMd": body,
            "categoryId": java_id, "status": 0, "isTop": 0,
            "tagIds": [t for t in [tag_id] if t],
        }, token=token)
        print(f"已建草稿: {title}  ({len(body)} 字符)")


if __name__ == "__main__":
    if "BLOG_ADMIN_PASS" not in os.environ and not DRY_RUN:
        sys.exit("请先设置环境变量 BLOG_ADMIN_PASS")
    main()
