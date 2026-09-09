#!/usr/bin/env bash
# harness.sh
# The agent's self-validation harness (Gradle). Runs every layer that is wired
# in build.gradle, parses the reports, and emits one JSON summary consumed by
# /validate. Layers whose plugin is not present emit {"status":"skipped"}.
#
# Usage:
#   .claude/scripts/harness.sh              # run; print human summary; non-zero on gate failure
#   .claude/scripts/harness.sh --report     # run; print build/harness-summary.json to stdout
#   .claude/scripts/harness.sh --baseline   # run; write .specs/_baseline.json (brownfield onboarding)
#
# Ports loiane/specs-driven-development-spring-angular's 10-layer harness.sh to
# Gradle + this repo (Java 17, Spring Boot 3.3.x, H2, no migrations).

set -euo pipefail

MODE="${1:-run}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

SUMMARY="build/harness-summary.json"
LOG="build/harness.log"
mkdir -p build

GRADLE="./gradlew"
[ -x "$GRADLE" ] || GRADLE="gradle"

section() { echo "=== $* ===" >&2; }   # progress → stderr, so --report/--baseline stdout stays clean JSON
has_task() { $GRADLE -q tasks --all 2>/dev/null | grep -qE "^[[:space:]]*$1( |$)"; }

STACK_JSON="$("$ROOT/.claude/scripts/detect-stack.sh" build.gradle 2>/dev/null || echo '{}')"
layer() { echo "$STACK_JSON" | (command -v jq >/dev/null 2>&1 && jq -r ".harness_layers.$1 // false" || echo false); }

# ---- run the build -----------------------------------------------------------
section "test + coverage report"
set +e
$GRADLE --console=plain test jacocoTestReport > "$LOG" 2>&1
BUILD_RC=$?
set -e

run_optional() {
  local task="$1"
  if has_task "$task"; then
    section "$task"
    $GRADLE --console=plain "$task" >> "$LOG" 2>&1 || true
  fi
}
[ "$(layer checkstyle)" = "true" ] && { run_optional checkstyleMain; run_optional checkstyleTest; }
[ "$(layer spotbugs)"   = "true" ] && run_optional spotbugsMain
[ "$(layer archunit)"   = "true" ] && run_optional archTest
[ "$(layer mutation)"   = "true" ] && run_optional pitest
[ "$(layer openapi)"    = "true" ] && { run_optional generateOpenApiDocs; run_optional forkedSpringBootRun; }
[ "$(layer owasp)"      = "true" ] && run_optional dependencyCheckAnalyze

# ---- parse reports ---------------------------------------------------------
parse_junit() {
  local dir="build/test-results/test"
  [ -d "$dir" ] || { echo '{"status":"skipped"}'; return; }
  python3 - "$dir" <<'PY'
import sys, glob, xml.etree.ElementTree as ET
d = sys.argv[1]; tests=fail=err=skip=0
for f in glob.glob(d + "/TEST-*.xml"):
    try: r = ET.parse(f).getroot()
    except Exception: continue
    tests += int(r.get("tests", 0)); fail += int(r.get("failures", 0))
    err   += int(r.get("errors", 0)); skip += int(r.get("skipped", 0))
status = "fail" if (fail or err) else ("skipped" if tests == 0 else "pass")
print(f'{{"status":"{status}","tests":{tests},"failures":{fail},"errors":{err},"skipped":{skip}}}')
PY
}

parse_jacoco() {
  local f="build/reports/jacoco/test/jacocoTestReport.xml"
  [ -f "$f" ] || { echo '{"status":"skipped","note":"enable reports.xml.required in build.gradle"}'; return; }
  python3 - "$f" <<'PY'
import sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
def ratio(t):
    cs = [c for c in root.iter('counter') if c.get('type') == t]
    if not cs: return 0.0, 0
    m = int(cs[-1].get('missed', 0)); c = int(cs[-1].get('covered', 0))
    tot = m + c
    return (c / tot if tot else 0.0), tot
lr, lt = ratio('LINE'); br, bt = ratio('BRANCH')
status = "fail" if (lt and lr < 0.90) or (bt and br < 0.90) else "pass"
print(f'{{"status":"{status}","line":{lr:.4f},"branch":{br:.4f}}}')
PY
}

