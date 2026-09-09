---
description: Phase 6 — 커밋 전 diff 셀프리뷰, 08-code-review.md 산출.
argument-hint: "[feature-id] [--base <ref>]"
agent: validator
---
# /review

**Phase 6.** 실행: `.claude/agents/validator.md` (review hat).

## Purpose
사용자가 커밋하기 전, diff를 루브릭에 대조해 심각도 태그가 붙은 findings와 최종 verdict를 `08-code-review.md`에 남긴다. 코드를 고치지 않고 커밋하지 않는다.
절차 · 거부조건 · 완료조건 · 참조 skill은 owning agent에 있다.

## Inputs
- `<feature-id>` (선택; 생략 시 가장 최근 feature)
- `--base <ref>` (기본 `origin/main`)
