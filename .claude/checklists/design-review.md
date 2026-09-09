# Checklist: Design Review (Phase 3 gate)

`architect` self-review before handing off to `/build`.

## Design
- [ ] Component map covers Controller → Service (iface+impl) → Repository → Model.
- [ ] Package/module boundaries stated; new ArchUnit rule(s) listed if a boundary is added.
- [ ] OpenAPI sketch present for every new/changed endpoint (path, method, request, response, status).
- [ ] Error model matches `spring-error-handling` (status table, `{"error": "..."}` envelope).
- [ ] Data model change to `User` / `USER_ATTRIBUTES` / `CredentialData` is spelled out; schema impact noted.
- [ ] Date handling uses `LocalDateTime` + `CustomLocalDateTimeSerializer`.
- [ ] No new behaviour or NFR beyond `01-spec.md` (any gap is a `Q-NNN` here).

## ADRs
- [ ] Every decision with a plausible alternative has an ADR under `adr/`.
- [ ] `03-design.md` links each ADR it depends on.

## Tasks
- [ ] Every AC from `01-spec.md` is covered by ≥1 task.
- [ ] Each task is 1–4 h.
- [ ] Each task lists `acs_covered`, `files_in_scope`, `depends_on`, `gates`.
- [ ] Every task touching `src/main/**` also lists a `src/test/**` file.
- [ ] Task index is in dependency order.
- [ ] Cross-cutting tasks (ArchUnit, contract) are explicit, not "later".

## State
- [ ] `.tdd-state.json` written: all tasks `phase: "pending"`, `active_task: null`, `files_in_scope`/`acs_covered` per task.

## Overall
- [ ] No unresolved `Q-NNN` in `03-design.md` or `04-tasks.md`.
