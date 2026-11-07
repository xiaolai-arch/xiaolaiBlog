"""Exercise server deployment helpers with fake services and temporary directories."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import tarfile

ROOT = Path(__file__).resolve().parents[2]


class DeployRegressionTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="xiaolai-deploy-test-")
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.bin = self.base / "bin"
        self.bin.mkdir()
        self.log = self.base / "commands.log"
        self.env = dict(os.environ, PATH=f"{self.bin}:{os.environ['PATH']}", DEPLOY_TEST_LOG=str(self.log))
        scripts = {
            "mysql": 'echo mysql >> "$DEPLOY_TEST_LOG"\n[ "${DEPLOY_TEST_MIGRATION_FAIL:-}" != 1 ]',
            "systemctl": 'echo "systemctl $*" >> "$DEPLOY_TEST_LOG"',
            "curl": 'echo "curl $*" >> "$DEPLOY_TEST_LOG"\n[ "${DEPLOY_TEST_HEALTH_FAIL:-}" != 1 ]',
            "chown": "exit 0",
            "sleep": "exit 0",
        }
        for name, source in scripts.items():
            path = self.bin / name
            path.write_text("#!/usr/bin/env bash\n" + source + "\n")
            path.chmod(0o755)
        self.site = self.base / "site"
        self.stage = self.site / "release-test"
        self.stage.mkdir(parents=True)

    def backend(self):
        (self.site / "blog.env").write_text("DB_USERNAME=test\nDB_PASSWORD=test\n")
        (self.site / "blog-1.0.0.jar").write_text("old backend")
        (self.stage / "blog-1.0.0.jar").write_text("new backend")
        for name in ["note.sql", "progress.sql", "migration_token_version.sql"]:
            (self.stage / name).write_text("SELECT 1;\n")
        return ["bash", str(ROOT / "tools/deploy_backend.sh"), str(self.site), str(self.stage), "http://127.0.0.1:8081/api/v1/rss"]

    def frontend(self):
        dist = self.site / "dist"
        (dist / "assets").mkdir(parents=True)
        (dist / "assets/old-hash.js").write_text("old chunk")
        (dist / "index.html").write_text("old index")
        release = self.base / "payload"
        (release / "assets").mkdir(parents=True)
        (release / "assets/new-hash.js").write_text("new chunk")
        (release / "index.html").write_text("new index")
        with tarfile.open(self.stage / "dist.tgz", "w:gz") as archive:
            for path in release.rglob("*"):
                archive.add(path, arcname="./" + str(path.relative_to(release)), recursive=False)
        return ["bash", str(ROOT / "tools/deploy_frontend.sh"), str(self.site), str(self.stage), "https://test.example"]

    def run_script(self, command):
        return subprocess.run(command, env=self.env, text=True, capture_output=True)

    def test_backend_health_is_remote_and_success_keeps_new_jar(self):
        result = self.run_script(self.backend())
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.site / "blog-1.0.0.jar").read_text(), "new backend")
        self.assertIn("http://127.0.0.1:8081/api/v1/rss", self.log.read_text())

    def test_backend_health_failure_returns_failure_and_restores_jar(self):
        self.env["DEPLOY_TEST_HEALTH_FAIL"] = "1"
        result = self.run_script(self.backend())
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual((self.site / "blog-1.0.0.jar").read_text(), "old backend")
        self.assertEqual(self.log.read_text().count("systemctl restart blog"), 2)

    def test_migration_failure_stops_before_restart(self):
        self.env["DEPLOY_TEST_MIGRATION_FAIL"] = "1"
        result = self.run_script(self.backend())
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual((self.site / "blog-1.0.0.jar").read_text(), "old backend")
        self.assertNotIn("systemctl", self.log.read_text())

    def test_frontend_keeps_old_chunks_and_publishes_new_document(self):
        result = self.run_script(self.frontend())
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.site / "dist/index.html").read_text(), "new index")
        self.assertEqual((self.site / "dist/assets/old-hash.js").read_text(), "old chunk")
        self.assertEqual((self.site / "dist/assets/new-hash.js").read_text(), "new chunk")

    def test_frontend_health_failure_restores_document(self):
        self.env["DEPLOY_TEST_HEALTH_FAIL"] = "1"
        result = self.run_script(self.frontend())
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual((self.site / "dist/index.html").read_text(), "old index")


if __name__ == "__main__":
    unittest.main()
