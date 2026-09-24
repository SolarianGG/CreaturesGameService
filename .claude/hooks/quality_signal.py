"""Stop hook: failure signal for the user when the agent seems to be stuck (D-223, D-224, D-227, D-229).

Triggers checked at the end of every turn:
  2  a Gradle task has been failing for more than 60 minutes without a green run of it (also when no tool ran since)
  3  the USD estimate of the active slice exceeds 2x the median of the closed slices (scripts/harness/metrics.py)
  4  the active slice journal has a sensor line `attempt 3/3 | FAIL` (escalation, docs/HARNESS.md §4)
Each trigger fires once per episode (state in `.claude/metrics/signal_state.json`). The output never blocks
(`decision: block` / exit 2); its `additionalContext` gives the agent one continuation to record the blocker in
docs/STATE.md and ask the user. Messages queued by `sensor_events.py` after a tool call are shown to the user here,
because a PostToolUse systemMessage is only visible in the expanded view (D-231). Any error fails open.
"""

import json
import re
import statistics
import sys
from datetime import datetime, timezone
from pathlib import Path

from sensor_events import (
    agent_output,
    branch,
    failure_triggers,
    load_state,
    new_signals,
    project_dir,
    read_events,
    save_state,
    user_output,
)

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "scripts" / "harness"))
import metrics  # noqa: E402

COST_FACTOR = 2.0
ACTIVE_SLICE = re.compile(r"Active slice:\s*`?docs/slices/((SOL-\d+)-[^\s`]*\.md)")


def active_slice(root: Path) -> str | None:
    """Id of the active slice in STATE.md, if its file exists."""
    try:
        match = ACTIVE_SLICE.search((root / "docs" / "STATE.md").read_text(encoding="utf-8"))
    except OSError:
        return None
    return match.group(2) if match and (root / "docs" / "slices" / match.group(1)).exists() else None


def escalations(root: Path, slice_id: str) -> dict[str, str]:
    active = {}
    for owner, match in metrics.sensor_lines(root):
        time, sensor, attempt, result = match.group(1, 2, 3, 4)
        if owner == slice_id and attempt == "3" and result == "FAIL":
            active[f"t4|{slice_id}|{time}|{sensor}"] = (
                f"{sensor} reached attempt 3/3 FAIL in {slice_id} ({time}) — escalate to the user"
            )
    return active


def cost_trigger(root: Path, transcripts: Path, slice_id: str) -> dict[str, str]:
    costs = metrics.slice_costs(root, transcripts)
    files = metrics.slice_files(root)
    closed = [
        cost.usd
        for name, cost in costs.items()
        if name in files and name != slice_id and metrics.slice_status(files[name]) == "done"
    ]
    current = costs.get(slice_id)
    if not closed or current is None:
        return {}
    median = statistics.median(closed)
    threshold = COST_FACTOR * median
    if current.usd <= threshold:
        return {}
    return {
        f"t3|{slice_id}": (
            f"{slice_id} has cost ${current.usd:,.2f} so far, above {COST_FACTOR:g}x the median of the closed "
            f"slices (median ${median:,.2f}, threshold ${threshold:,.2f})"
        )
    }


def main() -> int:
    try:
        payload = json.load(sys.stdin)
    except ValueError:
        return 0
    if not isinstance(payload, dict):
        return 0
    root = project_dir()
    branch_name = branch(root)
    state = load_state(root)

    stuck = failure_triggers(read_events(root), branch_name, datetime.now(timezone.utc), kinds=("t2",))
    messages = new_signals(state, stuck, (f"t2|{branch_name}|",))

    slice_id = active_slice(root)
    if slice_id is not None:
        transcript = payload.get("transcript_path")
        # the transcripts are the expensive part: skip them while this slice's cost episode is open
        if transcript and f"t3|{slice_id}" not in state.get("fired", []):
            messages += new_signals(state, cost_trigger(root, Path(transcript).parent, slice_id), ("t3|",))
        messages += new_signals(state, escalations(root, slice_id), ("t4|",))

    queued = state.pop("pending_user", [])
    save_state(root, state)
    if messages:
        # new at Stop: the user sees everything, the agent gets one continuation (D-229)
        json.dump({**user_output(queued + messages), **agent_output("Stop", messages)}, sys.stdout)
    elif queued:
        # raised after a tool call: the agent already has it, only the user is told (D-231)
        json.dump(user_output(queued), sys.stdout)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception:  # fail open: a broken signal must never stop the session
        sys.exit(0)
