---
description: Phase 4 — 태스크 하나를 red → green → refactor → simplify로. TDD 강제.
argument-hint: "<task-id>  (예: T-001)"
---
# /build

<!--
  frontmatter에 agent를 두지 않는다. 이 커맨드는 두 agent를 분기·루프로 지휘하는데,
  서브에이전트는 다른 서브에이전트를 스폰할 수 없으므로(중첩 불가) 단일 agent에 위임할 수 없다.
  따라서 메인 세션이 이 파일의 ## Orchestration을 직접 실행하며,
  red는 test-engineer, green/refactor/simplify는 implementer 서브에이전트에 순차 위임한다.
  가벼운 지휘 로직(git 체크, .tdd-state.json 전이, 루프, STOP)만 메인 세션에 남고,
  무거운 작업(테스트/코드 작성)은 각 콜드 서브에이전트 안에 격리된다.
-->

**Phase 4.** 오케스트레이션 커맨드. **메인 세션**이 아래 순서를 실행하며 두 agent를 순차 위임한다:
`.claude/agents/test-engineer.md` (red) → `.claude/agents/implementer.md` (green/refactor/simplify).
각 단계의 상세 절차 · 거부조건 · 완료조건 · 참조 skill은 각 agent 파일에 있다.

## Purpose
태스크 하나를 4개 TDD phase로 끝까지 실행하고, phase마다 `.tdd-state.json`을 전이시키고 `05-implementation-log.md`에 블록을 추가한다.

## Inputs
- `<task-id>` (예: `T-001`). 필수.

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
