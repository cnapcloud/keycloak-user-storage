#!/usr/bin/env bash
# create-feature-branch.sh
# Claude Code PreToolUse hook for Edit|Write.
#
# When spec-author (author hat) is about to Write a brand-new
# .specs/<feature-id>/01-spec.md, create (or switch to) a branch named
# spec/<feature-id> off whatever branch is currently checked out, before the
# file lands. This makes "/spec on branch X" produce a dedicated branch for
# that feature instead of piling spec + implementation onto X.
#
# Only fires on first creation of a spec (file does not exist yet on disk).
# Re-running /spec --continue on an existing 01-spec.md is a no-op here.
# Never blocks the Write — on any git failure this warns on stderr and lets
# the tool call proceed on the current branch.
#
# Input : Claude Code hook JSON on stdin.
# Output: always exit 0 (advisory/automation, not a gate).

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
  */.specs/*/01-spec.md|.specs/*/01-spec.md) ;;
  *) exit 0 ;;
esac

# Only the very first Write of this spec triggers a branch switch.
[ -f "$file_path" ] && exit 0

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || exit 0

feature_dir="$(dirname "$file_path")"
feature_id="$(basename "$feature_dir")"
[ -z "$feature_id" ] && exit 0

branch_name="spec/$feature_id"
current_branch="$(git branch --show-current 2>/dev/null || true)"
[ "$current_branch" = "$branch_name" ] && exit 0

if git rev-parse --verify --quiet "$branch_name" >/dev/null 2>&1; then
  if git checkout "$branch_name" >&2 2>&1; then
    echo "create-feature-branch: switched to existing '$branch_name' (from '$current_branch')." >&2
  else
    echo "create-feature-branch: WARN failed to switch to '$branch_name' — staying on '$current_branch'." >&2
  fi
else
  if git checkout -b "$branch_name" >&2 2>&1; then
    echo "create-feature-branch: created '$branch_name' from '$current_branch' for $feature_id." >&2
  else
    echo "create-feature-branch: WARN failed to create '$branch_name' — staying on '$current_branch'." >&2
  fi
fi

exit 0
