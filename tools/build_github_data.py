# -*- coding: utf-8 -*-
"""拉取 GitHub 仓库数据 + 合并人工整理介绍 → 前端 JSON。

介绍/标签/精选在 tools/github-projects.json 手动维护；
本脚本负责把语言、星标数、更新时间、链接等事实数据从 GitHub API 合并进去。

用法: python tools/build_github_data.py
输出: blog-frontend/public/data/github/projects.json
"""
import json
import sys
import urllib.request
from datetime import date
from pathlib import Path

USER = "xiaolai-arch"
ROOT = Path(__file__).resolve().parent.parent
CURATION = Path(__file__).resolve().parent / "github-projects.json"
OUT = ROOT / "blog-frontend" / "public" / "data" / "github" / "projects.json"


def gh(path: str):
    req = urllib.request.Request(
        f"https://api.github.com{path}",
        headers={"User-Agent": "blog-tool/1.0", "Accept": "application/vnd.github+json"},
    )
    return json.load(urllib.request.urlopen(req, timeout=30))


def main():
    curation = json.loads(CURATION.read_text(encoding="utf-8"))
    user = gh(f"/users/{USER}")
    repos = {r["name"]: r for r in gh(f"/users/{USER}/repos?per_page=100")}

    projects = []
    missing = []
    for c in curation["projects"]:
        r = repos.get(c["repo"])
        if not r:
            missing.append(c["repo"])
            continue
        projects.append({
            "name": r["name"],
            "desc": c.get("desc", ""),
            "category": c.get("category", ""),
            "badge": c.get("badge", ""),
            "featured": bool(c.get("featured")),
            "tags": c.get("tags", []),
            "highlights": c.get("highlights", []),
            "url": r["html_url"],
            "language": r.get("language") or "",
            "stars": r["stargazers_count"],
            "forks": r["forks_count"],
            "pushedAt": (r.get("pushed_at") or "")[:10],
            "homepage": r.get("homepage") or "",
        })
    if missing:
        print("警告：以下仓库未在 GitHub 找到 →", missing, file=sys.stderr)

    data = {
        "user": {
            "login": user["login"],
            "html_url": user["html_url"],
            "avatar": user["avatar_url"],
            "publicRepos": user["public_repos"],
        },
        "updatedAt": date.today().isoformat(),
        "projects": projects,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(data, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"写入 {len(projects)} 个项目 → {OUT}")
    for p in projects:
        print(f"  {p['name']:16s} [{p['language'] or '-'}] pushed={p['pushedAt']}")


if __name__ == "__main__":
    main()
