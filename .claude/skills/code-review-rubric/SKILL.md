---
name: code-review-rubric
description: The nine-section rubric /review walks the diff against, tuned to keycloak-user-storage conventions. Use in /review.
---
# Code review rubric

Walk `git diff origin/main...HEAD` file by file. For each hunk, evaluate every section. Record findings as `F-NNN` with severity `must-fix` / `should-fix` / `nit` / `praise`, plus file + line + suggested change.

## 1. Traceability
Every changed public method traces to an AC. Every AC in `01-spec.md` has ≥1 test in the diff or already merged. No orphan code.

## 2. Layer boundaries
Controller → Service → Repository → Model only. Controller does not inject a Repository. No reflection for field writes. (Mirror `archunit-rules`.)

## 3. Spring idioms
Constructor injection. `@Transactional` on write paths (impl methods). Lombok for boilerplate — no hand-written getters/setters. `ResponseEntity` return types are fine in this project.

## 4. Error handling
`ResponseStatusException`, not raw `RuntimeException`. 200/201/204/404/409 per the convention table. DELETE/PATCH return 204 with no body. Error envelope `{"error": "..."}`.

## 5. Data access
`attributes` map never null (`new HashMap<>()`), `FetchType.EAGER`. PATCH semantics: null → remove, absent → keep. Dynamic queries via Criteria API in `UserRepositoryImpl`, not string-concatenated JPQL. SQL reserved words escaped.

## 6. Dates & serialisation
`LocalDateTime` only (no `Timestamp`). Serialised via `CustomLocalDateTimeSerializer` (`yyyy-MM-dd'T'HH:mm:ss`). `additionParameters` stored/returned as a raw JSON string — not parsed and re-serialised.

## 7. Test quality
Every new `@Test` has `@Tag("AC-NNN")` + `@DisplayName("<T-id>: given …, when …, then …")`. Write tests clean up (create → delete). `null`-value bodies sent as raw JSON. No weakened assertions. No `@Disabled` without `# DisabledReason`.

## 8. Clarity over cleverness
Apply the `clarity-over-cleverness` skill as a review lens — flag clever hunks with a rewrite, do not auto-apply.

## 9. Migration / contract
H2 `ddl-auto=update` — no migration files, but call out schema-affecting entity changes (`USER_ATTRIBUTES`). Breaking OpenAPI changes vs `origin/main` are `must-fix` unless an ADR waives them.

## Verdict
- Approve — no `must-fix`, no unwaived `should-fix` on public behaviour.
- Approve with waivers — each waiver references an ADR.
- Request changes — unwaived `must-fix` exists. Commit blocked.
