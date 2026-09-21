"""Synthetic tests; never need Android SDK, network or teacher data."""
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from agent_quality import audit, PALETTE, REQUIRED_AGENTS


class AuditContractTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        prefix = "app/src/main/java/com/sayvabr/assistentepedagogico/ui/"
        tokens = "\n".join(f"val {key} = Color(0xFF{value})" for key, value in PALETTE.items())
        tokens += "\n" + "\n".join(f"object {name} {{}}" for name in ("ApPalette", "ApShapeToken", "ApSpace", "ApSizeToken"))
        self.write(prefix + "ApDesignTokens.kt", tokens)
        self.write(prefix + "ApTheme.kt", "ApPalette.Primary ApShapeToken MaterialTheme(colorScheme = apColorScheme, shapes = apShapes, typography = apTypography")
        self.write("docs/DESIGN_SYSTEM.md", " ".join("#" + code for code in PALETTE.values()))
        self.write("AGENTS.md", "tests and privacy")
        self.write(".github/copilot-instructions.md", "local-first")
        for agent in REQUIRED_AGENTS:
            self.write(f".github/agents/{agent}.md", f"---\nname: {agent}\ndescription: Responsible code reviewer\n---\n\nOnly assistente-pedagogico")
        self.write(".github/workflows/ci.yml", "on:\n  pull_request:\npermissions:\n  contents: read\njobs:\n  check: {}")
        self.write(".github/workflows/agent-quality.yml", "on:\n  pull_request:\npermissions:\n  contents: read\njobs:\n  check:\n    steps:\n    - run: python3 tools/agent_quality.py")

    def write(self, name, content):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")

    def test_clean_fixture(self):
        self.assertEqual([], audit(self.root))

    def test_detects_wrong_color(self):
        path = self.root / "app/src/main/java/com/sayvabr/assistentepedagogico/ui/ApDesignTokens.kt"
        path.write_text(path.read_text().replace("0xFF1CB0F6", "0xFF00FF00"))
        self.assertTrue(any("Palette Primary" in issue for issue in audit(self.root)))

    def test_detects_privileged_workflow(self):
        self.write(".github/workflows/unsafe.yml", "on:\n  pull_request_target:\npermissions:\n  contents: write\n")
        issues = audit(self.root)
        self.assertTrue(any("privileged event" in issue for issue in issues))
        self.assertTrue(any("read-only" in issue for issue in issues))

    def test_detects_unwanted_apk_distribution(self):
        self.write(".github/workflows/ci.yml", "on:\n  pull_request:\npermissions:\n  contents: read\n# Upload owner-requested debug preview")
        self.assertTrue(any("distributes a preview APK" in issue for issue in audit(self.root)))

    def test_allows_only_explicitly_scoped_debug_preview(self):
        ci = "\n".join((
            "on:", "  pull_request:", "permissions:", "  contents: read",
            "- name: Build, unit test, lint and compile instrumentation tests",
            "  run: gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest",
            "- name: Check APK and record build provenance",
            "  if: github.event_name == 'pull_request' && (github.event.pull_request.number == 15 || github.event.pull_request.number == 16 || github.event.pull_request.number == 19 || github.event.pull_request.number == 20)",
            "  run: |", "    apk=app/build/outputs/apk/debug/app-debug.apk",
            "    test -s \"$apk\"",
            '    test "$(git rev-parse HEAD)" = "$head_sha"',
            "- name: Upload installable preview APK (3-day retention)",
            "  if: github.event_name == 'pull_request' && (github.event.pull_request.number == 15 || github.event.pull_request.number == 16 || github.event.pull_request.number == 19 || github.event.pull_request.number == 20)",
            "  uses: actions/upload-artifact@v4",
            "  with:",
            "    name: assistente-pedagogico-preview-${{ github.event.pull_request.head.sha }}",
            "    if-no-files-found: error", "    retention-days: 3",
        ))
        self.write(".github/workflows/ci.yml", ci)
        self.assertEqual([], audit(self.root))
        self.write(".github/workflows/ci.yml", ci.replace("github.event.pull_request.number == 15", "github.event.pull_request.number >= 1"))
        self.assertTrue(any("scoped owner authorization" in issue for issue in audit(self.root)))
        self.write(".github/workflows/ci.yml", ci.replace("retention-days: 3", "retention-days: 90"))
        self.assertTrue(any("long-lived" in issue or "scoped owner authorization" in issue for issue in audit(self.root)))


if __name__ == "__main__":
    unittest.main()
