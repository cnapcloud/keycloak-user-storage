#!/usr/bin/env bash
# block-impl-without-failing-test.sh
# Claude Code PreToolUse hook for Edit|Write.
#
# Refuses to edit src/main/** unless the active feature's
# .specs/<id>/.tdd-state.json shows the current task in a TDD phase
# (red|green|refactor|simplify), with a non-empty red_failure_excerpt when
# phase == red, and the target file listed in that task's files_in_scope.
#
# Input : Claude Code hook JSON on stdin (tool_input.file_path / .path).
# Output: exit 0 to allow; exit 2 + stderr message to block.
#
# Adapted from loiane/specs-driven-development-spring-angular (Gradle port:
# paths are src/main/java, no Maven assumptions here).

set -euo pipefail

command -v jq >/dev/null 2>&1 || { echo "block-impl-without-failing-test: jq not found on PATH; install jq" >&2; exit 2; }

input="$(cat)"
file_path="$(echo "$input" | jq -r '.tool_input.file_path // .tool_input.path // empty')"
[ -z "$file_path" ] && exit 0

# Only enforce on production sources.
case "$file_path" in
  */src/main/*|src/main/*) ;;
  *) exit 0 ;;
esac

# Active feature = most recently modified .specs/<id>/.tdd-state.json
state_file="$(ls -t .specs/*/.tdd-state.json 2>/dev/null | head -n 1 || true)"
if [ -z "$state_file" ] || [ ! -f "$state_file" ]; then
  echo "BLOCKED: no .specs/<feature>/.tdd-state.json found. Run /plan to create it, then /build <task-id> so the failing test is written first." >&2
  exit 2
fi

active_task="$(jq -r '.active_task // empty' "$state_file")"
if [ -z "$active_task" ]; then
  echo "BLOCKED: no active_task in $state_file. Run /build <task-id> before touching src/main/**." >&2
  exit 2
fi

phase="$(jq -r --arg t "$active_task" '.tasks[$t].phase // empty' "$state_file")"
red_excerpt="$(jq -r --arg t "$active_task" '.tasks[$t].red_failure_excerpt // empty' "$state_file")"

case "$phase" in
  red|green|refactor|simplify) ;;
  *)
    echo "BLOCKED: task $active_task phase is '$phase'. Cannot edit src/main/** without a failing test (phase=red)." >&2
    exit 2 ;;
esac

if [ "$phase" = "red" ] && [ -z "$red_excerpt" ]; then
  echo "BLOCKED: task $active_task phase=red but red_failure_excerpt is empty. The failing test was not run, or it passed. Write and run the failing test first." >&2
  exit 2
fi

in_scope="$(jq -r --arg t "$active_task" --arg f "$file_path" '
  .tasks[$t].files_in_scope // []
  | map(select(. == $f or ($f | endswith(.)) or (. | endswith($f))))
  | length
' "$state_file")"

if [ "$in_scope" = "0" ]; then
  echo "BLOCKED: $file_path is not in task $active_task files_in_scope. Edit only declared paths, or update 04-tasks.md + .tdd-state.json and re-run /plan." >&2
  exit 2
fi

exit 0
