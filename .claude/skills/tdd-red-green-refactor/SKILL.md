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
      "red_failure_excerpt": "expected: 204 but was: 200",
      "blocked_reason": null
    }
  }
}
```
`phase` moves `pending → red → green → refactor → simplify → done`. `active_task` is `null` between tasks.

`blocked_reason` (string, optional — omit or `null` when not blocked): set when an out-of-scope regression holds a task at its current `phase` instead of advancing it. See "Blocked: out-of-scope regression found mid-task" below. Not itself a `phase` value — `phase` stays whatever step was last completed (`green`, `refactor`, or `simplify`).

## Phase 1 — Red (test-engineer)
Smallest test per AC, but write **every** AC in the task's `acs_covered` together in one pass (see "One TDD cycle per task (not per AC)" above). Annotation block per test: `@Test` → `@Tag("AC-NNN")` → `@DisplayName("T-NNN: given …, when …, then …")`. Run `./gradlew test --tests '<root-package>.ClassName'` (this project: `com.keycloak.userstorage.ClassName`) once for the whole class. Every new test must **fail for its AC reason** (not a compile error). Record one `red_failure_excerpt` summarizing the batch.

## Phase 2 — Green (implementer)
Minimum production code under `files_in_scope` to pass **every** red test in this task's batch — not just the first one you look at. Hardcode if the current batch allows it; don't build ahead for ACs outside this task. Run the test class once, then `./gradlew test`.

## Phase 3 — Refactor (implementer)
Remove duplication, move logic to the right layer, rename. Re-run the suite after each edit. No signature or behaviour changes. One `refactor` block, task-wide — not per AC.

## Phase 4 — Simplify (implementer)
Apply `clarity-over-cleverness`. Suite stays green. One `simplify` block, task-wide. Set `phase: "done"`, clear `active_task`.

## Blocked: out-of-scope regression found mid-task
Phase 2's full-suite run (`./gradlew test`) can surface a real regression whose fix
requires editing a file that isn't in the task's `files_in_scope` — e.g. a pre-existing
bug in an unrelated service, or (as happened building this feature) a shared
`@SpringBootApplication` class that a new test slice can't load. `enforce-files-in-scope.sh`
hook-blocks that edit; this is not the "spec conflict" case in Phase 2 (a new AC's test
disagreeing with an old one) — it's an unrelated bug the task's own batch merely exposed.

1. Do **not** edit the out-of-scope file. Do **not** weaken or skip the regressed test.
2. Do **not** advance `phase` past whatever step just completed (`green`, `refactor`, or
   `simplify`) — never set `done` while the full suite has unexplained failures.
3. Set `blocked_reason` on the task: what regressed (test name + failure), the root cause
   and file:line, why it's outside `files_in_scope`, and that it awaits a user decision.
4. Report this to the user in the same turn and stop — don't guess at a fix, don't
   silently continue to the next phase.
5. Once the user decides (commonly: expand `files_in_scope` to cover the fix, same as any
   other scope-expansion decision), fix it, re-run the full suite clean, clear
   `blocked_reason` (set to `null` or remove it), and resume the normal phase sequence.

## Step 5 — STOP
Surface `git status`, passing tests, a suggested commit message. Recommend `git commit → /build <next>`. **Never auto-commit. Never auto-start the next task.** (A remaining AC in the task means the batch was incomplete at Red — fix that in Phase 1, not by looping Step 5.)

## Hard bans
`./gradlew -x test`, `-Dpitest.skip`, `--no-verify`, removing assertions, `@Disabled` without `# DisabledReason`, editing files outside `files_in_scope`, looping the full red→green cycle per AC as the default flow (batch the task instead — see "One TDD cycle per task (not per AC)"), inventing an `AC-NNN` for a gap test.
