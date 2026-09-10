---
description: Phase 1 — 요청이나 백로그 항목을 EARS-lite 01-spec.md로 변환.
argument-hint: "<free text> | <feature-id> | .specs/README.md row"
agent: spec-author
---
# /spec — Phase 1

## Purpose
자유 텍스트든 `.specs/README.md` 백로그 행이든, 원시 의도를 완전하고 테스트 가능한
**no-invention** 명세로 옮긴다. 소스에 없는 값은 지어내지 않고 `Q-NNN`으로 남겨
사용자에게 되묻는다.

## Inputs
- 기능을 설명하는 자유 텍스트, `<feature-id>`, 또는 `.specs/README.md`의 백로그 행

## Outputs
- `.specs/<id>/01-spec.md` — `AC-NNN` 인수 조건 목록 (EARS-lite) + 미해결 항목은 `## Open Questions`의 `Q-NNN`

## Next
`Q-NNN`에 답을 채운 뒤 `/spec-review`로 넘어간다.

---
절차 · 거부조건 · 완료조건 · 참조 skill은 `.claude/agents/spec-author.md` (author hat)에 있다.
