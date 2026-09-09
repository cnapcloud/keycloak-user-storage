# Validation Report: <FEATURE-ID>

> Owner: `validator` · Phase 5 · Template: `.claude/templates/validation-report.template.md`
> Source: `build/harness-summary.json` (git <sha>, run <timestamp>)

## Verdict
**<PASS | WARN | FAIL>** — <one-line rationale>

## Gate table
| Gate | Status | Value | Baseline | Note |
|------|--------|-------|----------|------|
| unit | pass/fail | <n> tests, <n> failures | — | |
| coverage (overall) | pass/warn/fail/skipped | line <x%>, branch <y%> | line <b%> | ratchet |
| coverage (changed lines) | pass/fail/skipped | <c/t> (<z%>) | — | target 95% |
| archunit | pass/fail/skipped | <n> violations (<frozen>) | <frozen> | |
| checkstyle | pass/fail/skipped | <n> | <b> | |
| spotbugs | pass/fail/skipped | <n> | <b> | |
| mutation | pass/warn/fail/skipped | kill <x%>, survived <n> | — | changed pkgs |

## Baseline delta
- <metric>: <baseline> → <now> (<better/worse/same>)

## Top failing items
1. <file:line> — <what failed> — <smallest fix: /build T-NNN or a gap test>

## Traceability
See `07a-traceability.md`. Uncovered ACs: <none | list>.

## Waivers
- <gate> waived — see `adr/ADR-NNN-*.md` — rationale: <…>

## Recommended next action
`<​/review | /build T-NNN>`
