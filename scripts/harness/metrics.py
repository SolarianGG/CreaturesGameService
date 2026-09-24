"""Development quality metrics over the harness data (SOL-147, D-225).

Subcommands (markdown output; Python 3 standard library only):
  cost               tokens and USD per slice from the Claude Code transcripts of this project (D-221, D-222, D-230)
  sensors            failures per sensor from the hook event log and the journal sensor lines (D-215, D-217)
  rules              "Violated rule" counts over docs/LESSONS.md (D-218, D-220)
  trace SOL-<n>      one slice end to end: Linear, decisions, sessions, sensor events, commits, cost

Sources: Claude Code transcripts (`~/.claude/projects/<project>/*.jsonl`, subagents included), the slice journals
(`docs/slices/*.md`), `docs/LESSONS.md` and the sensor event log `.claude/metrics/events.jsonl` (D-215).
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from dataclasses import dataclass, field
from datetime import datetime, timezone
from collections import Counter
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
# the event log format is owned by the hook that writes it
sys.path.insert(0, str(REPO_ROOT / ".claude" / "hooks"))
from sensor_events import failed_tasks, read_events  # noqa: E402

MTOK = 1_000_000

# USD per MTok: input, 5m cache write, 1h cache write, cache read, output (D-230, first-party API list prices)
PRICES: dict[str, tuple[float, float, float, float, float]] = {
    "claude-opus-5-5": (4.0, 5.0, 8.0, 0.20, 20.0),
    "claude-opus-5": (5.0, 6.25, 10.0, 0.50, 25.0),
    "claude-opus-4-8": (5.0, 6.25, 10.0, 0.50, 25.0),
    "claude-opus-4-7": (5.0, 6.25, 10.0, 0.50, 25.0),
    "claude-opus-4-6": (5.0, 6.25, 10.0, 0.50, 25.0),
    "claude-fable-5-1": (10.0, 12.5, 20.0, 0.25, 50.0),
    "claude-sonnet-5": (2.0, 2.5, 4.0, 0.20, 10.0),
    "claude-sonnet-4-6": (3.0, 3.75, 6.0, 0.30, 15.0),
    "claude-haiku-4-5": (1.0, 1.25, 2.0, 0.10, 5.0),
}
FAST_MODE_FACTOR = {"claude-opus-5-5": 2.0, "claude-opus-5": 2.0}
IGNORED_MODELS = {"<synthetic>"}
UNATTRIBUTED = "unattributed"

SLICE_BRANCH = re.compile(r"^slice/(SOL-\d+)-")
SLICE_FILE = re.compile(r"^(SOL-\d+)-.*\.md$")
SESSION_LINE = re.compile(r"^- (\d{4}-\d{2}-\d{2} \d{2}:\d{2}) \| session \| (\S+) \| (start|end)\s*$")


def default_transcripts(root: Path) -> Path:
    return Path.home() / ".claude" / "projects" / re.sub(r"[^A-Za-z0-9]", "-", str(root))


# --- transcripts --------------------------------------------------------------------------------------------------


@dataclass
class Message:
    session: str
    branch: str
    time: datetime
    model: str
    usage: dict


def read_messages(transcripts: Path) -> list[Message]:
    """Assistant messages with usage, one per API message (a message is logged once per content block)."""
    seen: set[str] = set()
    messages = []
    for path in sorted([*transcripts.glob("*.jsonl"), *transcripts.glob("*/subagents/*.jsonl")]):
        with path.open(encoding="utf-8") as lines:
            for line in lines:
                if '"usage"' not in line:  # cheap skip of user / tool-result lines before parsing
                    continue
                try:
                    record = json.loads(line)
                except ValueError:
                    continue
                if not isinstance(record, dict) or record.get("type") != "assistant":
                    continue
                message = record.get("message") or {}
                usage = message.get("usage")
                key = message.get("id") or record.get("requestId")
                if not usage or not key or key in seen or message.get("model") in IGNORED_MODELS:
                    continue
                seen.add(key)
                try:
                    when = datetime.fromisoformat(str(record.get("timestamp")).replace("Z", "+00:00"))
                except ValueError:
                    continue
                messages.append(
                    Message(
                        str(record.get("sessionId") or ""),
                        str(record.get("gitBranch") or ""),
                        when,
                        str(message.get("model") or ""),
                        usage,
                    )
                )
    return messages


# --- slices and journals ------------------------------------------------------------------------------------------


def slice_files(root: Path) -> dict[str, Path]:
    files = {}
    for path in sorted((root / "docs" / "slices").glob("*.md")):
        match = SLICE_FILE.match(path.name)
        if match:
            files[match.group(1)] = path
    return files


def slice_status(path: Path) -> str:
    match = re.search(r"^Status:\s*(\w[\w ]*?)\s*(?:\||$)", path.read_text(encoding="utf-8"), re.MULTILINE)
    return match.group(1) if match else ""


@dataclass
class Window:
    slice_id: str
    session: str
    start: datetime
    end: datetime | None = None


def journal_matches(root: Path, pattern: re.Pattern[str]) -> list[tuple[str, re.Match[str]]]:
    """(slice id, match) for every line of every slice file that matches `pattern`."""
    matches = []
    for slice_id, path in slice_files(root).items():
        for line in path.read_text(encoding="utf-8").splitlines():
            match = pattern.match(line)
            if match:
                matches.append((slice_id, match))
    return matches


def session_windows(root: Path) -> list[Window]:
    """Journal session lines -> time windows; a window ends at `end` or at the session's next `start` (D-222)."""
    marks = []
    for slice_id, match in journal_matches(root, SESSION_LINE):
        local = datetime.strptime(match.group(1), "%Y-%m-%d %H:%M").astimezone()
        marks.append((local.astimezone(timezone.utc), match.group(3), match.group(2), slice_id))
    windows: list[Window] = []
    open_windows: dict[str, Window] = {}
    for when, kind, session, slice_id in sorted(marks):
        previous = open_windows.pop(session, None)
        if previous is not None:
            previous.end = when
        if kind == "start":
            window = Window(slice_id, session, when)
            windows.append(window)
            open_windows[session] = window
    return windows


