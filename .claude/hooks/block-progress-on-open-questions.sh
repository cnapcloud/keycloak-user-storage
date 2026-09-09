#!/usr/bin/env bash
# block-progress-on-open-questions.sh
# Claude Code PreToolUse hook for Edit|Write.
#
# The no-invention gate. While the active feature's 01-spec.md / 03-design.md /
# 04-tasks.md still carries an unresolved Q-NNN, block edits to src/** — the
# spec must be resolved (or the question explicitly deferred-with-rationale)
# before implementation starts.
#
# "Unresolved" = a line matching '- Q-NNN' with 'Status: open' nearby, OR a
# non-placeholder '- Q-NNN' bullet under an '## Open Questions' heading.
# A question is considered handled when its Status line reads 'resolved' or
# 'deferred'.
#
# Input : Claude Code hook JSON on stdin.
# Output: exit 0 to allow; exit 2 + stderr to block.

set -euo pipefail

input="$(cat)"

file_path=""
if command -v jq >/dev/null 2>&1; then
  file_path="$(echo "$input" | jq -r '.tool_input.file_path // .tool_input.path // empty')"
else
  file_path="$(echo "$input" | sed -n 's/.*"file_path"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
fi
[ -z "$file_path" ] && exit 0

case "$file_path" in
  */src/*|src/*) ;;
  *) exit 0 ;;
esac

# Active feature dir = most recently modified .specs/<id>/ containing 01-spec.md
spec_file="$(ls -t .specs/*/01-spec.md 2>/dev/null | head -n 1 || true)"
[ -z "$spec_file" ] && exit 0
feature_dir="$(dirname "$spec_file")"

open_hits=""
for f in "$feature_dir/01-spec.md" "$feature_dir/03-design.md" "$feature_dir/04-tasks.md"; do
  [ -f "$f" ] || continue
  # Lines like "- Q-001:" whose following "Status:" line is "open" (or missing).
  hits="$(awk '
    /^- Q-[0-9]+/ { qline=$0; status="open"; next }
    /^[[:space:]]*-?[[:space:]]*Status:/ {
      s=tolower($0)
      if (s ~ /resolved/ || s ~ /deferred/) status="handled"
      else status="open"
      if (qline != "") { print FILENAME ": " qline "  [" status "]"; qline="" }
    }
    END { if (qline != "") print FILENAME ": " qline "  [open]" }
  ' "$f" | grep -F "[open]" || true)"
  [ -n "$hits" ] && open_hits="$open_hits$hits"$'\n'
done

if [ -n "${open_hits// /}" ] && [ -n "$(echo "$open_hits" | tr -d '[:space:]')" ]; then
  echo "BLOCKED: unresolved Open Questions in $feature_dir — resolve or defer-with-rationale before editing src/**:" >&2
  echo "$open_hits" | sed '/^$/d' | sed 's/^/  /' >&2
  exit 2
fi

exit 0
