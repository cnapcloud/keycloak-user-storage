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
