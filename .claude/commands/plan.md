---
description: Phase 3 — PASS 스펙을 03-design.md + 04-tasks.md + ADR + .tdd-state.json으로.
argument-hint: "[feature-id]"
agent: architect
hat: design
---
# /plan — Phase 3

## Purpose
`PASS` verdict를 받은 `01-spec.md`를 구현 가능한 설계로 옮긴다. 컴포넌트 맵과 API 계약을
`03-design.md`에 정리하고, 그 설계를 1~4시간짜리 TDD 태스크로 쪼개 `04-tasks.md`에 담고,
대안이 있었던 결정마다 ADR을 남기고, `.tdd-state.json`을 전 태스크 `pending` 상태로 초기화한다.

## Inputs
- `[feature-id]` — 생략하면 가장 최근 수정된 `.specs/<id>/`

## Outputs
- `.specs/<id>/03-design.md` — 컴포넌트 맵 · OpenAPI 스케치 · 에러 모델 · NFR
- `.specs/<id>/04-tasks.md` — `T-NNN` 목록 (`acs_covered`, `files_in_scope`, `depends_on`, `gates`)
- `.specs/<id>/adr/*.md` — 대안이 있던 결정마다 한 건
- `.specs/<id>/.tdd-state.json` — 전 태스크 `pending`, `active_task: null`

## Next
설계가 서면 `/build T-001`로 첫 태스크를 시작한다. `/validate`가 FAIL을 낸 뒤 다시 부르면
전체 재설계 대신 `Gap-NNN`을 덮는 gap task만 `04-tasks.md`에 추가한다.

---
이 커맨드는 `architect`를 **design hat**으로 실행한다. 절차 · 거부조건 · 완료조건 ·
참조 skill은 `.claude/agents/architect.md`에 있다.
