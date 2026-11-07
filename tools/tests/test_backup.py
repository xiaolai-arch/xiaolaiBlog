import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]


class BackupTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.bin = self.base / 'bin'
        self.bin.mkdir()
        for name, code in {
            'mysql': 'echo "${BACKUP_NON_TRANSACTIONAL:-0}"',
            'mysqldump': 'echo "CREATE TABLE backup_test(id INT);"; [ "${BACKUP_FAIL_DUMP:-}" != 1 ]',
            'systemctl': 'echo "[Service]"',
            'nginx': 'echo "# nginx config"',
            'flock': 'exit 0',
        }.items():
            path = self.bin / name
            path.write_text('#!/usr/bin/env bash\n' + code + '\n')
            path.chmod(0o755)
        self.env = dict(os.environ, PATH=str(self.bin) + ':' + os.environ['PATH'])
        backend = self.base / 'backend'
        frontend = self.base / 'frontend'
        uploads = self.base / 'uploads'
        backend.mkdir()
        (frontend / 'dist').mkdir(parents=True)
        uploads.mkdir()
        (backend / 'blog.env').write_text('DB_USERNAME=test\nDB_PASSWORD=private-password\n')
        (backend / 'blog-1.0.0.jar').write_text('old version')
        (frontend / 'dist/index.html').write_text('old document')
        (uploads / 'photo.jpg').write_bytes(b'photo')
        self.backups = self.base / 'backups'
        self.command = ['bash', str(ROOT / 'tools/backup_server.sh'), str(backend), str(frontend), str(self.backups), str(uploads)]

    def test_complete_backup_has_verified_checksums_and_private_permissions(self):
        result = subprocess.run(self.command, env=self.env, capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertNotIn('private-password', result.stdout + result.stderr)
        backup = next(self.backups.glob('backup-*'))
        checked = subprocess.run(['shasum', '-a', '256', '-c', 'SHA256SUMS'], cwd=backup, capture_output=True)
        self.assertEqual(checked.returncode, 0, checked.stderr)
        self.assertEqual(backup.stat().st_mode & 0o777, 0o700)
        self.assertEqual((backup / 'blog.env').stat().st_mode & 0o777, 0o600)

    def test_dump_failure_does_not_publish_successful_backup(self):
        self.env['BACKUP_FAIL_DUMP'] = '1'
        result = subprocess.run(self.command, env=self.env, capture_output=True, text=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(list(self.backups.glob('backup-*')), [])
        self.assertNotIn('BACKUP_PATH=', result.stdout)

    def test_non_transactional_tables_stop_backup(self):
        self.env['BACKUP_NON_TRANSACTIONAL'] = '1'
        result = subprocess.run(self.command, env=self.env, capture_output=True, text=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(list(self.backups.glob('backup-*')), [])

    def test_retention_keeps_recent_backups_and_pinned_release_backup(self):
        self.backups.mkdir()
        old = []
        for index in range(4):
            path = self.backups / ('backup-20200101T00000%dZ-1' % index)
            path.mkdir()
            (path / 'SHA256SUMS').write_text('old checksums')
            old.append(path)
        (old[0] / 'PINNED').touch()
        self.env['BACKUP_KEEP'] = '2'
        result = subprocess.run(self.command, env=self.env, capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue(old[0].exists())
        self.assertTrue(old[3].exists())
        self.assertFalse(old[1].exists())
        self.assertFalse(old[2].exists())
