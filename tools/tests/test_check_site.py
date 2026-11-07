import importlib.util
import io
import json
from pathlib import Path
import unittest
import urllib.error

spec = importlib.util.spec_from_file_location("check_site", Path(__file__).resolve().parents[1] / "check_site.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class Response(io.BytesIO):
    status = 200


class CheckSiteTest(unittest.TestCase):
    def opener(self, request, timeout):
        self.assertEqual(timeout, 10)
        path = request.full_url.split("https://test.example")[1]
        if path == "/api/v1/notes":
            raise urllib.error.HTTPError(request.full_url, 401, "Unauthorized", {}, None)
        if path == "/api/v1/rss":
            return Response(b'<rss version="2.0"><channel/></rss>')
        if path.startswith("/api/v1/articles"):
            return Response(json.dumps({"code": 200, "data": {"list": []}}).encode())
        return Response(b'<html><div id="app"></div></html>')

    def test_healthy_site_and_protected_notes(self):
        result = module.check("https://test.example/", self.opener)
        self.assertEqual(len(result), 6)
        self.assertTrue(all(r["ok"] for r in result))

    def test_http_200_cannot_hide_application_errors_or_private_exposure(self):
        result = module.check("https://test.example", lambda *a, **kw: Response(b'{"code":500}'))
        self.assertTrue(all(not r["ok"] for r in result))

    def test_network_failure_is_reported_without_stopping_other_checks(self):
        def offline(*args, **kwargs):
            raise TimeoutError("request timed out")
        result = module.check("https://test.example", offline)
        self.assertEqual(len(result), 6)
        self.assertTrue(all(not r["ok"] for r in result))
