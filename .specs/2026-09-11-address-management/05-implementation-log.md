# Implementation Log: 2026-09-11-address-management

> Owner: `test-engineer` (red) + `implementer` (green/refactor/simplify) · Phase 4
> One block appended per phase per task. Newest at the bottom.

---

### T-001 — red
- when: 2026-09-11T03:27:25Z
- test: `com.keycloak.userstorage.AddressIntegrationTest.createAddress_existingUser_returns201WithAddressFields` — `@Tag("AC-003")`
- test: `com.keycloak.userstorage.AddressIntegrationTest.createAddress_sameUserTwice_bothSucceed` — `@Tag("AC-001")` (also covers AC-012)
- command: `./gradlew test --tests 'com.keycloak.userstorage.AddressIntegrationTest.createAddress_existingUser_returns201WithAddressFields'`
- command: `./gradlew test --tests 'com.keycloak.userstorage.AddressIntegrationTest.createAddress_sameUserTwice_bothSucceed'`
- result: FAIL (expected) — both tests fail with 404 NOT_FOUND because `POST /user/{userId}/addresses` does not exist yet (no `Address` model/controller/service/repository).
- excerpt:
  ```
  Gradle Test Executor 1 > AddressIntegrationTest > T-001: given an existing user,
  when POST /user/{userId}/addresses is submitted, then the system creates the
  address and returns 201 with its fields FAILED
      org.opentest4j.AssertionFailedError: expected: <201 CREATED> but was: <404 NOT_FOUND>
          at app//org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
          at app//org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
          at app//org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
          at app//org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
          at app//org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:177)
          at app//org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1145)
          at app//com.keycloak.userstorage.AddressIntegrationTest.createAddress_existingUser_returns201WithAddressFields(AddressIntegrationTest.java:52)
  1 test completed, 1 failed
  ```

### T-001 — green
- when: 2026-09-11T12:29:00+09:00
- files changed:
  - `src/main/java/com/keycloak/userstorage/model/Address.java` (new) — `@Data @Entity`, `id`(no `@GeneratedValue`), `userId` (`@Column(name = "user_id", nullable = false)`), `postalCode`, `roadAddress`, `detailAddress`.
  - `src/main/java/com/keycloak/userstorage/repository/AddressRepository.java` (new) — `interface AddressRepository extends JpaRepository<Address, String>` (no extra query methods; T-001 only needs `save()`).
  - `src/main/java/com/keycloak/userstorage/service/AddressService.java` (new) — `Address createAddress(String userId, Address address)`.
  - `src/main/java/com/keycloak/userstorage/service/AddressServiceImpl.java` (new) — constructor-injects `AddressRepository` + `UserRepository`; verifies user exists (`ResponseStatusException(NOT_FOUND, "user not found")` per `03-design.md` error model), sets `userId`, generates id `"addr-" + UUID...substring(0, 8)` (ADR-002), saves.
  - `src/main/java/com/keycloak/userstorage/controller/AddressController.java` (new) — `@RestController @RequestMapping("/user/{userId}/addresses")`, `POST` → `201 CREATED` with saved `Address` body.
- command: `./gradlew test --tests 'com.keycloak.userstorage.AddressIntegrationTest'`
- result: PASS — `createAddress_existingUser_returns201WithAddressFields` PASSED, `createAddress_sameUserTwice_bothSucceed` PASSED (2/2).
- command: `./gradlew test`
- result: PASS — full suite green, no regressions. `AddressIntegrationTest`: 2 tests, 0 failures, 0 errors. `UserStorageIntegrationTest`: 39 tests, 0 failures, 0 errors (matches `.specs/_baseline.json` gates.unit exactly: 39/0/0/0).

### T-001 — refactor
- when: 2026-09-11T12:31:00+09:00
- change: none — reviewed all four new files for duplication/misplaced logic; the slice is already minimal (single responsibility per class, id-generation lives in the service alongside the analogous `UserServiceImpl.createUser()` pattern it mirrors). No structural change made.
- command: `./gradlew test`
- result: PASS — unchanged (41/41).

### T-001 — simplify
- when: 2026-09-11T12:32:00+09:00
- change: none — code already uses early return (`existsById` guard before mutation), no ternaries, no dead options, names (`Address`, `postalCode`, `roadAddress`, `detailAddress`, `userId`) match `01-spec.md` glossary terms directly. No edits required.
- command: `./gradlew test`
- result: PASS — unchanged (41/41).
- task status: `done`.

