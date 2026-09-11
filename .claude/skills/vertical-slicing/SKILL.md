---
name: vertical-slicing
description: Decompose a feature into small end-to-end tasks (1–4 h) that each cut through Controller → Service → Repository with their own tests. Use in /plan.
---
# Vertical slicing (task decomposition)

Break work into tasks that are each **complete and independently testable**, not horizontal layers built in isolation.

Instead of "build all repositories, then all services, then all controllers", do:
```
T-001 (≈2 h): DELETE /user/{id} — repo delete + service + controller 204/404 + unit tests
T-002 (≈2 h): 404 path + error envelope + test
```

Test type default is `spring-unit-testing` (layer-isolated Controller/Service tests) — `spring-integration-testing`
is opt-in only, write one only when explicitly asked for that task.

## Size guide
| Estimate | Assessment |
|---|---|
| < 30 min | too small — merge with the next task |
| 1–4 h | ideal |
| > 4 h | too big — split |

## Task shape (04-tasks.md)
```
### T-001: <short imperative title>
- acs_covered: AC-001, AC-002
- files_in_scope:
  - src/main/java/com/keycloak/userstorage/controller/UserController.java
  - src/test/java/com/keycloak/userstorage/UserControllerTest.java
- depends_on: none
- gates: unit, integration, coverage
- estimated_phases: [red, green, refactor, simplify]
- notes: <helps the implementer; never invents behaviour>
```

## Rules
- Every task touching `src/main/**` also lists a `src/test/**` file (the hook and `/plan` both enforce it).
- Order the task index by dependency.
- Cross-cutting tests (ArchUnit, OpenAPI contract) are their own tasks near the end, not "later".
- Red flags: a task titled "entire feature X", an estimate over 4 h, or "test this afterwards".
