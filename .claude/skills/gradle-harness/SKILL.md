---
name: gradle-harness
description: How .claude/scripts/harness.sh maps the spec-driven "10-layer harness" onto Gradle tasks for this repo, and which layers are active vs scaffolded. Use in /onboard and /validate.
---
# Gradle harness

`.claude/scripts/harness.sh` is this repo's self-validation harness. It runs every layer whose plugin is wired in `build.gradle`, parses the reports, and writes `build/harness-summary.json`. A layer with no plugin emits `{"status":"skipped"}` — skipped is **not** a failure.

## Layer → Gradle task → report
| Layer | Task | Report parsed | Status now |
|---|---|---|---|
| unit | `test` | `build/test-results/test/TEST-*.xml` | **active** |
| coverage | `jacocoTestReport` | `build/reports/jacoco/test/jacocoTestReport.xml` | active once `reports.xml.required=true` (see harness-gradle.md) |
| archunit | `test` (rules run in JUnit) | same as unit | scaffolded |
| checkstyle | `checkstyleMain` | `build/reports/checkstyle/*.xml` | scaffolded |
| spotbugs | `spotbugsMain` | `build/reports/spotbugs/*.xml` | scaffolded |
| mutation | `pitest` | `build/reports/pitest/mutations.xml` | scaffolded |
| openapi | `generateOpenApiDocs` | generated `openapi.yaml` diff | scaffolded |
| owasp | `dependencyCheckAnalyze` | `build/reports/dependency-check-report.json` | scaffolded (offline NVD risk) |

## Modes
```bash
.claude/scripts/harness.sh              # human summary; exit 1 if any active gate fails
.claude/scripts/harness.sh --report     # print build/harness-summary.json
.claude/scripts/harness.sh --baseline   # write .specs/_baseline.json (brownfield onboarding)
```

## Brownfield ratchet
`/validate` compares each gate against `.specs/_baseline.json`:
- worse than baseline → FAIL finding
- no worse than baseline, still below absolute target → WARN + one-line rationale
- new code → must meet the absolute target (`check-new-code-coverage.sh`, ≥95%) regardless of baseline

## Wiring the scaffolded layers
Do **not** hand-edit `build.gradle` ad hoc. `.claude/docs/harness-gradle.md` holds the full patch; wire it through its own `.specs/<date>-wire-gradle-harness/` feature so the change itself goes through the workflow. Internal repo `reposilite.kind.internal` may not mirror every plugin — verify resolution first.