### T-002 — red
- when: 2026-09-11T04:20:15Z
- test: `com.keycloak.userstorage.AddressIntegrationTest.createAddress_nonexistentUser_returns404` — `@Tag("AC-004")`
- test: `com.keycloak.userstorage.AddressIntegrationTest.createAddress_invalidPostalCode_returns400` — `@Tag("AC-005")`
- test: `com.keycloak.userstorage.AddressIntegrationTest.createAddress_blankRoadAddress_returns400` — `@Tag("AC-013")`
- command: `./gradlew test --tests 'com.keycloak.userstorage.AddressIntegrationTest'`
- result: MIXED (expected) — 5 tests run (2 pre-existing T-001 tests + 3 new), 2 failed.
  - `createAddress_nonexistentUser_returns404` (AC-004) PASSED immediately — `AddressServiceImpl.createAddress()` already checks `userRepository.existsById(userId)` and throws 404 `"user not found"` from T-001's implementation. This is existing behaviour, not new work for T-002; test is kept to lock it in with its proper `@Tag`, per instructions not to force an artificial failure.
  - `createAddress_invalidPostalCode_returns400` (AC-005) FAILED for the right reason — no postal-code validation exists yet.
  - `createAddress_blankRoadAddress_returns400` (AC-013) FAILED for the right reason — no road-address validation exists yet.
- excerpt:
  ```
  T-002: given an existing user, when POST /user/{userId}/addresses is submitted with a postal code
  that is not exactly 5 digits, then the system responds with 400 and does not create an address FAILED
      org.opentest4j.AssertionFailedError: expected: <400 BAD_REQUEST> but was: <201 CREATED>
          at app//com.keycloak.userstorage.AddressIntegrationTest.createAddress_invalidPostalCode_returns400(AddressIntegrationTest.java:128)

  T-002: given an existing user, when POST /user/{userId}/addresses is submitted with a blank road
  address, then the system responds with 400 and does not create an address FAILED
      org.opentest4j.AssertionFailedError: expected: <400 BAD_REQUEST> but was: <201 CREATED>
          at app//com.keycloak.userstorage.AddressIntegrationTest.createAddress_blankRoadAddress_returns400(AddressIntegrationTest.java:146)

  T-002: given a userId that does not exist, when POST /user/{userId}/addresses is submitted,
  then the system responds with 404 and does not create an address PASSED (pre-existing behaviour from T-001)
  ```

### T-002 — green
- when: 2026-09-11T13:20:00+09:00
- files changed:
  - `src/main/java/com/keycloak/userstorage/service/AddressServiceImpl.java` — added two `if` checks in `createAddress()`, after the existing user-existence check and before `setUserId`/save: postal code must match `\d{5}` (else `ResponseStatusException(BAD_REQUEST, "postal code must be exactly 5 digits")`), road address must not be `null`/blank (else `ResponseStatusException(BAD_REQUEST, "road address must not be blank")`). Manual validation per ADR-003 — no Bean Validation annotations, no new dependency.
- command: `./gradlew test --tests 'com.keycloak.userstorage.AddressIntegrationTest'`
- result: PASS — 5/5 (`createAddress_existingUser_returns201WithAddressFields`, `createAddress_sameUserTwice_bothSucceed`, `createAddress_nonexistentUser_returns404`, `createAddress_invalidPostalCode_returns400`, `createAddress_blankRoadAddress_returns400`), 0 failures, 0 errors.
- command: `./gradlew test`
- result: PASS — full suite green, no regressions. `AddressIntegrationTest`: 5 tests, 0 failures, 0 errors. `UserStorageIntegrationTest`: 39 tests, 0 failures, 0 errors. Total 44/44.

### T-002 — refactor
- when: 2026-09-11T13:22:00+09:00
- change: extracted the two validation `if` blocks out of `createAddress()` into a new private `validate(Address address)` method, called right after the user-existence check. Matches `04-tasks.md` T-002 notes ("수동 `validate(Address)` 추가") and anticipates T-005 (PUT), whose notes say it will reuse this same `validate()`. No behaviour change — same checks, same order, same messages.
- command: `./gradlew test`
- result: PASS — unchanged (44/44).

### T-002 — simplify
- when: 2026-09-11T13:23:00+09:00
- change: none — `createAddress()` already reads top-to-bottom with early-return guard clauses (existence check → validate → assign → save), `validate()` has no nesting/ternaries, and names (`postalCode`, `roadAddress`, `validate`) match `01-spec.md`/`04-tasks.md` glossary terms directly. No edits required.
- command: `./gradlew test`
- result: PASS — unchanged (44/44).
- task status: `done`.

