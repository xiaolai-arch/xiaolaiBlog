# -*- coding: utf-8 -*-
"""把学丞教育-图书课程清单.xlsx 转成四级备考页用的 JSON。

数据来源：学城网「我的书课」抓取会话（sess_74f2718e）的成品 Excel。
用法： python tools/build_cet4_data.py
输出 blog-frontend/public/data/cet4/index.json + 01.json..03.json
"""
import json
from pathlib import Path

import openpyxl

XLSX = Path(r"C:\Users\34191\.zcode\workspace\default\学丞教育-图书课程清单.xlsx")
OUT = Path(__file__).resolve().parent.parent / "blog-frontend" / "public" / "data" / "cet4"


def dur_seconds(dur: str) -> int:
    parts = [int(x) for x in dur.split(":")]
    sec = 0
    for p in parts:
        sec = sec * 60 + p
    return sec


def main():
    wb = openpyxl.load_workbook(XLSX, read_only=True)
    ws = wb["全部课程"]
    rows = list(ws.iter_rows(values_only=True))
    books = {}   # 书名 -> {章节 -> [item]}
    order = []
    for r in rows:
        if not r or not r[1] or not r[3]:
            continue
        if str(r[1]).startswith(("全部课程明细", "图书")):
            continue
        book, chapter, title, dur = str(r[1]).strip(), str(r[2] or "").strip(), str(r[3]).strip(), str(r[4] or "00:00").strip()
        if book not in books:
            books[book] = {}
            order.append(book)
        chapters = books[book]
        if chapter not in chapters:
            chapters[chapter] = []
        chapters[chapter].append({"title": title, "dur": dur})

    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob("*.json"):
        old.unlink()

    index = []
    total_items = 0
    total_sec = 0
    for bi, book in enumerate(order, 1):
        key = f"{bi:02d}"
        chapters_out: list = []
        item_count = 0
        book_sec = 0
        for chapter, items in books[book].items():
            # 章节可能带两级（"【低频词汇】 / 09  Day 33"），同章合并
            if " / " in chapter:
                ch_title, sec_title = [x.strip() for x in chapter.split(" / ", 1)]
            else:
                ch_title, sec_title = chapter, ""
            ch = next((c for c in chapters_out if c["title"] == ch_title), None)
            if ch is None:
                ch = {"title": ch_title, "sections": []}
                chapters_out.append(ch)
            entries = []
            for it in items:
                sec = dur_seconds(it["dur"])
                book_sec += sec
                entries.append({
                    "id": f"{bi}-{item_count + 1}",
                    "title": it["title"],
                    "dur": it["dur"],
                    "sec": sec,
                })
                item_count += 1
            ch["sections"].append({"title": sec_title, "items": entries})
        h, m = book_sec // 3600, book_sec % 3600 // 60
        data = {
            "name": book,
            "itemsTotal": item_count,
            "duration": f"{h} 小时 {m} 分钟",
            "chapters": chapters_out,
        }
        (OUT / f"{key}.json").write_text(
            json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        index.append({"key": key, "name": book, "itemsTotal": item_count})
        total_items += item_count
        total_sec += book_sec
        print(f"{book}: {item_count} 节, {h}小时{m}分")

    th, tm = total_sec // 3600, total_sec % 3600 // 60
    (OUT / "index.json").write_text(
        json.dumps({"categories": index, "totalItems": total_items,
                    "totalDuration": f"{th} 小时 {tm} 分钟"},
                   ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"完成 {len(order)} 本书, 共 {total_items} 节, {th}小时{tm}分")


if __name__ == "__main__":
    main()
