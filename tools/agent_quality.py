#!/usr/bin/env python3
"""Read-only quality agent: validate checked-in contracts, never mutate a PR.

Run: python3 tools/agent_quality.py [repository-root]
Not an LLM, not a substitute for Android tests or visual review.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

PALETTE = {
    "Primary": "1CB0F6",
    "Pressed": "1899D6",
    "Navy": "102A56",
    "Sky": "DDF4FF",
    "White": "FFFFFF",
}
REQUIRED_AGENTS = ("android-implementer", "quality-guardian", "visual-designer")


def audit(root: Path) -> list[str]:
    problems: list[str] = []

    def read(relative: str) -> str:
        path = root / relative
        if not path.is_file():
            problems.append(f"Missing required file: {relative}")
            return ""
        return path.read_text(encoding="utf-8")

    token_path = "app/src/main/java/com/sayvabr/assistentepedagogico/ui/ApDesignTokens.kt"
    theme_path = "app/src/main/java/com/sayvabr/assistentepedagogico/ui/ApTheme.kt"
    tokens = read(token_path)
    theme = read(theme_path)
    handbook = read("docs/DESIGN_SYSTEM.md")
    read("AGENTS.md")
    read(".github/copilot-instructions.md")

    for name, hexadecimal in PALETTE.items():
        if not re.search(r"\bval\s+" + name + r"\s*=\s*Color\(0xFF" + hexadecimal + r"\)", tokens):
            problems.append(f"Palette {name} must be defined as #{hexadecimal} in {token_path}")
        if "#" + hexadecimal not in handbook:
            problems.append(f"Design handbook lacks canonical #{hexadecimal}")
    for name in ("ApPalette", "ApShapeToken", "ApSpace", "ApSizeToken"):
        if not re.search(r"\bobject\s+" + name + r"\b", tokens):
            problems.append(f"Missing design token object: {name}")
    if "MaterialTheme(colorScheme = apColorScheme, shapes = apShapes, typography = apTypography" not in theme:
        problems.append("ApTheme must use centralized colors, shapes and typography")
    if "ApPalette.Primary" not in theme or "ApShapeToken" not in theme:
        problems.append("ApTheme does not consume shared design tokens")

    for agent in REQUIRED_AGENTS:
        relative = f".github/agents/{agent}.md"
        profile = read(relative)
        if profile and not re.match(r"\A---\nname:\s*" + re.escape(agent) + r"\ndescription:\s*\S.+?\n---\n", profile):
            problems.append(f"Invalid frontmatter in {relative}")
        if profile and "assistente-pedagogico" not in profile:
            problems.append(f"Agent {agent} lacks repository scope")

    workflows_dir = root / ".github/workflows"
    if not workflows_dir.is_dir():
        problems.append("Missing GitHub workflows directory")
    else:
        workflows = sorted(workflows_dir.glob("*.yml"))
        if not workflows:
            problems.append("No GitHub workflows found")
        for workflow in workflows:
            source = workflow.read_text(encoding="utf-8")
            if not re.search(r"(?m)^permissions:\s*\n\s+contents:\s*read\s*$", source):
                problems.append(f"{workflow.name}: expected read-only GITHUB_TOKEN permissions")
            if re.search(r"(?m)^\s*(pull_request_target|workflow_run):", source):
                problems.append(f"{workflow.name}: privileged event requires separate security review")
            if re.search(r"(?m)^\s+contents:\s*write\b", source):
                problems.append(f"{workflow.name}: auto-write access prohibited for quality agents")
        ci = read(".github/workflows/ci.yml")
        # The owner explicitly authorized disposable PR #15/#16 previews, and PR #19
        # preview for first-access testing on 20/09/2026. Reject unscoped distribution, production binaries,
        # missing test/provenance gates, or long-lived previews. Not a release approval.
        distributes = "app/build/outputs/apk/debug/app-debug.apk" in ci or "Upload owner-requested debug preview" in ci
        if distributes:
            required = (
                "github.event_name == 'pull_request' && (github.event.pull_request.number == 15 || github.event.pull_request.number == 16 || github.event.pull_request.number == 19)",
                "name: assistente-pedagogico-preview-${{ github.event.pull_request.head.sha }}",
                "uses: actions/upload-artifact@v4",
                "retention-days: 3",
                "test -s \"$apk\"",
                'test "$(git rev-parse HEAD)" = "$head_sha"',
                ":app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest",
                "if-no-files-found: error",
            )
            if not all(fragment in ci for fragment in required):
                problems.append("CI distributes a preview APK without scoped owner authorization and quality gates")
            if any(fragment in ci for fragment in ("app-release.apk", "app-release.aab", "retention-days: 90")):
                problems.append("CI preview must never distribute release binaries or long-lived artifacts")
        guard = read(".github/workflows/agent-quality.yml")
        if "python3 tools/agent_quality.py" not in guard:
            problems.append("Quality workflow does not run the deterministic audit")
    return problems


def main() -> int:
    root = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent
    issues = audit(root)
    if issues:
        print("QUALITY AGENT: FAIL")
        for item in issues:
            print(f"  - {item}")
        return 1
    print("QUALITY AGENT: PASS — repository contracts and workflow safety checks")
    print("Not checked: Kotlin compilation, instrumented tests, real Android visual quality or user approval.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
