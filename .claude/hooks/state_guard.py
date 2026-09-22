"""Stop hook: block stopping once if work changed but the state files did not (D-39).

A changed file (per git status) under src/, build.gradle or docs/ that is newer than
docs/STATE.md or the active slice file -> exit 2, stderr is fed back to the agent.
If the hook already blocked in this turn (stop_hook_active), stopping is allowed.
Fails open (exit 0) when git is unavailable.
"""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys

STATE_FILE = "docs/STATE.md"
SLICES_DIR = "docs/slices/"
ACTIVE_SLICE = re.compile(r"Active slice:\s*`?(docs/slices/[^\s`]+\.md)")
MAX_LISTED = 5


def is_watched(rel: str) -> bool:
    if rel == STATE_FILE or rel.startswith(SLICES_DIR):
        return False
    return rel.startswith("src/") or rel == "build.gradle" or rel.startswith("docs/")


def changed_files(project_dir: str) -> list[str] | None:
    try:
        result = subprocess.run(
            ["git", "status", "--porcelain", "-uall", "-z"],
            cwd=project_dir,
            capture_output=True,
            text=True,
            encoding="utf-8",
        )
    except OSError:
        return None
    if result.returncode != 0:
        return None

    files = []
    entries = result.stdout.split("\0")
    i = 0
    while i < len(entries):
        entry = entries[i]
        i += 1
        if len(entry) < 4:
            continue
        status, path = entry[:2], entry[3:]
        if "R" in status or "C" in status:
            i += 1  # skip the original path of a rename/copy
        if "D" in status:
            continue  # deleted: nothing to compare
        files.append(path)
    return files


def mtime(project_dir: str, rel: str) -> float | None:
    try:
        return os.path.getmtime(os.path.join(project_dir, *rel.split("/")))
    except OSError:
        return None


def main() -> int:
    payload = json.load(sys.stdin)
    if payload.get("stop_hook_active"):
        return 0

    project_dir = os.environ.get("CLAUDE_PROJECT_DIR") or payload.get("cwd") or os.getcwd()
    files = changed_files(project_dir)
    if files is None:
        return 0

    changes = []
    for rel in files:
        if is_watched(rel):
            changed_at = mtime(project_dir, rel)
            if changed_at is not None:
                changes.append((changed_at, rel))
    if not changes:
        return 0
    changes.sort(reverse=True)
    newest = changes[0][0]

    state_at = mtime(project_dir, STATE_FILE)
    if state_at is None:
        sys.stderr.write(f"{STATE_FILE} does not exist. Create it before stopping (docs/HARNESS.md, Memory and state).")
        return 2

    stale = []
    if newest > state_at:
        stale.append(STATE_FILE)
    try:
        with open(os.path.join(project_dir, *STATE_FILE.split("/")), encoding="utf-8") as f:
            match = ACTIVE_SLICE.search(f.read())
    except OSError:
        match = None
    if match:
        slice_at = mtime(project_dir, match.group(1))
        if slice_at is None or newest > slice_at:
            stale.append(match.group(1))

    if not stale:
        return 0
    listed = "\n".join(f"  - {rel}" for _, rel in changes[:MAX_LISTED])
    sys.stderr.write(
        f"State is stale: {', '.join(stale)} older than the latest changes:\n{listed}\n"
        "Update the state (position, next action, checklist/journal) before stopping."
    )
    return 2


if __name__ == "__main__":
    sys.exit(main())
