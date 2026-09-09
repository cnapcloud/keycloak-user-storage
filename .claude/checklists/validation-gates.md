# Checklist: Validation Gates (Phase 5)

`validator` applies this when aggregating `build/harness-summary.json` into a verdict.

## Preconditions
- [ ] Every task in `04-tasks.md` is `done`.
- [ ] The harness was actually re-run this session (no stale `build/harness-summary.json`).

## Per-gate (for each of: unit, coverage, archunit, checkstyle, spotbugs, mutation)
- [ ] `pass` → ok.
- [ ] `fail` → FAIL verdict; list the smallest `/build` fix.
- [ ] `skipped` → plugin not wired; report as "not enforced yet", not pass.
- [ ] Report **missing** for a layer `_stack.json` marks active → error, not pass.
- [ ] Value compared against `.specs/_baseline.json` — any regression → FAIL finding.
- [ ] Value below absolute target but not regressed → WARN + one-line rationale.

## New code
- [ ] `check-new-code-coverage.sh` ≥ 95% on changed `src/main` lines (or `skipped` only while jacoco XML is off).
- [ ] Zero `SURVIVED` mutants in changed packages, or each has an "equivalent because …" line.

## Traceability
- [ ] `07a-traceability.md` regenerated.
- [ ] Zero ACs with no test.
- [ ] Zero orphan tests / orphan code (or each orphan is a `Q-NNN`).

## Waivers
- [ ] Every waiver points at an `adr/ADR-NNN-*.md`.

## Output
- [ ] `07-validation-report.md` has a single verdict (PASS / WARN / FAIL) + rationale + recommended next action.