def attribute(message: Message, windows: list[Window]) -> str:
    match = SLICE_BRANCH.match(message.branch)
    if match:
        return match.group(1)
    for window in windows:
        if (
            window.session == message.session
            and window.start <= message.time
            and (window.end is None or message.time < window.end)
        ):
            return window.slice_id
    return UNATTRIBUTED


# --- cost ---------------------------------------------------------------------------------------------------------


@dataclass
class Cost:
    messages: int = 0
    input: int = 0
    cache_write: int = 0
    cache_read: int = 0
    output: int = 0
    usd: float = 0.0
    unknown_models: set[str] = field(default_factory=set)

    def add(self, message: Message) -> None:
        usage = message.usage
        split = usage.get("cache_creation") or {}
        write_1h = int(split.get("ephemeral_1h_input_tokens") or 0)
        write_5m = int(usage.get("cache_creation_input_tokens") or 0) - write_1h
        read = int(usage.get("cache_read_input_tokens") or 0)
        tokens = (int(usage.get("input_tokens") or 0), write_5m, write_1h, read, int(usage.get("output_tokens") or 0))
        self.merge(Cost(1, tokens[0], write_5m + write_1h, read, tokens[4]))
        prices = PRICES.get(message.model)
        if prices is None:
            self.unknown_models.add(message.model)
            return
        factor = FAST_MODE_FACTOR.get(message.model, 1.0) if usage.get("speed") == "fast" else 1.0
        self.usd += factor * sum(count * price for count, price in zip(tokens, prices)) / MTOK

    def merge(self, other: "Cost") -> None:
        self.messages += other.messages
        self.input += other.input
        self.cache_write += other.cache_write
        self.cache_read += other.cache_read
        self.output += other.output
        self.usd += other.usd
        self.unknown_models |= other.unknown_models


def slice_costs(root: Path, transcripts: Path) -> dict[str, Cost]:
    windows = session_windows(root)
    costs: dict[str, Cost] = {}
    for message in read_messages(transcripts):
        costs.setdefault(attribute(message, windows), Cost()).add(message)
    return costs


def usd_text(cost: Cost) -> str:
    return f"{cost.usd:,.2f}" + (" ?" if cost.unknown_models else "")


