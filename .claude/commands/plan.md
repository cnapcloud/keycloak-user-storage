---
description: Phase 3 — PASS 스펙을 03-design.md + 04-tasks.md + ADR + .tdd-state.json으로.
argument-hint: "[feature-id]"
agent: architect
---
# /plan

**Phase 3.** 실행: `.claude/agents/architect.md` (design hat).

## Purpose
`PASS` verdict 스펙을 `03-design.md` + `04-tasks.md`로 번역하고, 유의미한 선택마다 ADR을 쓰고, `.tdd-state.json`을 초기화한다.
절차 · 거부조건 · 완료조건 · 참조 skill은 owning agent에 있다.

## Inputs
- `<feature-id>` (positional; 생략 시 가장 최근 feature)
