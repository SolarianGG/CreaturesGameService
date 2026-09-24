"""PreToolUse hook: require explicit user confirmation before editing config files.

Config list is approved in docs/HARNESS.md (section "Config files").
"""

import fnmatch
import json
import os
import re
import sys

CONFIG_PATTERNS = [
    # build
    "build.gradle",
    "settings.gradle",
    "gradle.properties",
    "gradle/*",
    # application
    "src/main/resources/application*.properties",
    "src/main/resources/application*.yml",
    "src/main/resources/application*.yaml",
    "src/test/resources/application*.properties",
    "src/test/resources/application*.yml",
    "src/test/resources/application*.yaml",
    # infrastructure & CI
    "docker-compose*",
    "compose*.yml",
    "compose*.yaml",
    "Dockerfile*",
    ".dockerignore",
    ".env.example",
    "docker/*",
    ".github/*",
    "*prometheus*",
    "*grafana*",
    # sensor & harness configs
    "config/*",
    ".claude/*",
    "AGENTS.md",
]


def main() -> int:
    payload = json.load(sys.stdin)
    tool_input = payload.get("tool_input", {})
    file_path = tool_input.get("file_path") or tool_input.get("notebook_path")
    if not file_path:
        return 0

    # Git Bash style "/c/Users/..." -> "C:/Users/..."
    match = re.match(r"^/([a-zA-Z])/(.*)$", file_path)
    if os.name == "nt" and match:
        file_path = f"{match.group(1)}:/{match.group(2)}"

    project_dir = os.environ.get("CLAUDE_PROJECT_DIR") or payload.get("cwd") or os.getcwd()
    try:
        rel = os.path.relpath(os.path.abspath(file_path), os.path.abspath(project_dir))
    except ValueError:
        return 0  # different drive -> outside project
    rel = rel.replace("\\", "/")
    if rel.startswith("../"):
        return 0

    if any(fnmatch.fnmatch(rel, pattern) for pattern in CONFIG_PATTERNS):
        json.dump(
            {
                "hookSpecificOutput": {
                    "hookEventName": "PreToolUse",
                    "permissionDecision": "ask",
                    "permissionDecisionReason": f"'{rel}' is a config file (AGENTS.md: never modify configs without asking)",
                }
            },
            sys.stdout,
        )
    return 0


if __name__ == "__main__":
    sys.exit(main())
