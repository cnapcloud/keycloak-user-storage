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
