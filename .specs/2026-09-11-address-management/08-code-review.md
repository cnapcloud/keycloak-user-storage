# Code Review: 2026-09-11-address-management

> Owner: `validator` (review hat) · Phase 6 · Template: `.claude/templates/code-review.template.md`
> Diff: `git diff bbf7c8d...HEAD -- src/` (feature-scoped; `bbf7c8d` = last completed feature boundary, since `origin/main` has not moved and a full `origin/main...HEAD` diff would re-review the already-approved `fix-otp-seed-test-drift` feature and initial harness introduction) · Validation verdict: PASS

## Findings
| ID | Severity | File:line | Finding | Suggested change |
|----|----------|-----------|---------|------------------|
| F-001 | should-fix | `src/test/java/com/keycloak/userstorage/integration/AddressIntegrationTest.java:81` (working tree, uncommitted) | `createAddress_sameUserTwice_bothSucceed` exercises AC-012 but the committed diff only tags it `@Tag("AC-001")` — `traceability.sh` reported it MISSING (Gap-001 in `07-validation-report.md`, first run). | Already fixed in the working tree (`@Tag("AC-012")` added) but not yet committed — include this file in the commit for this feature so the fix is actually part of history, not just the local tree. |
| F-002 | nit | `src/main/java/com/keycloak/userstorage/DataInitializer.java:30-34` | New `@Component` uses field injection (`@Autowired` on fields) while every other new class in this diff (`AddressController`, `AddressServiceImpl`, `UserServiceImpl`) uses constructor injection. Straight copy from the old `UserApplication.initData()`, so not a new defect, but it's a freshly-created class and now the only field-injected one in the diff. | Switch to constructor injection for consistency, if touched again — not urgent enough to hold up this PR. |
| F-003 | nit | `src/main/java/com/keycloak/userstorage/controller/AddressController.java:203,222,230` | `@Transactional` on `createAddress`/`updateAddress`/`deleteAddress` controller methods is redundant — the rubric's Spring-idioms section puts `@Transactional` on service impl methods, which `AddressServiceImpl` already has. Mirrors `UserController.updateUser`'s existing (pre-diff) convention, so consistent with the codebase rather than a new problem. | No action required; flagging only because the rubric calls it out. If the repo ever standardizes on service-only `@Transactional`, both controllers would need updating together. |
| F-004 | praise | `src/main/java/com/keycloak/userstorage/service/AddressServiceImpl.java:36-402` | Clean layer decomposition — `requireUserExists`/`requireOwnedAddress`/`validate` private helpers keep each public method a few lines and avoid duplicated 404/400 logic across create/get/update/delete. | — |
| F-005 | praise | `.specs/2026-09-11-address-management/adr/` | All three real design decisions (plain FK vs. JPA relation, address id format, manual validation vs. Bean Validation) got their own ADR instead of being silently decided in code — exactly what `adr-authoring` asks for. | — |

## Rubric section results
> From `.claude/skills/code-review-rubric/SKILL.md`.

| Section | Result | Notes |
|---------|--------|-------|
| 1 Traceability | findings | F-001 — AC-012 tag fix exists but is uncommitted. Every other AC (14/15 in the committed diff, 15/15 once F-001's fix is committed) traces cleanly to a test. |
| 2 Layer boundaries | ok | `AddressController` only calls `AddressService` (constructor-injected). `AddressServiceImpl` calls `AddressRepository`/`UserRepository` directly — same pattern as `UserServiceImpl`. No repository injected into a controller, no reflection field writes. |
| 3 Spring idioms | ok (nits) | Constructor injection everywhere except `DataInitializer` (F-002, pre-existing pattern moved verbatim, user-approved in T-004's implementation log). `@Data` used on `Address` — no hand-written accessors. Redundant controller-level `@Transactional` noted (F-003) but matches existing `UserController` convention. |
| 4 Error handling | ok | `ResponseStatusException` throughout the request path (`404 user not found` / `404 address not found` / `400` for postal code and road address). 201 on create, 200 on get, 204 on update/delete. `DataInitializer`'s `RuntimeException` is startup-seeding-only (moved as-is from `UserApplication`), not a request-path concern. |
| 5 Data access | ok | New `AddressRepository` derived queries only (`findByIdAndUserId`, `findByUserId`, `deleteByUserId`) — no string-built JPQL. `User.attributes` untouched by this feature. Address table/columns (`postalCode`, `roadAddress`, `detailAddress`, `user_id`) are not SQL reserved words; H2 DDL for the new `Address` table succeeds (71/71 tests green, including the full integration suite that round-trips through it). |
| 6 Dates & serialisation | n/a | `Address` has no `LocalDateTime` field; `CustomLocalDateTimeSerializer` and `additionParameters` raw-JSON handling are untouched by this diff. |
| 7 Test quality | ok (1 finding) | All new `@Test`s carry `@Tag("AC-NNN")` + `@DisplayName("T-NNN: given …, when …, then …")` except the AC-012 gap (F-001, already fixed pending commit). `AddressIntegrationTest` cleans up every created user in a `finally` block. No `@Disabled`, no weakened assertions. |
| 8 Clarity | ok | No clever/obscure constructs found; see F-004 for a positive callout. |
| 9 Migration / contract | ok | `Address` is a brand-new table under H2 `ddl-auto=update` — no migration file needed, consistent with repo convention; documented in `01-spec.md`. `DELETE /user/{id}`'s response contract (204, no body) is unchanged — the cascade delete is an added side effect, not a breaking API change. |

## Summary
- must-fix: 0 · should-fix: 1 (F-001, already fixed locally — needs to be part of the commit) · nit: 2 (F-002, F-003) · praise: 2 (F-004, F-005)
- Waivers: none

## Next action
- PASS/WARN → suggested commit message (stage `AddressIntegrationTest.java`'s `@Tag("AC-012")` addition together with this, or as its own small commit, before/with `07-validation-report.md` and `07a-traceability.md`):
  ```
  test(address): tag AC-012 on the no-address-limit integration test

  traceability.sh only matches @Tag, not @DisplayName text, so the
  existing no-limit assertion (AC-012) wasn't registering as covered.

  Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
  ```
  User runs `git commit` — the agent never commits.
- Verdict: **Approve**.
