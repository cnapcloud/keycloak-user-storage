---
description: Phase 6 — 커밋 전 diff 셀프리뷰, 08-code-review.md 산출.
argument-hint: "[feature-id] [--base <ref>]"
agent: validator
hat: review
---
# /review — Phase 6

## Purpose
사용자가 커밋하기 직전, `git diff`를 `code-review-rubric`의 아홉 항목에 대조한다.
발견 사항마다 심각도를 매기고 최종 verdict를 낸다. 코드를 고치거나 커밋하지 않는다 —
findings만 남긴다.

## Inputs
- `[feature-id]` — 생략하면 가장 최근 feature
- `--base <ref>` — 비교 기준 (기본 `origin/main`)

## Outputs
- `.specs/<id>/08-code-review.md` — `F-NNN` findings (`must-fix` / `should-fix` / `nit` / `praise`) + Approve / Approve-with-waivers / Request-changes

## Next
`must-fix`가 0건이면 사용자가 직접 `git commit`한다. 남아 있으면 `/build`로 돌아가 고친다.

---
이 커맨드는 `validator`를 **review hat**으로 실행한다. 절차 · 거부조건 · 완료조건 ·
참조 skill은 `.claude/agents/validator.md`에 있다.
