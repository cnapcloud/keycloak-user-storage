# Spec: <FEATURE-ID> — <short title>

> Owner: `spec-author` · Phase 1 · Template: `.claude/templates/spec.template.md`
>
> **No invention.** Anything not in the request, the conversation, or the code is a `Q-NNN` — ask the user.

## Source
- Origin: <backlog row in .specs/README.md | user request | USP guide section>
- Reference: <link or path>
- Snapshot date: <YYYY-MM-DD>
- Snapshot summary:
  > <verbatim or close paraphrase of the request>

## Goal
<one paragraph — the user-visible outcome>

## Acceptance Criteria
EARS-lite (see `.claude/docs/spec-format.md`). One condition per AC. IDs stable, never reused.

- AC-001: <When …, the system shall …> | <If …, then the system shall …> | <While …, the system shall …> | <The system shall …>
- AC-002: …

## Domain Entities and Relationships
> Business language only — no class/table/ORM names. Unknown → `Q-NNN`.

- **<Entity>** — purpose: <…>; key attributes: <…>
- **<A> 1..* <B>** — meaning: <business rule>

## Data Impact
- New table/column, or change to `User` / `USER_ATTRIBUTES` / `CredentialData`: <…>
- Shared with another endpoint: <yes/no — which>

## Non-Goals
- <explicitly out of scope>

## Glossary
- **Term** — definition.

## Assumptions
> Only assumptions stated by the user or the source. The agent never adds one silently.
- (none)

## Open Questions
> Every uncertainty. The agent does not pick a default.

- Q-001: <question>
  - Why it matters: <impact on design or behaviour>
  - Candidate options: <A>, <B>
  - Status: open

## Resolved Questions
> Verbatim user answer + timestamp.
- (none yet)

## Sign-off
- [ ] All AC atomic and testable.
- [ ] All `Q-NNN` resolved or deferred-with-rationale.
- [ ] Source recorded.
- [ ] Reviewed by user on <YYYY-MM-DD>.
