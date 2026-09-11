---
description: Phase 4 — 태스크 하나를 red → green → refactor → simplify로. TDD 강제.
argument-hint: "<task-id>  (예: T-001)"
---
# /build — Phase 4

## Purpose
태스크 하나를 red → green → refactor → simplify 네 phase로 완주시킨다. phase마다
`.tdd-state.json`을 전이시키고 `05-implementation-log.md`에 블록을 덧붙인다.
TDD 순서는 hook으로 강제된다 (실패 테스트 없이 `src/main/**` 편집 불가).

## Inputs
- `<task-id>` (예: `T-001`) — 필수

## Outputs
- 프로덕션 코드 + 테스트
- `.specs/<id>/05-implementation-log.md` — phase별 블록 (red / green / refactor / simplify)
- `.tdd-state.json` — `active_task` · `phase` 전이 (커밋은 하지 않음)

## Next
태스크가 끝나면 사용자가 `git commit`한 뒤 `/build <다음 T-NNN>`. `04-tasks.md`의
모든 태스크가 `done`이면 `/validate`로 넘어간다. 자동 커밋 · 다음 태스크 자동 시작은 하지 않는다.

<!--
  frontmatter에 agent 없음: 두 agent를 분기·루프로 지휘하므로 단일 위임 불가
  (서브에이전트는 서브에이전트를 못 스폰). 메인 세션이 아래 ## Orchestration을 직접 실행 —
  red는 test-engineer, green/refactor/simplify는 implementer 서브에이전트. 각 단계 상세는 그 agent 파일.
-->

## Orchestration
0. **Pre-flight.** `git status`. 이전 태스크의 미커밋 변경이 있으면 거부 — `git commit → /build <task-id>` 안내.
1. **Activate.** `.tdd-state.json` `active_task = <task-id>`. 다른 태스크가 TDD phase 중이면 거부 (한 번에 하나).
2. **Red (배치).** `test-engineer` red step — `tdd-red-green-refactor` skill의 "One TDD cycle per task (not per AC)" 규칙대로 태스크의 `acs_covered` 전체를 한 번에 테스트로 표현(AC마다 별도 `@Test`+`@Tag`)하고 클래스 단위로 gradle 1회 실행해 전부 확인. 완료 시 `phase: "red"` + `red_failure_excerpt` 채워짐 + `05-implementation-log.md`에 red 블록 하나(테스트 목록 전체 포함).
3. **Green → Refactor → Simplify (배치).** `implementer` — 그 배치 전체를 한 번의 green/refactor/simplify로 처리(각 1회 gradle 실행). 각 단계 후 `phase` 전이 + 로그 블록(태스크당 1개씩).
4. **Done.** `phase: "done"`, `active_task` 클리어. 정상 흐름은 3번 이후 바로 done(2번의 red가 이미 `acs_covered` 전체를 담음). **예외**: 구현 중 배치에 없던 진짜 새 갭 발견 시 — 기존 AC의 누락 케이스면 그 AC로 태그, 스펙에 없는 새 동작이면 `AC-NNN`을 지어내지 말고 먼저 `01-spec.md`에 정식 추가한 뒤에만 — 테스트 1개를 같은 클래스에 추가해 그 메소드만 국소적으로 red 확인 → green 구현 → **반드시 태스크 전체 테스트 클래스 + 전체 스위트(`./gradlew test`) 재실행으로 최종 확인**한 후에만 다시 done (`tdd-red-green-refactor` skill 규칙 10).
5. **STOP.** `git status` + 통과 테스트 + 제안 커밋 메시지를 표면화. `git commit → /build <다음 task-id>` 권장. 다음 태스크 자동 시작 금지, 자동 커밋 금지.

## Refuse if
- `<task-id>`가 `.tdd-state.json`에 없다.
- 다른 태스크가 진행 중이다.
- 새 Gradle 의존성이 필요하다 — 멈추고 사용자에게 먼저 확인.

## Done when
- `tasks[<task-id>].acs_covered`의 모든 AC에 `@Tag("AC-NNN")` 테스트가 1개 이상.
- `05-implementation-log.md`에 4개 phase 블록(red/green/refactor/simplify)이 있다.
- `.tdd-state.json` `phase: "done"`. 커밋 리마인더 표면화.
- 전 태스크 완료 후: `/validate` → `/review`.
