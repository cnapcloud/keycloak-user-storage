---
description: Read-only — show where every feature under .specs/ stands. See .claude/commands/status.md.
argument-hint: "[feature-id]"
---
# /status — meta (read-only)

## Purpose
활성 기능 각각이 파이프라인 어디에 있는지 한 표로 보여준다. 순수 리포팅 —
쓰기도 부작용도 없고 owning agent도 없다.

## Inputs
- `[feature-id]` — 선택. 없으면 `.specs/` 아래 전체를 요약한다.

## Outputs
- 없음 — 표 + 권장 다음 커맨드 한 줄을 출력만 한다

## Reads
- `.specs/*/01-spec.md`, `02-spec-review.md`, `03-design.md`, `04-tasks.md`, `.tdd-state.json`, `07-validation-report.md`
- `build/harness-summary.json` if present

## Process
One row per feature:
- `feature_id`
- `phase` — derived from which artifacts exist + their verdicts (specify → spec-review → plan → build → validate → review → done)
- `acs_total`, `acs_with_tests`
- `tasks_done / tasks_total`
- `last_validate_verdict` + timestamp
- `active_task` from `.tdd-state.json` and its current phase

Then one sentence: "Recommended next action: …".

## Refuse if
Never. Missing data shows `—`.

## Done when
The table is printed with a recommended next command.
