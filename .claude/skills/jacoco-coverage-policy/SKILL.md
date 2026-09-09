---
name: jacoco-coverage-policy
description: Coverage thresholds and the brownfield ratchet for keycloak-user-storage — 95% on changed lines now, overall line/branch ratcheted up from baseline. Use in /plan gates and /validate.
---
# JaCoCo coverage policy

## Targets
| Scope | Target |
|---|---|
| Changed `src/main` lines (vs `origin/main`) | **≥ 95%** — enforced now by `check-new-code-coverage.sh` |
| Overall line | `_baseline.json` value, ratcheted up; absolute target 90% |
| Overall branch | `_baseline.json` value, ratcheted up; absolute target 90% |

## Brownfield ratchet
- `/onboard` records the current overall line/branch in `.specs/_baseline.json`.
- A feature may not **lower** overall coverage — `/validate` FAILs on any regression vs baseline.
- Overall below the absolute target but not regressed → WARN, not FAIL.
- New code is held to 95% regardless of where the baseline sits.

## Enabling the XML report
`jacocoTestReport` currently emits HTML only. `check-new-code-coverage.sh` and `harness.sh` need XML:
```groovy
jacocoTestReport {
    reports { xml.required = true }
}
```
This one-liner is part of `.claude/docs/harness-gradle.md`; until it lands, the coverage gate reads `skipped`.

## Waivers
A file genuinely not unit-testable (e.g. a `main` bootstrap) is excluded in `build.gradle` `jacocoTestReport { classDirectories ... }` **with an ADR** — never by lowering the global threshold.