### T-003 — red
- when: 2026-09-11T14:06:32Z
- test: `com.keycloak.userstorage.AddressIntegrationTest.getAddress_existingAddressOfOwningUser_returns200WithAddressFields` — `@Tag("AC-006")`
- test: `com.keycloak.userstorage.AddressIntegrationTest.getAddress_nonexistentAddressId_returns404` — `@Tag("AC-010")`
- test: `com.keycloak.userstorage.AddressIntegrationTest.getAddress_addressBelongsToDifferentUser_returns404` — `@Tag("AC-014")`
- command: `./gradlew test --tests 'com.keycloak.userstorage.AddressIntegrationTest'`
- result: FAIL (expected) — 8 tests run (5 pre-existing T-001/T-002 tests + 3 new), 3 failed. No GET mapping exists on `AddressController` yet, so `GET /user/{userId}/addresses/{addressId}` falls through to Spring's default 404 handler regardless of scenario.
- excerpt:
  ```
  getAddress_existingAddressOfOwningUser_returns200WithAddressFields (AC-006) FAILED
      org.opentest4j.AssertionFailedError: expected: <200 OK> but was: <404 NOT_FOUND>
          at app//com.keycloak.userstorage.AddressIntegrationTest.getAddress_existingAddressOfOwningUser_returns200WithAddressFields(AddressIntegrationTest.java:169)

  getAddress_nonexistentAddressId_returns404 (AC-010) FAILED
      org.opentest4j.AssertionFailedError: expected: <address not found> but was: <Not Found>
          at app//com.keycloak.userstorage.AddressIntegrationTest.getAddress_nonexistentAddressId_returns404(AddressIntegrationTest.java:194)

  getAddress_addressBelongsToDifferentUser_returns404 (AC-014) FAILED
      org.opentest4j.AssertionFailedError: expected: <address not found> but was: <Not Found>
          at app//com.keycloak.userstorage.AddressIntegrationTest.getAddress_addressBelongsToDifferentUser_returns404(AddressIntegrationTest.java:216)

  8 tests completed, 3 failed (5 pre-existing T-001/T-002 tests unaffected)
  ```

### T-003 — green
- when: 2026-09-11T14:10:00+09:00
- files changed:
  - `src/main/java/com/keycloak/userstorage/repository/AddressRepository.java` — added `Optional<Address> findByIdAndUserId(String id, String userId)` derived query; empty result covers both "addressId doesn't exist" and "addressId belongs to a different user" in one lookup.
  - `src/main/java/com/keycloak/userstorage/service/AddressService.java` — added `Address getAddress(String userId, String addressId)` to the interface.
  - `src/main/java/com/keycloak/userstorage/service/AddressServiceImpl.java` — implemented `getAddress()`: `addressRepository.findByIdAndUserId(addressId, userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "address not found"))`. No `userRepository.existsById` check added (out of scope for T-003's ACs; deferred to whichever task covers "user not found" for GET, e.g. AC-015/T-004 for the list endpoint).
  - `src/main/java/com/keycloak/userstorage/controller/AddressController.java` — added `@GetMapping("/{addressId}") getAddress(@PathVariable String userId, @PathVariable String addressId)` returning `ResponseEntity.ok(address)`.
- command: `./gradlew test --tests 'com.keycloak.userstorage.AddressIntegrationTest'`
- result: PASS — 8/8 (5 pre-existing T-001/T-002 tests + 3 new: `getAddress_existingAddressOfOwningUser_returns200WithAddressFields`, `getAddress_nonexistentAddressId_returns404`, `getAddress_addressBelongsToDifferentUser_returns404`), 0 failures, 0 errors.
- command: `./gradlew test`
- result: PASS — full suite green, no regressions. `AddressIntegrationTest`: 8 tests, 0 failures, 0 errors. `UserStorageIntegrationTest`: 39 tests, 0 failures, 0 errors. Total 47/47.

### T-003 — refactor
- when: 2026-09-11T14:11:00+09:00
- change: none — reviewed all four touched files; `getAddress()` is a single-expression method mirroring the existing `orElseThrow` idiom used elsewhere in the codebase, the repository method is a plain derived query, and the controller method mirrors `createAddress()`'s structure. No duplication or misplaced logic found.
- command: `./gradlew test`
- result: PASS — unchanged (47/47).

### T-003 — simplify
- when: 2026-09-11T14:12:00+09:00
- change: none — `getAddress()` already reads as a single early-return-style expression, no ternaries or nested conditionals, and names (`addressId`, `userId`, `findByIdAndUserId`) match `01-spec.md`/`03-design.md` glossary terms directly. No edits required.
- command: `./gradlew test`
- result: PASS — unchanged (47/47).
- task status: `done`.
