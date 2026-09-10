---
description: Phase 0 — repo 분류, harness baseline 캡처, .specs/_onboarding.md 작성.
argument-hint: "[optional: subdir path]"
agent: architect
hat: onboarding
---
# /onboard — Phase 0

## Purpose
리포지토리를 조사해 스택을 기록하고 **brownfield baseline**을 캡처한다. 이후 모든 게이트가
이 "시작 지점" 대비로만 회귀를 따지므로 도입 첫날부터 차단당하는 일을 막는다.
`/spec` · `/plan` · `/validate`가 공통으로 읽는 repo 단위 `.specs/_*` 파일을 만든다.

## Inputs
- 없음 — 단일 모듈 Gradle repo라 인자가 필요 없다

## Outputs
- `.specs/_stack.json` — 스택 분류 + 활성 harness 레이어
- `.specs/_baseline.json` — 게이트 기준선 (이후 이보다 나빠지면 FAIL)
- `.specs/_onboarding.md` — 분류 결과 + baseline 표 + 미설정 레이어 + 권장 첫 `/spec`

## Next
repo당 한 번만 실행한다. 끝나면 `/spec`으로 첫 기능을 시작한다.

---
이 커맨드는 `architect`를 **onboarding hat**으로 실행한다. 절차 · 거부조건 · 완료조건 ·
참조 skill은 `.claude/agents/architect.md`에 있다.
