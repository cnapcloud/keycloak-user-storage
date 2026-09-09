---
name: adr-authoring
description: Record architecturally significant decisions as short MADR-style ADRs under .specs/<feature>/adr/. Use in /plan whenever a choice has a plausible alternative.
---
# ADR authoring

Write an ADR when a decision has a real alternative and future readers would ask "why this way?". Skip it for choices the codebase already forces.

## When
- Choosing between two persistence shapes (e.g. `@ElementCollection` vs a child entity for attributes).
- Introducing a global `@ControllerAdvice` vs keeping per-endpoint `ResponseStatusException`.
- Any `/validate` waiver — a waived gate **must** point at an ADR.

## File
`.specs/<feature-id>/adr/ADR-NNN-<kebab-title>.md`, `NNN` zero-padded, increasing per feature.

## Template
```markdown
# ADR-001: <decision title>

- Status: proposed | accepted | superseded by ADR-NNN
- Date: YYYY-MM-DD
- Feature: <feature-id>

## Context
<the forces: requirement, constraint, existing pattern, what the spec says>

## Decision
<what we will do, stated plainly>

## Alternatives considered
- <alt A> — why not
- <alt B> — why not

## Consequences
- Positive: …
- Negative / follow-up: …
- Gates affected: <e.g. adds an ArchUnit rule; waives coverage on X with rationale>
```

## Rules
- One decision per ADR.
- Never delete an ADR — supersede it and link forward.
- The `03-design.md` architecture section links every ADR it depends on.
