---
name: harness-report-parsing
description: Read build/harness-summary.json and the underlying Gradle reports correctly — never guess a gate result, never treat a missing report for an active layer as pass. Use in /validate.
---
# Harness report parsing

`/validate` reads structured reports — it never eyeballs console output.

## Order of truth
1. `build/harness-summary.json` — the aggregated verdict from `harness.sh`.
2. The raw reports it summarised:
   - `build/test-results/test/TEST-*.xml` — JUnit counts (`tests`, `failures`, `errors`, `skipped`).
   - `build/reports/jacoco/test/jacocoTestReport.xml` — last `<counter type="LINE">` / `"BRANCH">` = report total.
   - `build/reports/checkstyle/*.xml` — count `<error ` elements.
   - `build/reports/spotbugs/*.xml` — `total_bugs`.
   - `build/reports/pitest/mutations.xml` — `<mutation status="KILLED|SURVIVED|TIMED_OUT|NO_COVERAGE">`; kill rate = (KILLED+TIMED_OUT)/total.
3. `.claude/scripts/check-new-code-coverage.sh` output for changed-line coverage.

## Rules
- `status: "skipped"` = the plugin isn't wired. Report it as "not enforced yet", never as pass.
- A **missing** report for a layer `_stack.json` marks active = **error**, not pass.
- A test `skipped` with no `# DisabledReason` on the line above = error.
- A `SURVIVED` mutant in changed code needs either a new test or a one-line "equivalent because …" rationale in `07-validation-report.md`.
- Quote real numbers in the report (`142/150 lines, 94.7%`), not "looks fine".

## Verdict mapping
| Condition | Verdict |
|---|---|
| every active gate `pass`, ratchet not regressed | PASS |
| active gates pass but below absolute target, no baseline regression, waivers have ADRs | WARN |
| any active gate `fail`, or a ratchet regression, or an uncovered AC | FAIL |
