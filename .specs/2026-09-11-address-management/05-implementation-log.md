# Implementation Log: 2026-09-11-address-management

> Owner: `test-engineer` (red) + `implementer` (green/refactor/simplify) · Phase 4
> One block appended per phase per task. Newest at the bottom.

---

### T-001 — red
- when: 2026-09-11T03:27:25Z
- test: `com.keycloak.userstorage.integration.AddressIntegrationTest.createAddress_existingUser_returns201WithAddressFields` — `@Tag("AC-003")`
- test: `com.keycloak.userstorage.integration.AddressIntegrationTest.createAddress_sameUserTwice_bothSucceed` — `@Tag("AC-001")` (also covers AC-012)
- command: `./gradlew test --tests 'com.keycloak.userstorage.integration.AddressIntegrationTest.createAddress_existingUser_returns201WithAddressFields'`
- command: `./gradlew test --tests 'com.keycloak.userstorage.integration.AddressIntegrationTest.createAddress_sameUserTwice_bothSucceed'`
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
          at app//com.keycloak.userstorage.integration.AddressIntegrationTest.createAddress_existingUser_returns201WithAddressFields(AddressIntegrationTest.java:52)
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
- command: `./gradlew test --tests 'com.keycloak.userstorage.integration.AddressIntegrationTest'`
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
- test: `com.keycloak.userstorage.integration.AddressIntegrationTest.createAddress_nonexistentUser_returns404` — `@Tag("AC-004")`
- test: `com.keycloak.userstorage.integration.AddressIntegrationTest.createAddress_invalidPostalCode_returns400` — `@Tag("AC-005")`
- test: `com.keycloak.userstorage.integration.AddressIntegrationTest.createAddress_blankRoadAddress_returns400` — `@Tag("AC-013")`
- command: `./gradlew test --tests 'com.keycloak.userstorage.integration.AddressIntegrationTest'`
- result: MIXED (expected) — 5 tests run (2 pre-existing T-001 tests + 3 new), 2 failed.
  - `createAddress_nonexistentUser_returns404` (AC-004) PASSED immediately — `AddressServiceImpl.createAddress()` already checks `userRepository.existsById(userId)` and throws 404 `"user not found"` from T-001's implementation. This is existing behaviour, not new work for T-002; test is kept to lock it in with its proper `@Tag`, per instructions not to force an artificial failure.
  - `createAddress_invalidPostalCode_returns400` (AC-005) FAILED for the right reason — no postal-code validation exists yet.
  - `createAddress_blankRoadAddress_returns400` (AC-013) FAILED for the right reason — no road-address validation exists yet.
- excerpt:
  ```
  T-002: given an existing user, when POST /user/{userId}/addresses is submitted with a postal code
  that is not exactly 5 digits, then the system responds with 400 and does not create an address FAILED
      org.opentest4j.AssertionFailedError: expected: <400 BAD_REQUEST> but was: <201 CREATED>
          at app//com.keycloak.userstorage.integration.AddressIntegrationTest.createAddress_invalidPostalCode_returns400(AddressIntegrationTest.java:128)

  T-002: given an existing user, when POST /user/{userId}/addresses is submitted with a blank road
  address, then the system responds with 400 and does not create an address FAILED
      org.opentest4j.AssertionFailedError: expected: <400 BAD_REQUEST> but was: <201 CREATED>
          at app//com.keycloak.userstorage.integration.AddressIntegrationTest.createAddress_blankRoadAddress_returns400(AddressIntegrationTest.java:146)

  T-002: given a userId that does not exist, when POST /user/{userId}/addresses is submitted,
  then the system responds with 404 and does not create an address PASSED (pre-existing behaviour from T-001)
  ```

### T-002 — green
- when: 2026-09-11T13:20:00+09:00
- files changed:
  - `src/main/java/com/keycloak/userstorage/service/AddressServiceImpl.java` — added two `if` checks in `createAddress()`, after the existing user-existence check and before `setUserId`/save: postal code must match `\d{5}` (else `ResponseStatusException(BAD_REQUEST, "postal code must be exactly 5 digits")`), road address must not be `null`/blank (else `ResponseStatusException(BAD_REQUEST, "road address must not be blank")`). Manual validation per ADR-003 — no Bean Validation annotations, no new dependency.
