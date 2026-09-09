# Checklist: Implementation Definition of Done (per task, Phase 4)

A task is `done` only when every box is checked.

## TDD cycle
- [ ] `red` block in `05-implementation-log.md` — test ran and failed for the AC reason.
- [ ] `green` block — minimum code, full suite passes (`./gradlew test`).
- [ ] `refactor` block — structure improved, no signature/behaviour change, suite green.
- [ ] `simplify` block — `clarity-over-cleverness` applied, suite green.
- [ ] `.tdd-state.json` shows this task `phase: "done"`, `active_task` cleared.

## Tests
- [ ] Every AC in `acs_covered` has ≥1 test with `@Tag("AC-NNN")` + `@DisplayName("<T-id>: given …, when …, then …")`.
- [ ] Write tests clean up after themselves (create → delete).
- [ ] `null`-value request bodies sent as raw JSON strings.
- [ ] No `@Disabled` without `# DisabledReason`. No weakened assertions.

## Code
- [ ] Edits stayed inside `files_in_scope`.
- [ ] Layer boundaries respected (no Repository in Controller, no reflection for field writes).
- [ ] Error handling per convention (status codes, `ResponseStatusException`, 204 on DELETE/PATCH).
- [ ] `attributes` map never null; PATCH null=remove / absent=keep.
- [ ] Literals used 2+ times extracted to `private static final`.
- [ ] No new Gradle dependency (or the user explicitly approved one).
- [ ] `@Slf4j`, no `System.out`.

## Boundary
- [ ] `git status` surfaced with a suggested commit message.
- [ ] Did NOT auto-commit or auto-start the next task.
