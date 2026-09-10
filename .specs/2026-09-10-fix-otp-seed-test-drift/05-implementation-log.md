# Implementation Log: 2026-09-10-fix-otp-seed-test-drift

> Owner: `/build` orchestration (test-engineer + implementer) · Phase 4.
> 테스트 전용 feature — `src/main/**` 미변경. `block-impl-without-failing-test` hook 비적용.
> Red는 `_baseline.json` `unit` 실패 3건이 제공(신규 실패 테스트 작성 대신 기존 결함 단언 정합).

---

## T-001 — `otpMethod=SKIP` 개수 단언 정합 + 스테일 이름 rename

acs_covered: AC-001, AC-002, AC-005, AC-007
files_in_scope: `src/test/java/com/keycloak/userstorage/UserStorageIntegrationTest.java`

### red (2026-09-10)
기존 실패 2건을 실행해 red 상태 확인:

```
./gradlew test --tests '...UserStorageIntegrationTest.search_byAttributeKey_otpMethod_returnsAllUsers' \
               --tests '...UserStorageIntegrationTest.count_byAttributeKey_otpMethod_returns17'

> search_byAttributeKey_otpMethod_returnsAllUsers() FAILED
    org.opentest4j.AssertionFailedError: otpMethod=SKIP 는 시드 16명 전원 ==> expected: <16> but was: <14>
        at UserStorageIntegrationTest.java:528
> count_byAttributeKey_otpMethod_returns17() FAILED
    org.opentest4j.AssertionFailedError: otpMethod=SKIP 카운트 16명 전원 ==> expected: <16> but was: <14>
        at UserStorageIntegrationTest.java:564
```

원인: 시드(`user-data.json`)의 `otpMethod=SKIP` 사용자는 14명 (`john`·`jane`은 커밋 `8bdd8d6`에서 `SMS`).
단언은 16을 기대. 프로덕션 응답(14)이 정답 — ADR-001.

### green (2026-09-10)
`UserStorageIntegrationTest.java`:

- `import org.junit.jupiter.api.Tag;` 추가 (이 파일 최초의 `@Tag`).
- `search_byAttributeKey_otpMethod_returnsAllUsers` → **rename** `search_byAttributeKey_otpMethod_skip_returns14`.
  단언 `16` → `14`, 메시지 "시드 16명 전원" → "시드 사용자 14명 (john·jane은 SMS)". `@Tag("AC-001")` `@Tag("AC-005")` `@Tag("AC-007")`.
- `count_byAttributeKey_otpMethod_returns17` → **rename** `count_byAttributeKey_otpMethod_skip_returns14`.
  단언 `16L` → `14L`, 메시지 동일 패턴. `@Tag("AC-002")` `@Tag("AC-005")` `@Tag("AC-007")`.

```
./gradlew test --tests '...search_byAttributeKey_otpMethod_skip_returns14' \
               --tests '...count_byAttributeKey_otpMethod_skip_returns14'
  > both PASSED — BUILD SUCCESSFUL

./gradlew test
  > 38 tests completed, 1 failed
  > 남은 실패 1건 = search_combinedFieldAndAttribute_returnsIntersection (T-002 범위)
```
baseline `unit` 실패 3 → 1 (T-001이 2건 해소, 회귀 0).

### refactor (2026-09-10)
해당 없음 — 단언 값·이름 수정뿐, 로직/중복 없음. 시그니처·동작 불변.

### simplify (2026-09-10)
해당 없음 — `clarity-over-cleverness` 적용 대상 없음. 수정된 두 테스트는 이미 최소 형태.

### done
T-001 `acs_covered` = AC-001·AC-002·AC-005·AC-007 전부 `@Tag` 테스트로 커버. `phase: done`.

---

## T-002 — `otpMethod=SMS` 단언 + 필드·속성 교집합 정합

acs_covered: AC-003, AC-004, AC-006
files_in_scope: `src/test/java/com/keycloak/userstorage/UserStorageIntegrationTest.java`

### red (2026-09-10)
```
./gradlew test --tests '...UserStorageIntegrationTest.search_combinedFieldAndAttribute_returnsIntersection'

> search_combinedFieldAndAttribute_returnsIntersection() FAILED
    org.opentest4j.AssertionFailedError: expected: <1> but was: <0>
        at UserStorageIntegrationTest.java:560
```
원인: 질의 `?username=john&otpMethod=SKIP` — `john`의 시드 `otpMethod`는 `SMS`이므로 교집합 0.

### green (2026-09-10)
`UserStorageIntegrationTest.java`:

- `search_combinedFieldAndAttribute_returnsIntersection`: 질의 `otpMethod=SKIP` → `SMS`,
  주석 "john 은 otpMethod=SKIP → 1명" → "john 은 otpMethod=SMS → 1명 (필드+속성 교집합)".
  `@Tag("AC-004")` `@Tag("AC-006")` (AC-006 = 이 테스트 통과로 baseline 마지막 실패가 닫힘).
- 신규 `search_byAttributeKey_otpMethod_sms_returnsJohnAndJane` (characterization — 프로덕션 이미 정확):
  `?otpMethod=SMS` → size 2, username 정렬 `["jane","john"]`. `@Tag("AC-003")` `@Tag("AC-005")`.
  순수 읽기라 `@Order` 생략.

```
./gradlew test --tests '...search_combinedFieldAndAttribute_returnsIntersection' \
               --tests '...search_byAttributeKey_otpMethod_sms_returnsJohnAndJane'
  > both PASSED

./gradlew clean test
  > tests=39 failures=0 errors=0 skipped=0 — BUILD SUCCESSFUL
```
baseline `unit` 실패 3 → **0**. 테스트 수 38 → 39 (SMS characterization 1건 추가).

### refactor (2026-09-10)
해당 없음 — 질의 문자열·주석 수정 + 신규 단언 테스트뿐. 중복/레이어 이동 없음.

### simplify (2026-09-10)
해당 없음. 신규 테스트는 `stream().map().sorted().toList()` + `assertEquals(List.of(...))`로 이미 최소.

### done
T-002 `acs_covered` = AC-003·AC-004·AC-006 전부 `@Tag` 테스트로 커버. traceability 7/7 covered. `phase: done`.
`04-tasks.md` 전 태스크 done → 다음 `/validate`.
