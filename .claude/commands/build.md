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
2. **Red.** `test-engineer` red step. 완료 시 `phase: "red"` + `red_failure_excerpt` 채워짐 + `05-implementation-log.md`에 `red` 블록.
3. **Green → Refactor → Simplify.** `implementer`. 각 단계 후 `phase` 전이 + 로그 블록.
4. **Done.** `phase: "done"`, `active_task` 클리어. 이 태스크의 `acs_covered`에 아직 커버 안 된 AC가 있으면 스텝 2로 루프 (다음 슬라이스).
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