parse_checkstyle() {
  local n; n=$(cat build/reports/checkstyle/*.xml 2>/dev/null | grep -c '<error ' || true)
  [ -z "$n" ] && { echo '{"status":"skipped"}'; return; }
  ls build/reports/checkstyle/*.xml >/dev/null 2>&1 || { echo '{"status":"skipped"}'; return; }
  [ "$n" -eq 0 ] && echo '{"status":"pass","violations":0}' || echo "{\"status\":\"fail\",\"violations\":$n}"
}

parse_spotbugs() {
  ls build/reports/spotbugs/*.xml >/dev/null 2>&1 || { echo '{"status":"skipped"}'; return; }
  local n; n=$(grep -ho 'total_bugs="[0-9]*"' build/reports/spotbugs/*.xml 2>/dev/null | grep -o '[0-9]*' | paste -sd+ - | bc 2>/dev/null || echo 0)
  [ "${n:-0}" -eq 0 ] && echo '{"status":"pass","bugs":0}' || echo "{\"status\":\"fail\",\"bugs\":$n}"
}

parse_pit() {
  local f="build/reports/pitest/mutations.xml"
  [ -f "$f" ] || { echo '{"status":"skipped"}'; return; }
  python3 - "$f" <<'PY'
import sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
total=killed=survived=0
for m in root.iter('mutation'):
    total += 1; s = m.get('status', '')
    if s in ('KILLED', 'TIMED_OUT'): killed += 1
    elif s == 'SURVIVED': survived += 1
kr = killed / total if total else 0.0
status = "pass" if kr >= 0.75 else "fail"
print(f'{{"status":"{status}","kill_rate":{kr:.4f},"survived":{survived},"total":{total}}}')
PY
}

unit=$(parse_junit)
cov=$(parse_jacoco)
chk=$(parse_checkstyle)
sb=$(parse_spotbugs)
pit=$(parse_pit)
arch='{"status":"skipped"}'
[ "$(layer archunit)" = "true" ] && arch="$unit"   # ArchUnit rules run inside the JUnit suite

cat > "$SUMMARY" <<EOF
{
  "started_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "git_sha": "$(git rev-parse --short HEAD 2>/dev/null || echo unknown)",
  "build_exit": $BUILD_RC,
  "stack": $STACK_JSON,
  "gates": {
    "unit":       $unit,
    "coverage":   $cov,
    "archunit":   $arch,
    "checkstyle": $chk,
    "spotbugs":   $sb,
    "mutation":   $pit
  }
}
EOF

# ---- output ---------------------------------------------------------------
FAILED=0
for g in unit coverage archunit checkstyle spotbugs mutation; do
  st=$( (command -v jq >/dev/null 2>&1 && jq -r ".gates.$g.status" "$SUMMARY") || echo unknown)
  [ "$st" = "fail" ] && FAILED=1
done

case "$MODE" in
  --report)
    cat "$SUMMARY" ;;
  --baseline)
    mkdir -p .specs
    if command -v jq >/dev/null 2>&1; then
      jq '{captured_at: .started_at, git_sha, stack, gates}' "$SUMMARY" > .specs/_baseline.json
    else
      cp "$SUMMARY" .specs/_baseline.json
    fi
    echo "Wrote .specs/_baseline.json" ;;
  *)
    echo
    (command -v jq >/dev/null 2>&1 && jq '.gates' "$SUMMARY") || cat "$SUMMARY"
    echo
    [ "$BUILD_RC" -ne 0 ] && echo "build/test exited $BUILD_RC — see $LOG"
    [ "$FAILED" -eq 1 ] && { echo "HARNESS: FAIL"; exit 1; }
    echo "HARNESS: PASS (skipped layers are not failures)" ;;
esac