def cost_command(args: argparse.Namespace) -> int:
    costs = slice_costs(args.root, args.transcripts)
    ordered = sorted(costs.items(), key=lambda item: -item[1].usd)
    if args.json:
        rows = [
            {
                "slice": name,
                "messages": cost.messages,
                "input": cost.input,
                "cache_write": cost.cache_write,
                "cache_read": cost.cache_read,
                "output": cost.output,
                "usd": round(cost.usd, 6),
                "usd_complete": not cost.unknown_models,
            }
            for name, cost in ordered
        ]
        unknown = sorted(set().union(*(cost.unknown_models for cost in costs.values())))
        json.dump({"slices": rows, "unknown_models": unknown}, sys.stdout, indent=1)
        print()
        return 0
    total = Cost()
    print("| Slice | Messages | Input | Cache write | Cache read | Output | USD |")
    print("|---|---:|---:|---:|---:|---:|---:|")
    for name, cost in ordered:
        print(
            f"| {name} | {cost.messages} | {cost.input:,} | {cost.cache_write:,} | {cost.cache_read:,} "
            f"| {cost.output:,} | {usd_text(cost)} |"
        )
        total.merge(cost)
    print(
        f"| **total** | {total.messages} | {total.input:,} | {total.cache_write:,} | {total.cache_read:,} "
        f"| {total.output:,} | {usd_text(total)} |"
    )
    print(
        "\nUSD is an estimate at first-party API list prices (D-230), not an invoice. "
        "`?` = includes models without a price: " + (", ".join(sorted(total.unknown_models)) or "none") + "."
    )
    return 0


# --- sensors ------------------------------------------------------------------------------------------------------

SENSOR_LINE = re.compile(
    r"^- (\d{4}-\d{2}-\d{2} \d{2}:\d{2}) \| (\S+) \| attempt (\d+)/3 \| (FAIL|PASS) \| (.*?) \| (.*)$"
)


def sensor_lines(root: Path) -> list[tuple[str, re.Match[str]]]:
    return journal_matches(root, SENSOR_LINE)


def sensors_command(args: argparse.Namespace) -> int:
    failures: Counter[str] = Counter()
    runs: Counter[str] = Counter()
    journal: Counter[str] = Counter()
    for event in read_events(args.root):
        runs.update(event.get("tasks") or [])
        if event.get("result") == "FAIL":
            failures.update(failed_tasks(event))
    journal.update(match.group(2) for _, match in sensor_lines(args.root) if match.group(4) == "FAIL")
    print("| Sensor | Failures (hook log) | Runs (hook log) | FAIL lines (journals) |")
    print("|---|---:|---:|---:|")
    for sensor in sorted(failures | runs | journal, key=lambda name: (-failures[name] - journal[name], name)):
        print(f"| {sensor} | {failures[sensor]} | {runs[sensor]} | {journal[sensor]} |")
    print(
        "\nHook log: `.claude/metrics/events.jsonl` (every `./gradlew` run through Bash/PowerShell, D-215). Journals: "
        "pipe sensor lines of `docs/slices/*.md` (D-217); journals written before SOL-147 are free text and not counted."
    )
    return 0


# --- rules --------------------------------------------------------------------------------------------------------

LESSON_HEADER = re.compile(r"^### (L-\d+) ")
VIOLATED_RULE = re.compile(r"^- Violated rule:\s*(.+)$")


def rules_command(args: argparse.Namespace) -> int:
    text = (args.root / "docs" / "LESSONS.md").read_text(encoding="utf-8")
    entries = text.split("\n## Entries", 1)[1] if "\n## Entries" in text else ""
    rules: dict[str, list[str]] = {}
    without = 0
    lesson = ""
    for line in entries.splitlines():
        header = LESSON_HEADER.match(line)
        if header:
            lesson = header.group(1)
            continue
        match = VIOLATED_RULE.match(line)
        if not match:
            continue
        values = [value.strip() for value in match.group(1).split("; ") if value.strip()]
        if values == ["none"]:
            without += 1
        for value in values:
            if value != "none":
                rules.setdefault(value, []).append(lesson)
    print("| Violated rule | Count | Lessons |")
    print("|---|---:|---|")
    for rule, lessons in sorted(rules.items(), key=lambda item: (-len(item[1]), item[0])):
        print(f"| {rule} | {len(lessons)} | {', '.join(lessons)} |")
    print(f"\nLessons without a violated rule (none: {without}). Source: `docs/LESSONS.md` (D-218, D-220).")
    return 0


