---
description: Phase 5 — harness 실행, 리포트 파싱, 07-validation-report.md + 07a-traceability.md에 verdict 하나 산출.
argument-hint: "[feature-id]"
agent: validator
hat: validate
---
# /validate — Phase 5

## Purpose
`.claude/scripts/harness.sh`를 돌려 모든 게이트 리포트를 파싱하고, AC ↔ 태스크 ↔ 테스트
traceability 매트릭스를 만들고, 결과를 `PASS` / `WARN` / `FAIL` 하나로 판정한다.
테스트나 코드는 건드리지 않고, 커버리지 갭은 `Gap-NNN`으로 나열만 한다.

## Inputs
- `[feature-id]` — 생략하면 가장 최근 수정된 `.specs/<id>/` (traceability에 필수라 모호하면 사용자에게 확인)

## Outputs
- `.specs/<id>/07-validation-report.md` — verdict + 게이트 표 + baseline 델타 + `Gap-NNN` 목록
- `.specs/<id>/07a-traceability.md` — AC ↔ Task ↔ Test 매트릭스 (테스트 0개 AC = `MISSING`)
- `build/harness-summary.json`, `build/reports/**` — harness 부산물, 리포트가 링크로 참조

## Next
`PASS` · `WARN`이면 `/review`로 넘어간다. `FAIL`이면 `/plan`을 다시 불러 `Gap-NNN`을 덮는
gap task를 `04-tasks.md`에 추가한 뒤 `/build`로 닫는다. 복구는 사람이 트리거하며,
validator 자신은 테스트/코드를 고치지 않는다.

---
이 커맨드는 `validator`를 **validate hat**으로 실행한다. 절차 · 거부조건 · 완료조건 ·
참조 skill은 `.claude/agents/validator.md`에 있다.
