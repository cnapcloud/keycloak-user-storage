---
name: requirements-traceability
description: Keep AC-NNN ↔ task ↔ test ↔ code ↔ gate linked end to end. Use in /plan (coverage check), /build (tag tests), /validate (matrix).
---
# Requirements traceability

Every acceptance criterion must be reachable from a task, exercised by a test, and reported in the validation matrix. Zero uncovered ACs, zero orphan tests.

## The chain
```
AC-NNN (01-spec.md)
  └─ covered by ≥1 task  (04-tasks.md: acs_covered)
       └─ asserted by ≥1 test  (@Tag("AC-NNN") + @DisplayName)
            └─ exercises code  (src/main/**)
                 └─ reported    (07a-traceability.md)
```

## Rules
- `/plan` FAILs if any AC has no covering task.
- `/build` red step: the test annotation block is `@Test` → `@Tag("AC-NNN")` → `@DisplayName("<T-id>: given …, when …, then …")`.
- `/validate` runs `.claude/scripts/traceability.sh <feature-id>`; any AC with zero tests = FAIL.
- An **orphan test** (asserts behaviour tied to no AC) is allowed only for regression/utility coverage — give it a plain BDD `@DisplayName`, no `@Tag`.
- An **orphan code path** (new public method with no test and no AC) is a design gap → `Q-NNN`.

## Matrix format (07a-traceability.md)
| AC | Tasks | Tests | Status |
|----|-------|-------|--------|
| AC-001 | T-001 | `UserControllerTest.rejectsDuplicate` | covered |
| AC-002 | T-002 | `UserStorageIntegrationTest.patchNullRemovesKey` | covered |
