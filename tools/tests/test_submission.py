import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from prepare_hub_submission import prepare

class SubmissionManifestTest(unittest.TestCase):
    def test_real_commit_and_repository_form_two_line_manifest(self):
        commit = "0123456789abcdef0123456789abcdef01234567"
        self.assertEqual(prepare("https://github.com/tkoh-v7/2005scape", commit),
                         "repository=https://github.com/tkoh-v7/2005scape.git\ncommit=" + commit + "\n")

    def test_missing_placeholder_or_injected_commit_is_rejected(self):
        for commit in ("", "YOUR_COMMIT", "0" * 40, "a" * 40, "abcd\nrepository=evil"):
            with self.assertRaises(ValueError):
                prepare("https://github.com/tkoh-v7/2005scape.git", commit)

    def test_repository_cannot_inject_manifest_fields(self):
        for repository in ("http://github.com/tkoh-v7/2005scape", "https://example.com/repo", "https://github.com/tkoh-v7/2005scape\ncommit=evil"):
            with self.assertRaises(ValueError):
                prepare(repository, "0123456789abcdef0123456789abcdef01234567")