# --- trace --------------------------------------------------------------------------------------------------------

DECISION_REF = re.compile(r"D-(\d+)(?:\.\.D-(\d+))?")


def decision_refs(text: str) -> list[str]:
    numbers: set[int] = set()
    for first, last in DECISION_REF.findall(text):
        numbers.update(range(int(first), int(last or first) + 1))
    return [f"D-{number}" for number in sorted(numbers)]


def trace_command(args: argparse.Namespace) -> int:
    slice_id = args.slice
    path = slice_files(args.root).get(slice_id)
    if path is None:
        print(f"no slice file docs/slices/{slice_id}-*.md", file=sys.stderr)
        return 1
    text = path.read_text(encoding="utf-8")
    linear = re.search(r"^Linear:\s*(\S+)", text, re.MULTILINE)
    print(f"# Trace {slice_id}\n")
    print(f"- Slice file: `{path.relative_to(args.root).as_posix()}` (status: {slice_status(path) or '?'})")
    print(f"- Linear: {linear.group(1) if linear else '-'}")
    print(f"- Decisions: {', '.join(decision_refs(text)) or '-'}")
    sessions = sorted({m.group(2) for m in map(SESSION_LINE.match, text.splitlines()) if m})
    print(f"- Sessions (journal): {', '.join(sessions) or '-'}")

    print("\n## Sensor events (journal)\n")
    events = [match for owner, match in sensor_lines(args.root) if owner == slice_id]
    for match in events:
        time, sensor, attempt, result, ref, note = match.groups()
        print(f"- {time} {sensor} attempt {attempt}/3 {result} ({ref}): {note}")
    if not events:
        print("- none")

    print("\n## Gradle runs (hook log, slice branch)\n")
    runs = [
        event
        for event in read_events(args.root)
        if (match := SLICE_BRANCH.match(str(event.get("branch", "")))) and match.group(1) == slice_id
    ]
    for event in runs:
        failed = f", failed: {', '.join(failed_tasks(event))}" if event.get("result") == "FAIL" else ""
        print(f"- {event.get('time')} {' '.join(event.get('tasks') or [])} -> {event.get('result')}{failed}")
    if not runs:
        print("- none")

    print("\n## Commits (`git log --all --grep`)\n")
    log = subprocess.run(
        ["git", "-C", str(args.root), "log", "--all", "--format=%h %ad %s", "--date=short", f"--grep={slice_id}\\b"],
        capture_output=True,
        text=True,
        encoding="utf-8",
    )
    commits = log.stdout.strip().splitlines() if log.returncode == 0 else []
    print("\n".join(f"- {commit}" for commit in commits) or "- none")

    cost = slice_costs(args.root, args.transcripts).get(slice_id, Cost())
    print(
        f"\n## Cost\n\n- {cost.messages} messages, {cost.input + cost.cache_write + cost.cache_read + cost.output:,} "
        f"tokens, USD {usd_text(cost)} (API-price estimate, D-230)"
    )
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--root", type=Path, default=REPO_ROOT, help="repository root")
    parser.add_argument("--transcripts", type=Path, help="Claude Code transcript directory of the project")
    commands = parser.add_subparsers(dest="command", required=True)
    cost = commands.add_parser("cost", help="tokens and USD per slice")
    cost.add_argument("--json", action="store_true", help="machine-readable output")
    cost.set_defaults(handler=cost_command)
    commands.add_parser("sensors", help="failures per sensor").set_defaults(handler=sensors_command)
    commands.add_parser("rules", help="violated rules over the lessons").set_defaults(handler=rules_command)
    trace = commands.add_parser("trace", help="one slice end to end")
    trace.add_argument("slice", help="slice id, e.g. SOL-81")
    trace.set_defaults(handler=trace_command)
    args = parser.parse_args(argv)
    if args.transcripts is None:
        args.transcripts = default_transcripts(args.root)
    sys.stdout.reconfigure(encoding="utf-8")
    return args.handler(args)


if __name__ == "__main__":
    sys.exit(main())
