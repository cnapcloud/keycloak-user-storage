# Tasks: 2026-09-10-fix-otp-seed-test-drift

> Owner: `architect` · Phase 3 · Template: `.claude/templates/tasks.template.md`
>
> 테스트 전용 feature. 변경 파일은 `src/test/java/com/keycloak/userstorage/integration/UserStorageIntegrationTest.java` 하나.
> `src/main/**` 미변경 → `block-impl-without-failing-test` hook 비적용. Red는 `_baseline.json`의
> 기존 실패 3건이 이미 제공.

## Inputs
- `03-design.md` revision: 2026-09-10 (git `462df66` 기준)

## Task index
| ID | Title | acs_covered | depends_on | gates |
|----|-------|-------------|------------|-------|
| T-001 | `otpMethod=SKIP` 개수 단언 정합 + 스테일 이름 rename | AC-001, AC-002, AC-005, AC-007 | — | unit |
| T-002 | `otpMethod=SMS` 단언 + 필드·속성 교집합 정합 | AC-003, AC-004, AC-006 | T-001 | unit |

## Tasks

### T-001: `otpMethod=SKIP` 개수 단언 정합 + 스테일 이름 rename
- **acs_covered:** AC-001, AC-002, AC-005, AC-007
- **files_in_scope:**
  - `src/test/java/com/keycloak/userstorage/integration/UserStorageIntegrationTest.java`
- **depends_on:** none
- **gates:** unit
- **estimated_phases:** [red, green]
  - red: 기존 실패 `search_byAttributeKey_otpMethod_returnsAllUsers` (`expected: <16> but was: <14>`),
    `count_byAttributeKey_otpMethod_returns17` (`expected: <16> but was: <14>`) — 실행해 `red_failure_excerpt` 기록.
  - green: 기대값 16 → 14, 메시지 문자열 "시드 16명 전원" → "SKIP 14명". 메서드 rename:
    `search_byAttributeKey_otpMethod_returnsAllUsers` → `search_byAttributeKey_otpMethod_skip_returns14`,
    `count_byAttributeKey_otpMethod_returns17` → `count_byAttributeKey_otpMethod_skip_returns14`.
    `@Tag("AC-001")` / `@Tag("AC-002")` 부착 (+ `import org.junit.jupiter.api.Tag`).
- **estimated:** ~1 h
- **notes:** refactor/simplify 해당 없음 (behaviour 불변). AC-005는 "개수를 시드와 일치하게만 단언",
  AC-007은 "이름이 시드와 모순되지 않음" — 이 두 테스트 수정으로 충족. AC-007의 나머지(다른 스테일
  이름)도 이 파일에서 함께 정리하되, `otpMethod` 무관 메서드는 건드리지 않는다.

### T-002: `otpMethod=SMS` 단언 + 필드·속성 교집합 정합
- **acs_covered:** AC-003, AC-004, AC-006
- **files_in_scope:**
  - `src/test/java/com/keycloak/userstorage/integration/UserStorageIntegrationTest.java`
- **depends_on:** T-001
- **gates:** unit
- **estimated_phases:** [red, green]
  - red: 기존 실패 `search_combinedFieldAndAttribute_returnsIntersection`
    (`?username=john&otpMethod=SKIP` → `expected: <1> but was: <0>`) — 실행해 `red_failure_excerpt` 기록.
  - green: 질의를 `?username=john&otpMethod=SMS` 로, 기대 `john` 1명 유지. 주석
    "john 은 otpMethod=SKIP → 1명" → "john 은 otpMethod=SMS → 1명". `@Tag("AC-004")`.
  - AC-003 신규 테스트 `search_byAttributeKey_otpMethod_sms_returnsJohnAndJane`:
    `?otpMethod=SMS` → size 2, username 집합 `{john, jane}`. `@Tag("AC-003")`.
    **characterization** — 프로덕션이 이미 정확하므로 작성 즉시 green (red 아님).
- **estimated:** ~1 h
- **notes:** T-002 완료 후 `./gradlew test` 실행 → 실패 0건이면 AC-006 충족. AC-006의 최종 확인은
  `/validate`의 `unit` 게이트. `_baseline.json` 갱신은 이 태스크 범위 밖 (fix·커밋 후 사람이
  `harness.sh --baseline`).

## Cross-cutting tasks
- ArchUnit / OpenAPI contract: 해당 없음 (프로덕션 구조·계약 불변).

## Open Questions
- (없음)

## Sign-off
- [x] `01-spec.md`의 모든 AC가 태스크 1개 이상으로 커버됨 (AC-001·002·005·007 → T-001; AC-003·004·006 → T-002).
- [x] `src/main/**`을 건드리는 태스크 없음 → `src/test/**` 나열 규칙 자동 충족.
- [x] Task index가 의존 순서 (T-001 → T-002, 같은 파일이라 순차).
- [x] 모든 `Q-NNN` resolved.
- [ ] 사용자 리뷰: 2026-09-__
