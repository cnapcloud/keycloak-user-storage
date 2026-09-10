---
description: Phase 2 — 01-spec.md를 체크리스트에 대조, PASS/FAIL verdict로 02-spec-review.md 산출.
argument-hint: "[feature-id]"
agent: spec-author
hat: review
---
# /spec-review — Phase 2

## Purpose
`01-spec.md`를 `.claude/checklists/spec-review.md`에 한 줄씩 대조해 통과 여부를
`PASS` / `FAIL` 하나로 판정한다. `FAIL`이면 author가 그대로 적용할 수 있는
편집 목록(라인 + 대체문)을 함께 남긴다.

## Inputs
- `[feature-id]` — 생략하면 가장 최근 수정된 `.specs/<id>/`

## Outputs
- `.specs/<id>/02-spec-review.md` — `PASS` / `FAIL` verdict + 필요한 편집의 번호 목록 + 새로 발견한 `Q-NNN`

## Next
`PASS`면 `/plan`으로 넘어간다. `FAIL`이면 `01-spec.md`를 고쳐서 다시 부른다
(사용자 에스컬레이션 전 최대 3회 반복).

---
이 커맨드는 `spec-author`를 **review hat**으로 실행한다. 절차 · 거부조건 · 완료조건 ·
참조 skill은 `.claude/agents/spec-author.md`에 있다.
