#!/usr/bin/env bash
# check-new-code-coverage.sh
# Enforce >= 95% line coverage on lines added/changed vs a base ref.
# Needs build/reports/jacoco/test/jacocoTestReport.xml (run harness.sh first).
#
# Usage: .claude/scripts/check-new-code-coverage.sh [base-ref]   (default origin/main)
# Exit : 0 pass or skipped (no jacoco xml / no changed main lines); 1 below threshold.

set -euo pipefail

BASE="${1:-origin/main}"
JACOCO="build/reports/jacoco/test/jacocoTestReport.xml"
THRESHOLD="0.95"

if [ ! -f "$JACOCO" ]; then
  echo "new-code-coverage: $JACOCO not found — skipped (enable jacoco reports.xml and run harness.sh)"
  exit 0
fi
git rev-parse --verify "$BASE" >/dev/null 2>&1 || { echo "new-code-coverage: base ref '$BASE' not found — skipped"; exit 0; }

python3 - "$BASE" "$JACOCO" "$THRESHOLD" <<'PY'
import subprocess, sys, re, xml.etree.ElementTree as ET

base, jacoco, threshold = sys.argv[1], sys.argv[2], float(sys.argv[3])

diff = subprocess.run(
    ["git", "diff", "--unified=0", f"{base}...HEAD", "--", "src/main/**"],
    capture_output=True, text=True).stdout

changed = {}          # basename -> set(line numbers added on the new side)
cur = None
for line in diff.splitlines():
    if line.startswith("+++ b/"):
        cur = line[6:].split("/")[-1]
        changed.setdefault(cur, set())
    elif line.startswith("@@") and cur:
        m = re.search(r"\+(\d+)(?:,(\d+))?", line)
        if m:
            start = int(m.group(1)); count = int(m.group(2) or 1)
            for n in range(start, start + count):
                changed[cur].add(n)

if not any(changed.values()):
    print("new-code-coverage: no changed src/main lines — skipped")
    sys.exit(0)

root = ET.parse(jacoco).getroot()
covered = missed = 0
for sf in root.iter("sourcefile"):
    name = sf.get("name")
    if name not in changed:
        continue
    for ln in sf.iter("line"):
        n = int(ln.get("nr"))
        if n not in changed[name]:
            continue
        ci = int(ln.get("ci", 0))   # covered instructions
        if ci > 0: covered += 1
        else:      missed += 1

total = covered + missed
ratio = covered / total if total else 1.0
print(f"new-code-coverage: {covered}/{total} changed lines covered ({ratio:.1%}), threshold {threshold:.0%}")
sys.exit(0 if ratio >= threshold else 1)
PY
