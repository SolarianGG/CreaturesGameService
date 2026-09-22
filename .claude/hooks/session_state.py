"""SessionStart hook: inject the current project state into the agent context.

Prints docs/STATE.md and the active slice file referenced in it (D-38).
Plain-text stdout of a SessionStart hook is added to the context.
"""

from __future__ import annotations

import os
import re
import sys

ACTIVE_SLICE = re.compile(r"Active slice:\s*`?(docs/slices/[^\s`]+\.md)")


def read(path: str) -> str | None:
    try:
        with open(path, encoding="utf-8") as f:
            return f.read()
    except OSError:
        return None


def main() -> int:
    sys.stdout.reconfigure(encoding="utf-8")
    project_dir = os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()

    state = read(os.path.join(project_dir, "docs", "STATE.md"))
    if state is None:
        print("[session_state] docs/STATE.md not found. Create it before starting work (docs/HARNESS.md, Memory and state).")
        return 0

    print("[session_state] Current project state (docs/STATE.md):\n")
    print(state)

    match = ACTIVE_SLICE.search(state)
    if match:
        slice_path = match.group(1)
        slice_text = read(os.path.join(project_dir, *slice_path.split("/")))
        if slice_text is None:
            print(f"\n[session_state] WARNING: active slice file {slice_path} referenced in STATE.md does not exist.")
        else:
            print(f"\n[session_state] Active slice ({slice_path}):\n")
            print(slice_text)
    return 0


if __name__ == "__main__":
    sys.exit(main())
