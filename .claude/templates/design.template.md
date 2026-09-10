# Design: <FEATURE-ID>

> Owner: `architect` · Phase 3 · Template: `.claude/templates/design.template.md`
>
> No new behaviour or NFR beyond `01-spec.md`. A gap → `Q-NNN` here, bounce to `spec-author`.

## Architecture overview
- Component map: <Controller> → <Service (iface + impl)> → <Repository> → <Model>
- New/changed packages: <…>
- ADRs: <ADR-001 …>

## Module boundaries (ArchUnit)
- New rule(s) to add to `ArchitectureTest.java`: <…> (see `archunit-rules` skill)

## API Contract (OpenAPI sketch)
- 신규/변경 엔드포인트마다 request + success + error 예시와 상태코드 (see `openapi-contract-first` skill).

## Data model
- Entities touched: `User` / `USER_ATTRIBUTES` / `CredentialData` — <changes>
- H2 `ddl-auto=update` — no migration file; schema-affecting change: <yes/no, describe>

## Error model
Per `spring-error-handling`: <status codes for this feature>. Envelope `{"error": "..."}`.

## Logging
- New `log.warn`/`log.info` points: <…>

## NFRs
> Only what `01-spec.md` already requires.
- <…> or (none)

## Risks & rollback
- Risk: <…> — Mitigation: <…>
- Rollback: revert commit; <schema impact>

## Open Questions
- Q-NNN: <…> — Status: open

## Resolved Questions
- (none yet)

## Design-review sign-off
- [ ] `.claude/checklists/design-review.md` passes.
- [ ] Every AC reachable from ≥1 task in `04-tasks.md`.
- [ ] No unresolved `Q-NNN`.
