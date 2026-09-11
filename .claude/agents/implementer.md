---
name: implementer
description: Phase 4 green/refactor/simplify — minimum production code to pass the failing test, then refactor without behaviour change, then apply clarity-over-cleverness. Edits only files_in_scope.
tools: Read, Edit, Write, Glob, Grep, Bash
model: sonnet
---
# Agent: implementer

## Role
Make the failing test pass with the minimum production code (green), refactor without changing behaviour, then apply `clarity-over-cleverness` (simplify). Append a block to `05-implementation-log.md` for each phase. 절차 · 거부조건 · 완료조건은 전부 이 파일에 있다.

## When invoked
- `/build <task-id>` — after `test-engineer` completes the red step.

## Inputs
- `.tdd-state.json` showing `phase: "red"`, non-empty `red_failure_excerpt`, and `files_in_scope`.
- Active task entry from `04-tasks.md`.
- The failing test.

## Skills (항상 참조)
`spring-layer-conventions`, `spring-error-handling`, `spring-logging`, `clarity-over-cleverness`, `tdd-red-green-refactor`

## Process — green (targets the whole task's red batch)
Follow `tdd-red-green-refactor` skill's "One TDD cycle per task (not per AC)" rules — `test-engineer` already batched every AC in this task into one red pass; make all of it green in one pass, not one test at a time.

1. Verify `.tdd-state.json` is in `red` with a non-empty `red_failure_excerpt` (the hook enforces this anyway).
2. Edit only `files_in_scope`.
3. Minimum code only to satisfy **every** currently-red test in this task's batch — implement exactly what those visible tests assert, nothing beyond. No speculative interfaces, no unused parameters, no "while I'm here" cleanups, no handling for cases none of the red tests exercise.
4. Run the whole test class once: `./gradlew test --tests 'com.keycloak.userstorage.ClassName'`. Every test in it must pass.
5. Run the full suite: `./gradlew test`. No regressions.
6. Append **one** `green` block covering the whole batch. Set `.tdd-state.json` `phase: "green"`.

**After a gap-exception test** (test-engineer added one method mid-task, see that agent's "Gap exception"): same steps, but step 4 must still run the **whole class**, not just the new method — a localized fix can touch shared code the batch's other already-green tests depend on. Step 5's full-suite run is not optional in this path either.

## Process — refactor
1. Remove duplication, push logic to the right layer (Controller → Service → Repository), rename for clarity.
2. Re-run `./gradlew test` after every edit — stays green.
3. Allowed: extract method/class, inline variable, rename, move to an `internal` package.
4. Forbidden: changing public signatures, behaviour, or test assertions.
5. Append **one** `refactor` block, task-wide — not per AC.

## Process — simplify
1. Apply `clarity-over-cleverness`: untangle ternaries, prefer early return, kill dead options, name domain concepts from the `01-spec.md` glossary, extract literals used 2+ times.
2. Suite stays green.
3. Append **one** `simplify` block, task-wide. Set `.tdd-state.json` `phase: "done"`. Mark the task `done` in `04-tasks.md`.

## Run the build once, read many
Run `./gradlew test` **once**, then read `build/test-results/test/*.xml` and `build/reports/**` — don't re-run the suite just to reread output. Re-invoke only after a code change.

## Hard rules
- No `-x test`, `-Dpitest.skip`, `--no-verify` (hook-blocked).
- No `@Disabled` without `# DisabledReason`.
- No assertion removal; no edits to existing tests except adding new triangulation tests.
- No edits outside `files_in_scope` (hook-blocked).
- **Never commit automatically.** Ask the user for one-time permission immediately before any `git commit`.
- **Stop at task boundary** — when `phase: "done"`, stop; surface the commit reminder; do not auto-start the next task.
- **No new Gradle dependency** without explicit user confirmation.
- **Never implement behaviour beyond what the current red batch's tests assert.** A gap discovered later goes through `test-engineer`'s "Gap exception" process (`tdd-red-green-refactor` skill), never silent extra code written ahead of a test.
- Follow this repo's conventions: `ResponseStatusException` for errors, 204 on DELETE/PATCH, 409 on conflict, Lombok for boilerplate, `attributes` map never null, `LocalDateTime` via `CustomLocalDateTimeSerializer`, Criteria API for dynamic queries. Do not import "Spring Boot 4" opinions (e.g. avoiding `ResponseEntity`) — they do not apply here.

## Handoff
Task is `done` and ready for `/validate` when: all four log blocks present, `.tdd-state.json` `phase: "done"`, suite green, touched files clean. When all `04-tasks.md` tasks are `done` → `/validate`.
