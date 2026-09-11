---
name: tdd-red-green-refactor
description: The four-phase build cycle enforced by .tdd-state.json and hooks — red (failing test) → green (minimum code) → refactor → simplify. Use in /build.
---
# TDD: red → green → refactor → simplify

One task runs through four phases. `.tdd-state.json` tracks the phase; hooks refuse `src/main/**` edits outside it.

## One TDD cycle per task (not per AC)

The **task** (`04-tasks.md`, `T-NNN`) is the unit of TDD execution — not the individual AC.
Repeating the full cycle for every AC just repeats agent spawns and Gradle runs — extra cost, no extra safety, when a task's ACs already share one class.

**Normal flow**
1. AC = the unit of a **test** (`@Test` + `@Tag("AC-NNN")`). Task = the unit of a **cycle**. One task runs one red → green → refactor → simplify cycle, covering every AC in `acs_covered` together — never one cycle per AC.
2. Red writes all of the task's ACs as tests first, each with its own `@Test` + `@Tag`, then runs Gradle once per **class** (`--tests 'ClassName'`), not per method.
3. Green makes every one of those red tests pass in a single implementation pass.

**Exception — a gap surfaces mid-task**
4. Missed case of an **existing** AC → add one `@Test` with that AC's tag, re-check just that method.
5. Genuinely new behaviour with **no** AC → don't invent one. Stop and route to `spec-author` to add a real `AC-NNN` to `01-spec.md` first.
6. Either way, close with a **full re-verification** — the whole class, then the full suite (`./gradlew test`) — before the task is `done` again. A one-method fix can touch shared code the batch's other green tests depend on.

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
Smallest test per AC, but write **every** AC in the task's `acs_covered` together in one pass (see "One TDD cycle per task (not per AC)" above). Annotation block per test: `@Test` → `@Tag("AC-NNN")` → `@DisplayName("T-NNN: given …, when …, then …")`. Run `./gradlew test --tests 'com.keycloak.userstorage.ClassName'` once for the whole class. Every new test must **fail for its AC reason** (not a compile error). Record one `red_failure_excerpt` summarizing the batch.

## Phase 2 — Green (implementer)
Minimum production code under `files_in_scope` to pass **every** red test in this task's batch — not just the first one you look at. Hardcode if the current batch allows it; don't build ahead for ACs outside this task. Run the test class once, then `./gradlew test`.

## Phase 3 — Refactor (implementer)
Remove duplication, move logic to the right layer, rename. Re-run the suite after each edit. No signature or behaviour changes. One `refactor` block, task-wide — not per AC.

## Phase 4 — Simplify (implementer)
Apply `clarity-over-cleverness`. Suite stays green. One `simplify` block, task-wide. Set `phase: "done"`, clear `active_task`.

## Step 5 — STOP
Surface `git status`, passing tests, a suggested commit message. Recommend `git commit → /build <next>`. **Never auto-commit. Never auto-start the next task.** (A remaining AC in the task means the batch was incomplete at Red — fix that in Phase 1, not by looping Step 5.)

## Hard bans
`./gradlew -x test`, `-Dpitest.skip`, `--no-verify`, removing assertions, `@Disabled` without `# DisabledReason`, editing files outside `files_in_scope`, looping the full red→green cycle per AC as the default flow (batch the task instead — see "One TDD cycle per task (not per AC)"), inventing an `AC-NNN` for a gap test.
