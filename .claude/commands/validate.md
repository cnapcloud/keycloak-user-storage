---
description: Phase 5 — harness 실행, 리포트 파싱, 07-validation-report.md + 07a-traceability.md에 verdict 하나 산출.
argument-hint: "[feature-id]"
agent: validator
---
# /validate

**Phase 5.** 실행: `.claude/agents/validator.md` (validate hat).

## Purpose
harness를 돌리고 결과를 파싱해 `07-validation-report.md`에 PASS / WARN / FAIL 하나를 남긴다. `/review`의 입력.
절차 · 거부조건 · 완료조건 · 참조 skill은 owning agent에 있다.

## Inputs
- `<feature-id>` (선택; 생략 시 owning agent가 `origin/main` 이후 전체 변경으로 해석)

## Outputs
- `.specs/<feature-id>/07-validation-report.md` — verdict(PASS / WARN / FAIL) + 게이트 표 + baseline 델타 + `Gap-NNN` 목록 + 다음 권장 조치
- `.specs/<feature-id>/07a-traceability.md` — AC ↔ Task ↔ Test 매트릭스, 테스트 0개 AC = `MISSING`
- `build/harness-summary.json`, `build/reports/**` — harness 부산물 (리포트가 링크)
- 코드/테스트/`.tdd-state.json` 수정 없음, `git commit` 없음. FAIL 복구는 `/plan` gap re-plan → `/build`
