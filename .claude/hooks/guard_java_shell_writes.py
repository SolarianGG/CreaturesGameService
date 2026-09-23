"""PreToolUse hook: ask before a shell command writes to a Java source file.

Java sources are edited only through Edit/Write, so the Spotless hook and line-ending handling run
(AGENTS.md anti-pattern, L-22, L-28). Detection rule approved as D-134: the command names a `.java` path
AND contains a write indicator. A false positive costs one confirmation prompt.
"""

import json
import re
import sys

JAVA_PATH = re.compile(r"\.java\b")

# Command words must stand at the start of the command or after a separator / whitespace.
WORD = r"(?:^|[\s;&|(])"

WRITE_INDICATORS = [
    # "> file" / ">> file", but not "2>&1", ">/dev/null", "> $null"
    ("output redirect", re.compile(r"(?<![<>=-])>>?\s*(?!&|/dev/null|\$null)[^\s>]")),
    ("sed -i", re.compile(WORD + r"sed\b[^\n;&|]*\s-i")),
    ("tee", re.compile(WORD + r"tee\b")),
    ("heredoc", re.compile(r"<<")),
    ("cp / mv", re.compile(WORD + r"(?:cp|mv)\s")),
    ("PowerShell content cmdlet", re.compile(r"\b(?:Set-Content|Out-File|Add-Content)\b", re.IGNORECASE)),
    ("script interpreter", re.compile(WORD + r"(?:python3?|py|perl|node)(?:\.exe)?\s")),
]


def main() -> int:
    try:
        payload = json.load(sys.stdin)
    except ValueError:
        return 0
    if not isinstance(payload, dict) or payload.get("tool_name") not in ("Bash", "PowerShell"):
        return 0
    command = (payload.get("tool_input") or {}).get("command") or ""
    if not JAVA_PATH.search(command):
        return 0

    matched = [name for name, pattern in WRITE_INDICATORS if pattern.search(command)]
    if matched:
        json.dump(
            {
                "hookSpecificOutput": {
                    "hookEventName": "PreToolUse",
                    "permissionDecision": "ask",
                    "permissionDecisionReason": (
                        f"shell command may write a .java file ({', '.join(matched)}); Java sources are edited "
                        "only through Edit/Write (AGENTS.md anti-pattern L-22; D-132, D-134)"
                    ),
                }
            },
            sys.stdout,
        )
    return 0


if __name__ == "__main__":
    sys.exit(main())
