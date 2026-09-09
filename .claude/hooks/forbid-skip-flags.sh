#!/usr/bin/env bash
# forbid-skip-flags.sh
# Claude Code PreToolUse hook for Bash.
#
# Blocks build invocations that would skip the harness: Gradle task exclusions
# (-x test / -x check / --exclude-task), pitest/jacoco skips, and git/gradle
# --no-verify. Keeps the self-validating harness honest.
#
# Input : Claude Code hook JSON on stdin (tool_input.command).
# Output: exit 0 to allow; exit 2 + stderr to block.

set -euo pipefail

input="$(cat)"
if command -v jq >/dev/null 2>&1; then
  cmd="$(echo "$input" | jq -r '.tool_input.command // empty')"
else
  cmd="$(echo "$input" | sed -n 's/.*"command"[[:space:]]*:[[:space:]]*"\(.*\)"[[:space:]]*}.*/\1/p')"
fi
[ -z "$cmd" ] && exit 0

norm="$(echo "$cmd" | tr '[:upper:]' '[:lower:]')"

deny() {
  echo "BLOCKED: '$1' skips a harness layer. Run the full build (./gradlew check / .claude/scripts/harness.sh)." >&2
  echo "  command: $cmd" >&2
  exit 2
}

case "$norm" in
  *" -x test"*|*" -x check"*|*" -x pitest"*|*" -x jacoco"*|*" -x jacocotestreport"*|*" -x jacocotestcoverageverification"*|*" -x integrationtest"*)
    deny "gradle -x <verification task>" ;;
  *"--exclude-task test"*|*"--exclude-task check"*|*"--exclude-task pitest"*)
    deny "gradle --exclude-task" ;;
  *"-dpitest.skip"*|*"-dpit.skip"*|*"-dskiptests"*|*"-dskip.tests"*|*"-dtest.skip"*|*"-dmaven.test.skip"*)
    deny "test/mutation skip property" ;;
  *"--no-verify"*)
    deny "--no-verify" ;;
esac

exit 0
