"""PostToolUse hook: format an edited .java file with Spotless.

No-op until Spotless is configured in build.gradle (phase 0).
On failure, exit code 2 feeds stderr back to Claude as a sensor signal.
"""

import json
import os
import subprocess
import sys


def main() -> int:
    payload = json.load(sys.stdin)
    tool_input = payload.get("tool_input", {})
    file_path = tool_input.get("file_path") or payload.get("tool_response", {}).get("filePath")
    if not file_path or not file_path.endswith(".java"):
        return 0

    project_dir = os.environ.get("CLAUDE_PROJECT_DIR") or payload.get("cwd") or os.getcwd()
    build_file = os.path.join(project_dir, "build.gradle")
    try:
        with open(build_file, encoding="utf-8") as f:
            if "spotless" not in f.read():
                return 0
    except OSError:
        return 0

    gradlew = os.path.join(project_dir, "gradlew.bat" if os.name == "nt" else "gradlew")
    result = subprocess.run(
        [gradlew, "spotlessApply", f"-PspotlessIdeHook={os.path.abspath(file_path)}", "--quiet"],
        cwd=project_dir,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        sys.stderr.write(f"spotlessApply failed for {file_path}:\n{result.stdout}\n{result.stderr}")
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
