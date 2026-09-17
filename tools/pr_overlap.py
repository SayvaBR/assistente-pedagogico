#!/usr/bin/env python3
"""Read-only sibling PR collision gate for parallel agents.

Uses only the ephemeral GitHub Actions token with pull-requests:read. It never
checks out another PR, modifies a branch, comments, or merges anything.
"""
from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Callable

MAX_PAGES = 20


def related_prs(current: dict, candidates: list[dict]) -> list[dict]:
    """Only siblings share a target branch; stacked PRs have different bases."""
    base = current.get("base", {}).get("ref")
    number = current.get("number")
    return sorted(
        (item for item in candidates
         if item.get("state") == "open"
         and item.get("number") != number
         and item.get("base", {}).get("ref") == base),
        key=lambda item: item["number"],
    ) if base else []


def overlap_report(current: dict, candidates: list[dict],
                   files_for_pr: Callable[[int], list[str]]) -> list[dict]:
    """Fetch changed files once for the current PR and once per sibling."""
    siblings = related_prs(current, candidates)
    if not siblings:
        return []
    own = set(files_for_pr(current["number"]))
    report = []
    for sibling in siblings:
        common = sorted(own.intersection(files_for_pr(sibling["number"])))
        if common:
            report.append({
                "number": sibling["number"],
                "paths": common,
                "blocking": any(is_risky_path(path) for path in common),
            })
    return report


def is_risky_path(path: str) -> bool:
    """Shared application, build, security or automation files must be reconciled."""
    return (path.startswith("app/") or path.startswith("tools/")
            or path.startswith(".github/workflows/")
            or path in ("AGENTS.md", "build.gradle.kts", "settings.gradle.kts"))


def github_get(repo: str, endpoint: str, token: str) -> object:
    url = "https://api.github.com/repos/" + repo + endpoint
    request = urllib.request.Request(url, headers={
        "Authorization": "Bearer " + token,
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "assistente-pedagogico-pr-overlap-readonly",
    })
    with urllib.request.urlopen(request, timeout=15) as response:
        return json.load(response)


def paginated(repo: str, endpoint: str, token: str) -> list[dict]:
    """Never report a partial file/PR listing as collision-free."""
    result: list[dict] = []
    for page in range(1, MAX_PAGES + 1):
        separator = "&" if "?" in endpoint else "?"
        batch = github_get(repo, f"{endpoint}{separator}per_page=100&page={page}", token)
        if not isinstance(batch, list):
            raise ValueError("GitHub API returned a non-list response")
        result.extend(batch)
        if len(batch) < 100:
            return result
    raise ValueError("Too many pages; refusing to infer that there are no overlaps")


def safe_text(text: object) -> str:
    """Keep filenames from injecting markup into Actions summaries."""
    return re.sub(r"[\r\n`<>]", "_", str(text))


def main() -> int:
    try:
        event_path = Path(os.environ["GITHUB_EVENT_PATH"])
        event = json.loads(event_path.read_text(encoding="utf-8"))
        current = event["pull_request"]
        repo = os.environ["GITHUB_REPOSITORY"]
        token = os.environ["GH_READ_TOKEN"]
        if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repo) or not token:
            raise ValueError("Invalid repository or absent read-only token")
        candidates = paginated(repo, "/pulls?state=open", token)
        report = overlap_report(current, candidates,
                                lambda number: [entry["filename"] for entry in paginated(
                                    repo, f"/pulls/{number}/files", token)])
        lines = ["## Agente de colisões entre PRs", "", "Somente PRs abertos com a mesma branch base; PRs empilhados são ignorados.", ""]
        if not report:
            lines.append("✅ Nenhum arquivo compartilhado entre PRs irmãos nesta verificação.")
        for item in report:
            severity = "BLOQUEIO: código compartilhado" if item["blocking"] else "AVISO: documentação compartilhada"
            lines.extend([f"### PR #{item['number']} — {severity}", ""])
            for path in item["paths"]:
                lines.append(f"- `{safe_text(path)}`")
            lines.append("")
        lines.append("Não houve merge, escrita ou alteração dos PRs. Resolver o diff conjunto antes de integrar.")
        message = "\n".join(lines) + "\n"
        summary = os.environ.get("GITHUB_STEP_SUMMARY")
        if summary:
            with open(summary, "a", encoding="utf-8") as output:
                output.write(message)
        print(message)
        return 1 if any(item["blocking"] for item in report) else 0
    except (KeyError, ValueError, TypeError, OSError, urllib.error.URLError) as error:
        print("PR overlap audit unavailable; failing closed: " + safe_text(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
