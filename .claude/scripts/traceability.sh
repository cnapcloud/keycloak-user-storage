#!/usr/bin/env bash
# traceability.sh
# Map every AC-NNN in a feature spec to the tests that exercise it.
# A test "covers" an AC if it carries @Tag("AC-NNN") or "AC-NNN" in @DisplayName.
#
# Usage: .claude/scripts/traceability.sh <feature-id>
# Prints a markdown matrix to stdout. Exit 1 if any AC has zero tests.

set -euo pipefail

FID="${1:-}"
[ -n "$FID" ] || { echo "usage: traceability.sh <feature-id>" >&2; exit 2; }

SPEC=".specs/$FID/01-spec.md"
[ -f "$SPEC" ] || { echo "traceability: $SPEC not found" >&2; exit 2; }

ACS="$(grep -oE 'AC-[0-9]{3}' "$SPEC" | sort -u || true)"
[ -n "$ACS" ] || { echo "traceability: no AC-NNN ids in $SPEC" >&2; exit 2; }

echo "# Traceability — $FID"
echo
echo "| AC | Tests | Status |"
echo "|----|-------|--------|"

missing=0
while IFS= read -r ac; do
  [ -z "$ac" ] && continue
  hits="$(grep -rlE "@Tag\\(\"$ac\"\\)|\"$ac[:\" ]" src/test 2>/dev/null | sed 's#src/test/java/##;s#/#.#g;s#\.java##' | paste -sd', ' - || true)"
  if [ -n "$hits" ]; then
    echo "| $ac | $hits | covered |"
  else
    echo "| $ac | — | MISSING |"
    missing=$((missing + 1))
  fi
done <<< "$ACS"

echo
total="$(echo "$ACS" | grep -c . || echo 0)"
echo "_${total} ACs, ${missing} without tests._"
[ "$missing" -eq 0 ] || exit 1
