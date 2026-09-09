#!/usr/bin/env bash
# enforce-files-in-scope.sh
# Claude Code PreToolUse hook for Edit|Write.
#
# When a TDD task is active (.specs/<id>/.tdd-state.json has active_task in a
# TDD phase), any edit under src/** must target a path listed in that task's
# files_in_scope. Edits outside src/** (docs, .specs/**, .claude/**, build
# config) are always allowed.
#
# Input : Claude Code hook JSON on stdin.
# Output: exit 0 to allow; exit 2 + stderr to block.

set -euo pipefail

command -v jq >/dev/null 2>&1 || { echo "enforce-files-in-scope: jq not found on PATH; install jq" >&2; exit 2; }

input="$(cat)"
file_path="$(echo "$input" | jq -r '.tool_input.file_path // .tool_input.path // empty')"
[ -z "$file_path" ] && exit 0

case "$file_path" in
  */src/*|src/*) ;;
  *) exit 0 ;;
esac

state_file="$(ls -t .specs/*/.tdd-state.json 2>/dev/null | head -n 1 || true)"
[ -z "$state_file" ] && exit 0
[ -f "$state_file" ] || exit 0

active_task="$(jq -r '.active_task // empty' "$state_file")"
[ -z "$active_task" ] && exit 0

phase="$(jq -r --arg t "$active_task" '.tasks[$t].phase // empty' "$state_file")"
case "$phase" in
  red|green|refactor|simplify) ;;
  *) exit 0 ;;
esac

in_scope="$(jq -r --arg t "$active_task" --arg f "$file_path" '
  .tasks[$t].files_in_scope // []
  | map(select(. == $f or ($f | endswith(.)) or (. | endswith($f))))
  | length
' "$state_file")"

if [ "$in_scope" = "0" ]; then
  echo "BLOCKED: $file_path is outside task $active_task files_in_scope. Declared scope:" >&2
  jq -r --arg t "$active_task" '.tasks[$t].files_in_scope[]? | "  - " + .' "$state_file" >&2
  echo "Update 04-tasks.md and .tdd-state.json first if the scope really needs to grow." >&2
  exit 2
fi

exit 0
