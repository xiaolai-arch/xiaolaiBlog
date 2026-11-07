#!/usr/bin/env python3
"""Bounded, read-only checks of the public site and its database-backed API."""
import argparse
import json
import sys
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET


def check(base_url, open_url=urllib.request.urlopen):
    results = []
    for path, kind in [("/", "html"), ("/gallery", "html"), ("/tools/markdown", "html"),
                       ("/api/v1/rss", "rss"), ("/api/v1/articles?page=1&size=1", "articles"),
                       ("/api/v1/notes", "private")]:
        request = urllib.request.Request(base_url.rstrip("/") + path, headers={"User-Agent": "xiaolai-health/1.0"})
        try:
            with open_url(request, timeout=10) as response:
                body = response.read(2 * 1024 * 1024)
                status = response.status
            if kind == "private":
                raise ValueError("private endpoint accepted an anonymous request")
            if status != 200:
                raise ValueError("unexpected HTTP status: " + str(status))
            if kind == "html" and b'<div id="app"' not in body:
                raise ValueError("missing Vue app document")
            if kind == "rss" and ET.fromstring(body).tag != "rss":
                raise ValueError("invalid RSS document")
            if kind == "articles":
                data = json.loads(body)
                if data.get("code") != 200 or not isinstance(data.get("data", {}).get("list"), list):
                    raise ValueError("article API returned an invalid result")
            results.append({"path": path, "ok": True})
        except urllib.error.HTTPError as error:
            if kind == "private" and error.code == 401:
                results.append({"path": path, "ok": True})
            else:
                results.append({"path": path, "ok": False, "error": "HTTP " + str(error.code)})
            error.close()
        except Exception as error:
            results.append({"path": path, "ok": False, "error": str(error)})
    return results


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("base_url")
    args = parser.parse_args()
    results = check(args.base_url)
    print(json.dumps({"ok": all(r["ok"] for r in results), "checks": results}, ensure_ascii=False, indent=2))
    sys.exit(0 if all(r["ok"] for r in results) else 1)
