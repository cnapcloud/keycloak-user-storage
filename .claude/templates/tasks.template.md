# Tasks: <FEATURE-ID>

> Owner: `architect` · Phase 3 · Template: `.claude/templates/tasks.template.md`
>
> One task ≈ 1–4 h. Each task has `acs_covered`, `files_in_scope` (incl. a `src/test/**` file when it touches `src/main/**`), and gates. Executed by `/build <task-id>`.

## Inputs
- `03-design.md` revision: <git-sha or timestamp>

## Task index
| ID | Title | acs_covered | depends_on | gates |
|----|-------|-------------|------------|-------|
| T-001 | … | AC-001 | — | unit, integration, coverage |
| T-002 | … | AC-002, AC-003 | T-001 | unit, integration, coverage |

## Tasks

### T-001: <short imperative title>
- **acs_covered:** AC-001
- **files_in_scope:**
  - `src/main/java/com/keycloak/userstorage/<...>.java`
  - `src/test/java/com/keycloak/userstorage/<...>Test.java`
- **depends_on:** none
- **gates:** unit, integration, coverage
- **estimated_phases:** [red, green, refactor, simplify]
- **notes:** <helps the implementer; never invents behaviour>

### T-NNN: …

## Cross-cutting tasks (near the end)
- ArchUnit rule additions from `03-design.md`
- OpenAPI contract check (once springdoc is wired)

## Open Questions
- Q-NNN: …

## Sign-off
- [ ] Every AC from `01-spec.md` is covered by ≥1 task.
- [ ] Every task touching `src/main/**` lists a `src/test/**` file.
- [ ] Task index is in dependency order.
- [ ] All `Q-NNN` resolved or deferred-with-rationale.
- [ ] Reviewed by user on <YYYY-MM-DD>.
