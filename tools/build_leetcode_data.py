# -*- coding: utf-8 -*-
"""把 leetcode-problem-list 仓库的题目索引转成博客用的 JSON。

用法：
  git clone --depth 1 https://gitee.com/li-dawei10086/leetcode-problem-list.git /tmp/leetcode-problem-list
  python tools/build_leetcode_data.py /tmp/leetcode-problem-list

输出 blog-frontend/public/data/leetcode/index.json + 01.json..12.json
（只提取事实性索引：题号/题名/链接/章节；不含题单讲解正文）
"""
import json
import re
import sys
from pathlib import Path

SRC = Path(sys.argv[1] if len(sys.argv) > 1 else "/tmp/leetcode-problem-list")
OUT = Path(__file__).resolve().parent.parent / "blog-frontend" / "public" / "data" / "leetcode"

ROW = re.compile(r"^\|\s*\d+\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*\[题目\]\((https://[^)]+)\)\s*\|\s*(.+?)\s*\|$")
GUIDE_ROW = re.compile(r"^\|\s*(.+?)\s*\|\s*(https://[^)]+?)\s*\|$")
SOURCE = re.compile(r"由\s*\[原帖\]\((https://[^)]+)\)")


def parse_category(md_path: Path):
    text = md_path.read_text(encoding="utf-8")
    source = SOURCE.search(text)
    problems, guides = [], []
    in_problems = False
    for line in text.split("\n"):
        if line.startswith("## "):
            in_problems = "全部题目" in line
            continue
        if in_problems:
            m = ROW.match(line)
            if m:
                pid, title, url, section = m.groups()
                chapter = section.split(" / ")[0].strip()
                # 题号可能是 3 / LCR 180 / 面试题 16.06
                pid = pid.strip()
                problems.append({
                    "id": int(pid) if pid.isdigit() else pid,
                    "title": title,
                    "url": url,
                    "chapter": chapter,
                    "section": section,
                })
        gm = GUIDE_ROW.match(line)
        if gm and "leetcode.cn" in gm.group(2):
            label, url = gm.groups()
            if label not in ("说明", "---") and not label.startswith("---"):
                guides.append({"label": label, "url": url})
    return source.group(1) if source else "", problems, guides


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    index = []
    all_ids = set()
    for d in sorted(SRC.iterdir()):
        if not d.is_dir():
            continue
        md = d / "题目索引.md"
        if not md.exists():
            continue
        name = re.sub(r"^\d+-", "", d.name)
        source, problems, guides = parse_category(md)
        for p in problems:
            all_ids.add(p["id"])
        data = {
            "name": name,
            "source": source,
            "total": len(problems),
            "problems": problems,
            "guides": guides,
        }
        (OUT / f"{d.name[:2]}.json").write_text(
            json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        index.append({"key": d.name[:2], "name": name, "total": len(problems)})
        print(f"{d.name}: {len(problems)} 题, {len(guides)} 条讲解链接")

    index_data = {"categories": index, "uniqueTotal": len(all_ids)}
    (OUT / "index.json").write_text(
        json.dumps(index_data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"共 {len(index)} 个专题, 去重 {len(all_ids)} 题")


if __name__ == "__main__":
    main()
