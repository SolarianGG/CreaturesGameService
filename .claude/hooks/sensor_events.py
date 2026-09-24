"""PostToolUse / PostToolUseFailure hook: record every ./gradlew run as a sensor event.

The event log is written by the harness, not by the agent, so sensor statistics and the failure signal do not
depend on self-reporting (D-215). One JSON line per run in `.claude/metrics/events.jsonl` (git-ignored, D-216,
D-219). Commands that do not run gradlew are ignored; the hook never blocks and fails silently.
"""

import json
import os
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

# gradlew in command position: at the start or after a separator, optionally behind a path or PowerShell's `&`
GRADLEW = re.compile(r"(?:^|[;&|(\n])\s*(?:&\s*)?(?:[^\s;&|]*[/\\])?gradlew(?:\.bat)?(?=\s|$)")
# the gradle arguments end at a separator, a newline or a redirect (`>`, `2>`, `<`)
COMMAND_END = re.compile(r"\s\d*[<>]|[;&|<>\n]")
FAILED_TASK = re.compile(r"> Task :(\S+) FAILED|Execution failed for task ':([^']+)'")
# Gradle options that take the next word as their value
OPTIONS_WITH_VALUE = {"--tests", "-x", "--exclude-task", "-p", "--project-dir", "--console", "--warning-mode"}


def project_dir() -> Path:
    env = os.environ.get("CLAUDE_PROJECT_DIR")
    return Path(env) if env else Path(__file__).resolve().parents[2]


def gradle_tasks(command: str) -> list[str]:
    tasks: list[str] = []
    for match in GRADLEW.finditer(command):
        rest = COMMAND_END.split(command[match.end() :], maxsplit=1)[0]
        words = iter(rest.split())
        for word in words:
            if word in OPTIONS_WITH_VALUE:
                next(words, None)
            elif not word.startswith("-"):
                tasks.append(word.strip("'\""))
    return tasks


def response_text(response: dict) -> str:
    return "\n".join(str(response.get(key) or "") for key in ("stdout", "stderr", "output", "error"))


def branch(root: Path) -> str:
    try:
        head = (root / ".git" / "HEAD").read_text(encoding="utf-8").strip()
    except OSError:
        return ""
    return head.removeprefix("ref: refs/heads/") if head.startswith("ref: ") else head[:12]


def build_event(payload: dict, root: Path) -> dict | None:
    tool_input = payload.get("tool_input") or {}
    command = tool_input.get("command") or ""
    # a background command reports at launch, before gradle has a result
    if not GRADLEW.search(command) or tool_input.get("run_in_background"):
        return None
    raw = payload.get("tool_response")
    response = raw if isinstance(raw, dict) else {"output": raw} if isinstance(raw, str) else {}
    exit_code = response.get("exit_code")
    failed_event = payload.get("hook_event_name") == "PostToolUseFailure"
    text = response_text(response)
    failed_tasks = list(dict.fromkeys(a or b for a, b in FAILED_TASK.findall(text)))
    # a pipe (`./gradlew ... | tail`) hides gradle's exit code, so the output decides as well
    passed = not failed_event and exit_code in (0, None) and not failed_tasks and "BUILD FAILED" not in text
    return {
        "time": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "session": payload.get("session_id") or "",
        "branch": branch(root),
        "tool": payload.get("tool_name"),
        "command": command,
        "tasks": gradle_tasks(command),
        "exit_code": exit_code,
        "interrupted": bool(response.get("interrupted")),
        "result": "PASS" if passed else "FAIL",
        "failed_tasks": failed_tasks,
    }


def metrics_dir(root: Path) -> Path:
    return root / ".claude" / "metrics"


def append_event(root: Path, event: dict) -> None:
    log = metrics_dir(root) / "events.jsonl"
    log.parent.mkdir(parents=True, exist_ok=True)
    with log.open("a", encoding="utf-8") as out:
        out.write(json.dumps(event) + "\n")


def read_events(root: Path) -> list[dict]:
    try:
        lines = (metrics_dir(root) / "events.jsonl").read_text(encoding="utf-8").splitlines()
    except OSError:
        return []
    events = []
    for line in lines:
        try:
            event = json.loads(line)
        except ValueError:
            continue
        if isinstance(event, dict):
            events.append(event)
    return events


def failed_tasks(event: dict) -> list[str]:
    """Failed tasks of an event; without a `> Task :x FAILED` line (e.g. output piped through tail) the requested
    tasks failed. Shared by the failure signal and `scripts/harness/metrics.py`."""
    return event.get("failed_tasks") or event.get("tasks") or ["gradlew"]