- command: `./gradlew test --tests 'com.keycloak.userstorage.integration.AddressIntegrationTest'`
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
- test: `com.keycloak.userstorage.integration.AddressIntegrationTest.getAddress_existingAddressOfOwningUser_returns200WithAddressFields` — `@Tag("AC-006")`
- test: `com.keycloak.userstorage.integration.AddressIntegrationTest.getAddress_nonexistentAddressId_returns404` — `@Tag("AC-010")`
- test: `com.keycloak.userstorage.integration.AddressIntegrationTest.getAddress_addressBelongsToDifferentUser_returns404` — `@Tag("AC-014")`
- command: `./gradlew test --tests 'com.keycloak.userstorage.integration.AddressIntegrationTest'`
- result: FAIL (expected) — 8 tests run (5 pre-existing T-001/T-002 tests + 3 new), 3 failed. No GET mapping exists on `AddressController` yet, so `GET /user/{userId}/addresses/{addressId}` falls through to Spring's default 404 handler regardless of scenario.
- excerpt:
  ```
  getAddress_existingAddressOfOwningUser_returns200WithAddressFields (AC-006) FAILED
      org.opentest4j.AssertionFailedError: expected: <200 OK> but was: <404 NOT_FOUND>
          at app//com.keycloak.userstorage.integration.AddressIntegrationTest.getAddress_existingAddressOfOwningUser_returns200WithAddressFields(AddressIntegrationTest.java:169)

  getAddress_nonexistentAddressId_returns404 (AC-010) FAILED
      org.opentest4j.AssertionFailedError: expected: <address not found> but was: <Not Found>
          at app//com.keycloak.userstorage.integration.AddressIntegrationTest.getAddress_nonexistentAddressId_returns404(AddressIntegrationTest.java:194)

  getAddress_addressBelongsToDifferentUser_returns404 (AC-014) FAILED
      org.opentest4j.AssertionFailedError: expected: <address not found> but was: <Not Found>
          at app//com.keycloak.userstorage.integration.AddressIntegrationTest.getAddress_addressBelongsToDifferentUser_returns404(AddressIntegrationTest.java:216)

  8 tests completed, 3 failed (5 pre-existing T-001/T-002 tests unaffected)
  ```

