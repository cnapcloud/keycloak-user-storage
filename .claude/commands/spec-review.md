---
description: Phase 2 — 01-spec.md를 체크리스트에 대조, PASS/FAIL verdict로 02-spec-review.md 산출.
argument-hint: "[feature-id]"
agent: spec-author
---
# /spec-review

**Phase 2.** 실행: `.claude/agents/spec-author.md` (review hat).

## Purpose
`01-spec.md`를 `.claude/checklists/spec-review.md`에 대조해 `02-spec-review.md`를 만든다. `PASS` / `FAIL` verdict 하나 + 필요한 편집의 번호 목록.
절차 · 거부조건 · 완료조건 · 참조 skill은 owning agent에 있다.

## Inputs
- `<feature-id>` (선택; 생략 시 가장 최근 수정된 `.specs/<id>/`)