def parse_time(value: str) -> datetime:
    return datetime.strptime(value, "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)


# --- failure signal (D-223, D-224, D-227, D-228) -------------------------------------------------------------------

FAILS_IN_A_ROW = 3
MINUTES_WITHOUT_GREEN = 60
RESETTING_TASKS = {"check", "build"}


def open_failures(events: list[dict], branch_name: str) -> dict[str, dict]:
    """Failed tasks on the branch not yet fixed by a green run of the same task, `check` or `build` (D-228)."""
    failures: dict[str, dict] = {}
    for event in events:
        if event.get("branch") != branch_name:
            continue
        tasks = event.get("tasks") or []
        if event.get("result") == "PASS":
            if RESETTING_TASKS & set(tasks):
                failures.clear()
            for task in tasks:
                failures.pop(task, None)
            continue
        for task in failed_tasks(event):
            entry = failures.setdefault(task, {"count": 0, "first": event.get("time", "")})
            entry["count"] += 1
    return failures


def failure_triggers(
    events: list[dict], branch_name: str, now: datetime, kinds: tuple[str, ...] = ("t1", "t2")
) -> dict[str, str]:
    """Active triggers of `kinds` (1: fails in a row, 2: failing too long) as {episode key: message}."""
    active = {}
    for task, entry in open_failures(events, branch_name).items():
        if "t1" in kinds and entry["count"] >= FAILS_IN_A_ROW:
            active[f"t1|{branch_name}|{task}"] = (
                f"{task} failed {entry['count']} times in a row on {branch_name} without a green run of it"
            )
        if "t2" not in kinds:
            continue
        try:
            minutes = (now - parse_time(entry["first"])).total_seconds() / 60
        except ValueError:
            continue
        if minutes > MINUTES_WITHOUT_GREEN:
            active[f"t2|{branch_name}|{task}"] = (
                f"{task} has been failing on {branch_name} for {int(minutes)} min "
                f"(> {MINUTES_WITHOUT_GREEN}) without a green run of it"
            )
    return active


def load_state(root: Path) -> dict:
    """Episode state (D-227): `fired` trigger keys and `pending_user` messages queued for Stop (D-231)."""
    try:
        state = json.loads((metrics_dir(root) / "signal_state.json").read_text(encoding="utf-8"))
    except (OSError, ValueError):
        state = {}
    return state if isinstance(state, dict) else {}


def save_state(root: Path, state: dict) -> None:
    path = metrics_dir(root) / "signal_state.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(state, indent=1), encoding="utf-8")


def new_signals(state: dict, active: dict[str, str], scope: tuple[str, ...]) -> list[str]:
    """Messages of triggers not fired yet in this episode; keys with a `scope` prefix no longer active are reset."""
    fired = [key for key in state.get("fired", []) if not key.startswith(scope) or key in active]
    state["fired"] = fired + [key for key in active if key not in fired]
    return [message for key, message in active.items() if key not in fired]


def user_output(messages: list[str]) -> dict:
    text = "; ".join(messages)
    return {
        "systemMessage": f"Quality signal (D-223): {text}",
        "terminalSequence": f"\x1b]9;GameService quality signal: {text[:200]}\x07",
    }


def agent_output(event_name: str, messages: list[str]) -> dict:
    return {
        "hookSpecificOutput": {
            "hookEventName": event_name,
            "additionalContext": (
                f"Quality signal (D-223): {'; '.join(messages)}. Stop working on the fix: record a blocker in "
                "docs/STATE.md and ask the user how to proceed (D-224)."
            ),
        }
    }


def main() -> int:
    try:
        payload = json.load(sys.stdin)
    except ValueError:
        return 0
    if not isinstance(payload, dict) or payload.get("tool_name") not in ("Bash", "PowerShell"):
        return 0
    root = project_dir()
    event = build_event(payload, root)
    if event is None:
        return 0
    try:
        append_event(root, event)
        branch_name = event["branch"]
        active = failure_triggers(read_events(root), branch_name, datetime.now(timezone.utc))
        state = load_state(root)
        messages = new_signals(state, active, (f"t1|{branch_name}|", f"t2|{branch_name}|"))
        # a PostToolUse systemMessage shows only in the expanded view, so the Stop hook shows it (D-231)
        state["pending_user"] = [*state.get("pending_user", []), *messages]
        save_state(root, state)
    except OSError:
        return 0
    if messages:
        json.dump(agent_output(payload.get("hook_event_name") or "PostToolUse", messages), sys.stdout)
    return 0


if __name__ == "__main__":
    sys.exit(main())