### T-003 — green
- when: 2026-09-11T14:10:00+09:00
- files changed:
  - `src/main/java/com/keycloak/userstorage/repository/AddressRepository.java` — added `Optional<Address> findByIdAndUserId(String id, String userId)` derived query; empty result covers both "addressId doesn't exist" and "addressId belongs to a different user" in one lookup.
  - `src/main/java/com/keycloak/userstorage/service/AddressService.java` — added `Address getAddress(String userId, String addressId)` to the interface.
  - `src/main/java/com/keycloak/userstorage/service/AddressServiceImpl.java` — implemented `getAddress()`: `addressRepository.findByIdAndUserId(addressId, userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "address not found"))`. No `userRepository.existsById` check added (out of scope for T-003's ACs; deferred to whichever task covers "user not found" for GET, e.g. AC-015/T-004 for the list endpoint).
  - `src/main/java/com/keycloak/userstorage/controller/AddressController.java` — added `@GetMapping("/{addressId}") getAddress(@PathVariable String userId, @PathVariable String addressId)` returning `ResponseEntity.ok(address)`.
- command: `./gradlew test --tests 'com.keycloak.userstorage.integration.AddressIntegrationTest'`
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

### T-004 — red
- when: 2026-09-11T15:05:00Z
- test: `com.keycloak.userstorage.service.AddressServiceImplTest.getAddresses_existingUserWithAddresses_returnsAllAddresses` — `@Tag("AC-007")`
- test: `com.keycloak.userstorage.service.AddressServiceImplTest.getAddresses_existingUserWithNoAddresses_returnsEmptyList` — `@Tag("AC-007")`
- test: `com.keycloak.userstorage.service.AddressServiceImplTest.getAddresses_nonexistentUser_throws404` — `@Tag("AC-015")`
- test: `com.keycloak.userstorage.controller.AddressControllerTest.getAddresses_serviceReturnsAddresses_returns200WithList` — `@Tag("AC-007")`
- test: `com.keycloak.userstorage.controller.AddressControllerTest.getAddresses_serviceReturnsEmptyList_returns200WithEmptyArray` — `@Tag("AC-007")`
- test: `com.keycloak.userstorage.controller.AddressControllerTest.getAddresses_serviceThrows404_returns404` — `@Tag("AC-015")`
- first task on this feature using the `spring-unit-testing` layer-isolated convention (both files are new; no prior `AddressServiceImplTest`/`AddressControllerTest`).
- command: `./gradlew test --tests 'com.keycloak.userstorage.service.AddressServiceImplTest'`
- command: `./gradlew test --tests 'com.keycloak.userstorage.controller.AddressControllerTest'`
- result: FAIL (expected) — `:compileTestJava` fails, not `:test`. Per `04-tasks.md` T-004 notes, `AddressService.getAddresses(String)` and `AddressRepository.findByUserId(String)` genuinely do not exist in `src/main` yet — the test-engineer role never edits `src/main/**`, so red for a brand-new interface method is necessarily a compile-time symbol-not-found, not a runtime assertion failure. Every one of the 8 compiler errors resolves to exactly these two missing symbols (no typos, no unrelated errors); `implementer` adding both methods in green is what turns this into a runtime green. Because `compileTestJava` compiles the whole `src/test/java` source set together regardless of the `--tests` filter, both Gradle invocations produced the identical 8-error listing.
- excerpt:
  ```
  > Task :compileTestJava FAILED
  .../controller/AddressControllerTest.java:55: error: cannot find symbol
          when(addressService.getAddresses(EXISTING_USER_ID)).thenReturn(List.of(first, second));
                             ^
    symbol:   method getAddresses(String)
    location: variable addressService of type AddressService
  .../service/AddressServiceImplTest.java:58: error: cannot find symbol
          when(addressRepository.findByUserId(EXISTING_USER_ID)).thenReturn(List.of(first, second));
                                ^
    symbol:   method findByUserId(String)
    location: variable addressRepository of type AddressRepository
  .../service/AddressServiceImplTest.java:60: error: cannot find symbol
          List<Address> result = addressService.getAddresses(EXISTING_USER_ID);
                                               ^
    symbol:   method getAddresses(String)
    location: variable addressService of type AddressServiceImpl
  8 errors total (3 in AddressControllerTest, 5 in AddressServiceImplTest), all resolving to the
  same two missing symbols: AddressService.getAddresses(String), AddressRepository.findByUserId(String).

  FAILURE: Build failed with an exception.
  > Task :compileTestJava FAILED
  ```

### T-004 — green
- when: 2026-09-11T16:53:00+09:00
- files changed:
  - `src/main/java/com/keycloak/userstorage/repository/AddressRepository.java` — added `List<Address> findByUserId(String userId)` derived query.
  - `src/main/java/com/keycloak/userstorage/service/AddressService.java` — added `List<Address> getAddresses(String userId)` to the interface.
  - `src/main/java/com/keycloak/userstorage/service/AddressServiceImpl.java` — implemented `getAddresses()`: checks `userRepository.existsById(userId)` (throws `ResponseStatusException(NOT_FOUND, "user not found")` for AC-015, mirroring `createAddress()`'s existing check), then `addressRepository.findByUserId(userId)`.
  - `src/main/java/com/keycloak/userstorage/controller/AddressController.java` — added `@GetMapping getAddresses(@PathVariable String userId)` returning `ResponseEntity.ok(addresses)`.
- mid-batch infra fix (not part of the original red batch, user-approved before this block): `AddressControllerTest` is the first `@WebMvcTest` slice test in this repo. Its `ApplicationContext` failed to start because `UserApplication` directly `@Autowired`s `UserRepository`/`CredentialDataRepository` for its `@PostConstruct initData()` seeder — a `@WebMvcTest` slice excludes JPA autoconfiguration, so those beans can't be satisfied and the slice context refuses to start regardless of what `AddressControllerTest` itself asserts. Fixed by extracting the seeder into its own `@Component`:
  - `src/main/java/com/keycloak/userstorage/DataInitializer.java` (new) — `@Component` holding the two `@Autowired` repository fields and the `@PostConstruct initData()` method, moved verbatim (byte-for-byte body) from `UserApplication`.
  - `src/main/java/com/keycloak/userstorage/UserApplication.java` — stripped to a bare `@SpringBootApplication` class with only `main()`; removed the two `@Autowired` fields, `initData()`, and now-unused imports (`java.io.*`, `java.time.LocalDateTime`, `java.util.List`, `Autowired`, Jackson types, `CredentialData`/`User`/`JacksonConfig`, both repository imports, `PostConstruct`).
  - No runtime behaviour change: `DataInitializer` is a normal `@Component`, so `@PostConstruct` still fires during full `SpringApplication` context startup exactly as before (full-suite `UserStorageIntegrationTest`, which depends on the seeded data, stays green — see full-suite result below). `@WebMvcTest` slices, being narrower, correctly never load `DataInitializer`, which is exactly why the split unblocks `AddressControllerTest`.
- command: `./gradlew test --tests 'com.keycloak.userstorage.controller.AddressControllerTest' --tests 'com.keycloak.userstorage.service.AddressServiceImplTest'`
- result: PASS — 6/6 (`AddressControllerTest`: `getAddresses_serviceReturnsAddresses_returns200WithList`, `getAddresses_serviceReturnsEmptyList_returns200WithEmptyArray`, `getAddresses_serviceThrows404_returns404`; `AddressServiceImplTest`: `getAddresses_existingUserWithAddresses_returnsAllAddresses`, `getAddresses_existingUserWithNoAddresses_returnsEmptyList`, `getAddresses_nonexistentUser_throws404`), 0 failures, 0 errors.
- command: `./gradlew test`
- result: PASS — full suite green, no regressions. `AddressControllerTest`: 3/0/0/0. `AddressServiceImplTest`: 3/0/0/0. `AddressIntegrationTest`: 8/0/0/0 (unchanged). `UserStorageIntegrationTest`: 39/0/0/0 (unchanged — confirms `DataInitializer` extraction preserved seeding behaviour under full context startup). Total 53/53, 0 failures, 0 errors.

### T-004 — refactor
- when: 2026-09-11T16:55:10+09:00
- change: `src/main/java/com/keycloak/userstorage/service/AddressServiceImpl.java` — `createAddress()` and `getAddresses()` each had an identical `if (!userRepository.existsById(userId)) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"); }` guard. Extracted into a private `requireUserExists(String userId)` method called at the top of both. No behaviour change — same check, same exception, same message, same call order relative to the rest of each method.
- command: `./gradlew test`
- result: PASS — unchanged (53/53, 0 failures, 0 errors).

### T-004 — simplify
- when: 2026-09-11T16:55:40+09:00
- change: none — reviewed all touched files (`AddressRepository`, `AddressService`, `AddressServiceImpl`, `AddressController`, `DataInitializer`, `UserApplication`). No ternaries, no nesting beyond a single guard clause, no dead options; names (`getAddresses`, `findByUserId`, `requireUserExists`, `DataInitializer`) match `01-spec.md`/`04-tasks.md` glossary terms directly. No edits required.
- command: `./gradlew test`
- result: PASS — unchanged (53/53, 0 failures, 0 errors).
- task status: `done`.

### T-005 — red
- when: 2026-09-11T17:40:00Z
- test: `com.keycloak.userstorage.service.AddressServiceImplTest.updateAddress_existingAddressOfOwningUser_replacesAllFieldsAndSaves` — `@Tag("AC-008")`
- test: `com.keycloak.userstorage.service.AddressServiceImplTest.updateAddress_detailAddressOmitted_overwritesExistingDetailAddressWithNull` — `@Tag("AC-008")`
- test: `com.keycloak.userstorage.service.AddressServiceImplTest.updateAddress_invalidPostalCode_throws400AndDoesNotSave` — `@Tag("AC-005")`
- test: `com.keycloak.userstorage.service.AddressServiceImplTest.updateAddress_blankRoadAddress_throws400AndDoesNotSave` — `@Tag("AC-013")`
- test: `com.keycloak.userstorage.service.AddressServiceImplTest.updateAddress_nonexistentAddressId_throws404AndDoesNotSave` — `@Tag("AC-010")`
- test: `com.keycloak.userstorage.service.AddressServiceImplTest.updateAddress_addressBelongsToDifferentUser_throws404AndDoesNotSave` — `@Tag("AC-014")`
- test: `com.keycloak.userstorage.controller.AddressControllerTest.updateAddress_serviceAccepts_returns204` — `@Tag("AC-008")`
- test: `com.keycloak.userstorage.controller.AddressControllerTest.updateAddress_serviceThrowsBadRequestForPostalCode_returns400` — `@Tag("AC-005")`
- test: `com.keycloak.userstorage.controller.AddressControllerTest.updateAddress_serviceThrowsBadRequestForRoadAddress_returns400` — `@Tag("AC-013")`
- test: `com.keycloak.userstorage.controller.AddressControllerTest.updateAddress_serviceThrowsNotFoundForNonexistentAddressId_returns404` — `@Tag("AC-010")`
- test: `com.keycloak.userstorage.controller.AddressControllerTest.updateAddress_serviceThrowsNotFoundForDifferentOwner_returns404` — `@Tag("AC-014")`
- unit-test-by-default policy per `04-tasks.md` T-005 notes update (T-005's `files_in_scope` still lists `AddressIntegrationTest.java` from an earlier draft, but per the repo's now-default `spring-unit-testing` policy — see T-004's precedent — both new batches went into the existing `AddressServiceImplTest`/`AddressControllerTest` unit files instead; no integration test added or touched).
- command: `./gradlew test --tests 'com.keycloak.userstorage.service.AddressServiceImplTest'`
- command: `./gradlew test --tests 'com.keycloak.userstorage.controller.AddressControllerTest'`
- result: FAIL (expected) — both invocations fail identically at `:compileTestJava`, not `:test`. `AddressService.updateAddress(String, String, Address)` genuinely does not exist yet in `src/main` — the test-engineer role never edits `src/main/**`, so red for a brand-new interface method is necessarily a compile-time symbol-not-found, same pattern as T-004. All 11 compiler errors (6 in `AddressServiceImplTest`, 5 in `AddressControllerTest`) resolve to the single missing symbol `AddressService.updateAddress(String,String,Address)`; no typos, no unrelated errors. `implementer` adding the method (interface + impl + controller `@PutMapping`) in green turns this into a runtime green. Because `compileTestJava` compiles the whole `src/test/java` source set together regardless of the `--tests` filter, both Gradle invocations produced the identical 11-error listing.
- excerpt:
  ```
  > Task :compileTestJava FAILED
  .../controller/AddressControllerTest.java:111: error: cannot find symbol
          verify(addressService).updateAddress(eq(EXISTING_USER_ID), eq(EXISTING_ADDRESS_ID), any(Address.class));
                                ^
    symbol:   method updateAddress(String,String,Address)
    location: interface AddressService
  .../service/AddressServiceImplTest.java:125: error: cannot find symbol
          addressService.updateAddress(EXISTING_USER_ID, EXISTING_ADDRESS_ID, submitted);
                        ^
    symbol:   method updateAddress(String,String,Address)
    location: variable addressService of type AddressServiceImpl
  11 errors total (5 in AddressControllerTest, 6 in AddressServiceImplTest), all resolving to the
  same missing symbol: AddressService.updateAddress(String, String, Address).

  FAILURE: Build failed with an exception.
  > Task :compileTestJava FAILED
  ```

### T-005 — green
- when: 2026-09-11T17:15:00+09:00
- files changed:
  - `src/main/java/com/keycloak/userstorage/service/AddressService.java` — added `void updateAddress(String userId, String addressId, Address address);` to the interface.
  - `src/main/java/com/keycloak/userstorage/service/AddressServiceImpl.java` — implemented `updateAddress()`: `addressRepository.findByIdAndUserId(addressId, userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "address not found"))` (same lookup pattern as `getAddress()`, covers both AC-010 nonexistent id and AC-014 wrong owner as one code path), then the existing `validate(address)` helper (covers AC-005/AC-013), then full-replace of `postalCode`, `roadAddress`, `detailAddress` on the found entity (detail address included even when the submitted value is `null`, so omission overwrites rather than preserves — matches PUT full-replace semantics, not PATCH), then `addressRepository.save(existing)`. Annotated `@Transactional` like `createAddress()`.
  - `src/main/java/com/keycloak/userstorage/controller/AddressController.java` — added `@PutMapping("/{addressId}") @Transactional updateAddress(...)` calling the service method and returning `ResponseEntity.noContent().build()` (204, no body).
- command: `./gradlew test --tests 'com.keycloak.userstorage.service.AddressServiceImplTest' --tests 'com.keycloak.userstorage.controller.AddressControllerTest'`
- result: PASS — all tests in both classes green (gradle report totals below are the source of truth), 0 failures, 0 errors.
- command: `./gradlew test`
- result: PASS — full suite green, no regressions. `AddressServiceImplTest`: 9/0/0/0. `AddressControllerTest`: 8/0/0/0. `AddressIntegrationTest`: 8/0/0/0 (unchanged). `UserStorageIntegrationTest`: 39/0/0/0 (unchanged). Total 64/64, 0 failures, 0 errors.

### T-005 — refactor
- when: 2026-09-11T17:17:00+09:00
- change: none — `updateAddress()` reuses the existing `findByIdAndUserId` lookup pattern (identical to `getAddress()`) and the existing `validate()` helper (identical to `createAddress()`); no new duplication introduced. Controller method mirrors the structure of `createAddress()`/`getAddress()`. No extraction or restructuring needed.
- command: `./gradlew test`
- result: PASS — unchanged (64/64, 0 failures, 0 errors).

### T-005 — simplify
- when: 2026-09-11T17:18:00+09:00
- change: none — `updateAddress()` reads top-to-bottom as a single sequence of guard-then-mutate steps (lookup-or-404 → validate → assign three fields → save), no ternaries or nested conditionals, names (`updateAddress`, `postalCode`, `roadAddress`, `detailAddress`) match `01-spec.md`/`04-tasks.md` glossary terms directly. No edits required.
- command: `./gradlew test`
- result: PASS — unchanged (64/64, 0 failures, 0 errors).
- task status: `done`.

### T-006 — red
- when: 2026-09-11T18:05:00Z
- test: `com.keycloak.userstorage.service.AddressServiceImplTest.deleteAddress_existingAddressOfOwningUser_deletesRecord` — `@Tag("AC-009")`
- test: `com.keycloak.userstorage.service.AddressServiceImplTest.deleteAddress_nonexistentAddressId_throws404AndDoesNotDelete` — `@Tag("AC-010")`
- test: `com.keycloak.userstorage.service.AddressServiceImplTest.deleteAddress_addressBelongsToDifferentUser_throws404AndDoesNotDelete` — `@Tag("AC-014")`
- test: `com.keycloak.userstorage.controller.AddressControllerTest.deleteAddress_serviceAccepts_returns204` — `@Tag("AC-009")`
- test: `com.keycloak.userstorage.controller.AddressControllerTest.deleteAddress_serviceThrowsNotFoundForNonexistentAddressId_returns404` — `@Tag("AC-010")`
- test: `com.keycloak.userstorage.controller.AddressControllerTest.deleteAddress_serviceThrowsNotFoundForDifferentOwner_returns404` — `@Tag("AC-014")`
- unit-test-by-default policy per repo's now-default `spring-unit-testing` policy (T-004/T-005 precedent) — T-006's `files_in_scope` in `04-tasks.md` still lists `AddressIntegrationTest.java` from an earlier draft, but both new batches went into the existing `AddressServiceImplTest`/`AddressControllerTest` unit files instead; no integration test added or touched. `files_in_scope` corrected in `.tdd-state.json` to reflect the two unit test files actually touched.
- command: `./gradlew test --tests 'com.keycloak.userstorage.service.AddressServiceImplTest'`
- command: `./gradlew test --tests 'com.keycloak.userstorage.controller.AddressControllerTest'`
- result: FAIL (expected) — both invocations fail identically at `:compileTestJava`, not `:test`. `AddressService.deleteAddress(String, String)` genuinely does not exist yet in `src/main` — the test-engineer role never edits `src/main/**`, so red for a brand-new interface method is necessarily a compile-time symbol-not-found, same pattern as T-004/T-005. All 6 compiler errors (3 in `AddressServiceImplTest`, 3 in `AddressControllerTest`) resolve to the single missing symbol `AddressService.deleteAddress(String,String)`; no typos, no unrelated errors. `implementer` adding the method (interface + impl reusing `findByIdAndUserId` + `addressRepository.deleteById` + controller `@DeleteMapping`) in green turns this into a runtime green. Because `compileTestJava` compiles the whole `src/test/java` source set together regardless of the `--tests` filter, both Gradle invocations produced the identical 6-error listing.
- excerpt:
  ```
  > Task :compileTestJava FAILED
  .../controller/AddressControllerTest.java:193: error: cannot find symbol
          verify(addressService).deleteAddress(eq(EXISTING_USER_ID), eq(EXISTING_ADDRESS_ID));
                                ^
    symbol:   method deleteAddress(String,String)
    location: interface AddressService
  .../service/AddressServiceImplTest.java:270: error: cannot find symbol
          addressService.deleteAddress(EXISTING_USER_ID, EXISTING_ADDRESS_ID);
                        ^
    symbol:   method deleteAddress(String,String)
    location: variable addressService of type AddressServiceImpl
  6 errors total (3 in AddressControllerTest, 3 in AddressServiceImplTest), all resolving to the
  same missing symbol: AddressService.deleteAddress(String, String).

  FAILURE: Build failed with an exception.
  > Task :compileTestJava FAILED
  ```

### T-006 — green
- when: 2026-09-11T18:24:00+09:00
- files changed:
  - `src/main/java/com/keycloak/userstorage/service/AddressService.java` — added `void deleteAddress(String userId, String addressId);` to the interface.
  - `src/main/java/com/keycloak/userstorage/service/AddressServiceImpl.java` — implemented `deleteAddress()`: `addressRepository.findByIdAndUserId(addressId, userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "address not found"))` (same lookup pattern as `getAddress()`/`updateAddress()`, covers both AC-010 nonexistent id and AC-014 wrong owner as one code path), then `addressRepository.deleteById(existing.getId())`. Annotated `@Transactional` like `createAddress()`/`updateAddress()`.
  - `src/main/java/com/keycloak/userstorage/controller/AddressController.java` — added `@DeleteMapping("/{addressId}") @Transactional deleteAddress(...)` calling the service method and returning `ResponseEntity.noContent().build()` (204, no body).
- command: `./gradlew test --tests 'com.keycloak.userstorage.service.AddressServiceImplTest' --tests 'com.keycloak.userstorage.controller.AddressControllerTest'`
- result: PASS — all tests in both classes green, including all 6 new T-006 tests (`AddressServiceImplTest.deleteAddress_existingAddressOfOwningUser_deletesRecord`, `deleteAddress_nonexistentAddressId_throws404AndDoesNotDelete`, `deleteAddress_addressBelongsToDifferentUser_throws404AndDoesNotDelete`; `AddressControllerTest.deleteAddress_serviceAccepts_returns204`, `deleteAddress_serviceThrowsNotFoundForNonexistentAddressId_returns404`, `deleteAddress_serviceThrowsNotFoundForDifferentOwner_returns404`). `AddressServiceImplTest`: 11/0/0/0. `AddressControllerTest`: 11/0/0/0. 0 failures, 0 errors.
- command: `./gradlew test`
- result: **FAIL — pre-existing regression unrelated to T-006, not fixed (out of `files_in_scope`)**. 70 tests completed, 14 failed. Root cause: `src/main/java/com/keycloak/userstorage/service/UserServiceImpl.java:95` — the duplicate-username guard in `createUser()` reads `if (userRepository.findByUsername(user.getUsername()).isEmpty()) { throw new ResponseStatusException(HttpStatus.CONFLICT, "user already exists"); }`. This is inverted: it throws 409 when the username does **not** already exist (i.e. on every legitimate new-user creation) and instead allows the save to proceed when the username **does** already exist (which then hits the DB unique constraint and surfaces as a 500). This line is untouched by T-006 and sits in a file outside T-006's `files_in_scope` (`AddressService.java`, `AddressServiceImpl.java`, `AddressController.java`, `AddressServiceImplTest.java`, `AddressControllerTest.java` only) — editing `UserServiceImpl.java` here would be out of scope and hook-blocked.
  - Directly caused (3): `UserStorageIntegrationTest.createUser_success_returns201WithIdUsernameEmail` (expected 201, got 409), `createUser_attributesEmpty_returns201` (expected 201, got 409), `createUser_duplicateUsername_returns409` (expected 409, got 500 — the inverted branch lets the actual duplicate through to the DB, which throws a constraint violation instead of the intended controlled 409).
  - Cascaded from the above within the same `@SpringBootTest` class, whose later tests depend on users successfully created by earlier ones or by shared setup (5): `deleteUser_success_returns204AndUserGone` (404 instead of 204 — the user it expected to delete was never created), `patchMultiAttributes_multipleValues_storedAsIndividualEntries` (404), `patchMultiAttributes_rePatch_replacesOnlyTargetKey`, `patchMultiAttributes_emptyListRemovesKey`, `patchMultiAttributes_singleValue_storedCorrectly`, `patchMultiAttributes_doesNotAffectSingleValueAttributes` (NullPointerException on null `attrs`/`multiAttrs` — same missing-setup-user cause).
  - Cascaded into `AddressIntegrationTest` (5): `POST /user/{userId}/addresses` (happy path, x1:N, both validation-failure cases) and `GET /user/{userId}/addresses/{addressId}` (happy path) all fail with 404 instead of their expected status, because `AddressIntegrationTest`'s setup helper (line 236: `rest.exchange("/user", HttpMethod.POST, ...)`) itself calls the now-broken `POST /user` to provision its test user; that call now returns 409 instead of 201, so the user the address tests expect to exist is never created.
  - This is a single-root-cause regression (one inverted boolean on one line) that fans out to 14 test failures across two integration classes purely through shared setup dependency, not 14 independent bugs.
- Per the process's hard rule ("if a test that wasn't added by this task fails after the change, that's a regression — fix the implementation, never the old test") and the explicit scope boundary (`files_in_scope` excludes `UserServiceImpl.java`), this fix is not made here. `T-006`'s own 6 new tests are green; `.tdd-state.json` `T-006.phase` is set to `green`, not advanced past that (refactor/simplify below apply only to the T-006-owned files) — **task is not marked `done`** and is not eligible for `/validate` until this regression is resolved by the user's decision (see final report).
- **Update (2026-09-11, post drill)**: the coordinator confirmed the `UserServiceImpl.createUser` regression above was a deliberate drill (commit `42a76a4`), since reverted via `git revert` (commit `8775d13`); `UserServiceImpl.java:95` is back to `.isPresent()`. Re-ran `./gradlew test` after the revert: **PASS — 70/70, 0 failures, 0 errors** (`AddressServiceImplTest` 12/0/0/0, `AddressControllerTest` 11/0/0/0, `AddressIntegrationTest` 8/0/0/0, `UserStorageIntegrationTest` 39/0/0/0). No regression remains; see updated task status in the simplify block below.

### T-006 — refactor
- when: 2026-09-11T18:30:00+09:00
- change: `src/main/java/com/keycloak/userstorage/service/AddressServiceImpl.java` — `getAddress()`, `updateAddress()`, and `deleteAddress()` each had an identical `addressRepository.findByIdAndUserId(addressId, userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "address not found"))` lookup (the third occurrence introduced by this task's `deleteAddress()`). Extracted into a private `Address requireOwnedAddress(String userId, String addressId)` method, mirroring the existing `requireUserExists()` naming/shape; all three call sites now call it directly. No behaviour change — same lookup, same exception type, same status, same message, same call order relative to the rest of each method.
- command: `./gradlew test --tests 'com.keycloak.userstorage.service.AddressServiceImplTest' --tests 'com.keycloak.userstorage.controller.AddressControllerTest'`
- result: PASS — unchanged (`AddressServiceImplTest`: 12/0/0/0, `AddressControllerTest`: 11/0/0/0).
- command: `./gradlew test`
- result: FAIL (at the time) — unchanged, same pre-existing `UserServiceImpl` regression as the green step above, same failure counts (`AddressServiceImplTest` 12/0/0/0, `AddressControllerTest` 11/0/0/0, `AddressIntegrationTest` 8 total/5 failed (unchanged), `UserStorageIntegrationTest` 39 total/9 failed (unchanged)). Confirms this refactor introduced no new regressions on top of the pre-existing one. **Update: the pre-existing regression was a reverted drill (see green block); re-ran after the revert and the full suite is 70/70 PASS, 0 failures, 0 errors.**

### T-006 — simplify
- when: 2026-09-11T18:31:00+09:00
- change: none beyond the refactor above — reviewed `AddressService`, `AddressServiceImpl`, `AddressController`. No ternaries, no nesting beyond single guard clauses, no dead options; names (`deleteAddress`, `requireOwnedAddress`) match `01-spec.md`/`04-tasks.md` glossary terms directly (`deleteAddress` mirrors `createAddress`/`updateAddress`/`getAddress`). No further edits required.
- command: `./gradlew test`
- result: PASS — 70/70, 0 failures, 0 errors, after the drill regression was reverted (commit `8775d13`) and the suite re-run. T-006's own files remain fully green; no regressions anywhere in the suite.
- task status: **`done`**. T-006's own 6 ACs (AC-009, AC-010, AC-014) are implemented, green, refactored, and simplified; the full-suite run is clean.
