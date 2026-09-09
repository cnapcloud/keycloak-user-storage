---
name: tdd-red-green-refactor
description: The four-phase build cycle enforced by .tdd-state.json and hooks — red (failing test) → green (minimum code) → refactor → simplify. Use in /build.
---
# TDD: red → green → refactor → simplify

One task runs through four phases. `.tdd-state.json` tracks the phase; hooks refuse `src/main/**` edits outside it.

## `.tdd-state.json` shape
```json
{
  "active_task": "T-001",
  "tasks": {
    "T-001": {
      "phase": "red",
      "acs_covered": ["AC-001"],
      "files_in_scope": [
        "src/main/java/com/keycloak/userstorage/controller/UserController.java",
        "src/test/java/com/keycloak/userstorage/UserControllerTest.java"
      ],
      "red_at": "2026-09-08T05:00:00Z",
      "red_failure_excerpt": "expected: 204 but was: 200"
    }
  }
}
```
`phase` moves `pending → red → green → refactor → simplify → done`. `active_task` is `null` between tasks.

## Phase 1 — Red (test-engineer)
Smallest test that captures one AC slice. Annotation block: `@Test` → `@Tag("AC-NNN")` → `@DisplayName("T-NNN: given …, when …, then …")`. Run `./gradlew test --tests 'com.keycloak.userstorage.ClassName.method'`. It must **fail for the AC reason** (not a compile error). Record `red_failure_excerpt`.

## Phase 2 — Green (implementer)
Minimum production code under `files_in_scope` to pass. Hardcode if one test allows it. Run the new test, then `./gradlew test`.

## Phase 3 — Refactor (implementer)
Remove duplication, move logic to the right layer, rename. Re-run the suite after each edit. No signature or behaviour changes.

## Phase 4 — Simplify (implementer)
Apply `clarity-over-cleverness`. Suite stays green. Set `phase: "done"`, clear `active_task`.

## Step 5 — STOP
Surface `git status`, passing tests, a suggested commit message. Recommend `git commit → /build <next>`. **Never auto-commit. Never auto-start the next task.** If ACs in the task remain, loop back to Red with the next slice first.

## Hard bans
`./gradlew -x test`, `-Dpitest.skip`, `--no-verify`, removing assertions, `@Disabled` without `# DisabledReason`, editing files outside `files_in_scope`.
