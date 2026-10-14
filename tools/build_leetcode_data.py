# -*- coding: utf-8 -*-
"""把 leetcode-problem-list 仓库的题单.md（原帖全文）转成博客用的 JSON。

与旧版不同：不去重、不重排，保留原帖「章 -> 节 -> 题目列表」的原始结构与顺序，
同一题出现在多个小节就展示多次。只提取题目行和小节内的讲解链接行，
不搬运原帖的散文讲解。

用法：
  git clone --depth 1 https://gitee.com/li-dawei10086/leetcode-problem-list.git /tmp/leetcode-problem-list
  python tools/build_leetcode_data.py /tmp/leetcode-problem-list
"""
import json
import re
import sys
from pathlib import Path

SRC = Path(sys.argv[1] if len(sys.argv) > 1 else "/tmp/leetcode-problem-list")
OUT = Path(__file__).resolve().parent.parent / "blog-frontend" / "public" / "data" / "leetcode"

PROBLEM_ITEM = re.compile(r"^- \[(.+?)\]\((https://leetcode\.cn/problems/[^)]+)\)\s*(.*)$")
ANY_LIST_LINK = re.compile(r"^- \[(.+?)\]\((https?://[^)]+)\)\s*$")
PARA_LINK = re.compile(r"^\[(.+?)\]\((https://leetcode\.cn/[^)]+)\)\s*$")
SOURCE_LINK = re.compile(r"\*\*来源\*\*：\[.*?\]\((https://leetcode\.cn/discuss/[^)]+)\)")
PID_PATTERNS = [
    re.compile(r"^(\d+)\."),
    re.compile(r"^(LCP \d+)\."),
    re.compile(r"^(LCR \d+)\."),
    re.compile(r"^(面试题 \d+\.\d+)\."),
]
SKIP_CHAPTERS = ("目录", "关联题单", "算法题单")


def extract_pid(label: str):
    for pat in PID_PATTERNS:
        m = pat.match(label)
        if m:
            return m.group(1)
    return label.split(" ")[0] if " " in label else label


def parse_category(md_path: Path):
    text = md_path.read_text(encoding="utf-8")
    title = ""
    source = ""
    chapters = []          # [{title, sections:[{title, notes, problems}]}]
    chapter = None
    section = None
    seen_first_chapter = False

    for raw in text.split("\n"):
        line = raw.strip()
        if line.startswith("# ") and not title:
            title = line[2:].strip()
            continue
        if line.startswith(">"):
            m = SOURCE_LINK.search(line)
            if m:
                source = m.group(1)
            continue
        if not line or line.startswith(("---", "![")):
            continue

        if line.startswith("## "):
            chapter_title = line[3:].strip()
            if chapter_title in SKIP_CHAPTERS:
                chapter, section = None, None
                continue
            chapter = {"title": chapter_title, "sections": []}
            chapters.append(chapter)
            section = None
            seen_first_chapter = True
            continue

        if line.startswith("### "):
            section = {"title": line[4:].strip(), "notes": [], "problems": []}
            if chapter is not None:
                chapter["sections"].append(section)
            continue

        if not seen_first_chapter or chapter is None:
            continue

        m = PROBLEM_ITEM.match(line)
        if m:
            label, url, suffix = m.groups()
            suffix = suffix.strip()
            if section is None:
                section = {"title": "", "notes": [], "problems": []}
                chapter["sections"].append(section)
            section["problems"].append({
                "pid": extract_pid(label),
                "label": label,
                "url": url,
                "suffix": suffix,
            })
            continue

        # 小节里的独立讲解链接 / 非题目列表链接（如入门计划）
        m = PARA_LINK.match(line) or ANY_LIST_LINK.match(line)
        if m and "#" not in m.group(2):
            label, url = m.groups()
            if section is None:
                section = {"title": "", "notes": [], "problems": []}
                chapter["sections"].append(section)
            section["notes"].append({"label": label, "url": url})

    # 去掉完全没有题目的章节（如「思考」）和小节
    for c in chapters:
        c["sections"] = [s for s in c["sections"] if s["problems"]]
    chapters = [c for c in chapters if c["sections"]]

    pids = set()
    count = 0
    for c in chapters:
        for s in c["sections"]:
            for p in s["problems"]:
                count += 1
                pids.add(p["pid"])
    return {
        "title": title,
        "source": source,
        "itemsTotal": count,
        "uniqueTotal": len(pids),
        "chapters": chapters,
    }


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    # 清掉旧格式文件，避免残留
    for old in OUT.glob("*.json"):
        old.unlink()
    index = []
    for d in sorted(SRC.iterdir()):
        if not d.is_dir():
            continue
        md = d / "题单.md"
        if not md.exists():
            continue
        name = re.sub(r"^\d+-", "", d.name)
        data = parse_category(md)
        data["name"] = name
        (OUT / f"{d.name[:2]}.json").write_text(
            json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        index.append({"key": d.name[:2], "name": name, "uniqueTotal": data["uniqueTotal"]})
        print(f"{d.name}: {data['itemsTotal']} 条目 / 去重 {data['uniqueTotal']} 题, "
              f"{len(data['chapters'])} 章")

    (OUT / "index.json").write_text(
        json.dumps({"categories": index}, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8")
    print(f"完成 {len(index)} 个专题")


if __name__ == "__main__":
    main()
