# Checklist: Spec Review (Phase 2 gate)

Walk every item. Record `pass | fail | n/a` + a one-line rationale in `02-spec-review.md`.

## Source & scope
- [ ] `## Source` records where the request came from and a snapshot date.
- [ ] `## Goal` is one paragraph of user-visible outcome — no implementation.
- [ ] `## Non-Goals` explicitly lists what is out of scope.

## Acceptance criteria
- [ ] Every AC uses an EARS-lite shape.
- [ ] Every AC is atomic — one condition, one outcome.
- [ ] Every AC is testable (maps to a concrete assertion).
- [ ] AC ids are `AC-NNN`, sequential, none reused.
- [ ] No implementation language in any AC (no class/library/table names).
- [ ] USP contract endpoints touched are referenced where relevant.

## Domain & data
- [ ] Conceptual entities + relationships (with cardinality) are stated in business language.
- [ ] Data impact on `User` / `USER_ATTRIBUTES` / `CredentialData` is explicit.
- [ ] `attributes` null-safety and PATCH semantics (null=remove, absent=keep) are respected where relevant.

## NFRs & assumptions
- [ ] Every NFR has a concrete number, or is a `Q-NNN`.
- [ ] `## Assumptions` contains only user/source-stated assumptions.

## Open questions
- [ ] Every uncertainty is a `Q-NNN` with "why it matters" + candidate options.
- [ ] `## Open Questions` is empty (or every item `deferred`-with-rationale) before verdict `PASS`.

## Overall
- [ ] Verdict line present: `PASS` or `FAIL` + one-line rationale.
- [ ] If `FAIL`, each required edit names a line and a replacement.
