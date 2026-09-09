---
description: Phase 1 — 요청이나 백로그 항목을 EARS-lite 01-spec.md로 변환.
argument-hint: "<free text> | <feature-id> | .specs/README.md row"
agent: spec-author
---
# /spec

**Phase 1.** 실행: `.claude/agents/spec-author.md` (author hat).

## Purpose
원시 의도(문장, 문단, 또는 `.specs/README.md` 백로그 행)를 완전하고 테스트 가능한 **no-invention** 명세 `.specs/<feature-id>/01-spec.md`로 만든다.
절차 · 거부조건 · 완료조건 · 참조 skill은 owning agent에 있다.

## Inputs
- 기능을 설명하는 자유 텍스트, 또는 `.specs/README.md`의 백로그 id / 행
