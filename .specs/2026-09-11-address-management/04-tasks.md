# Tasks: 2026-09-11-address-management

> Owner: `architect` · Phase 3 · Template: `.claude/templates/tasks.template.md`

## Inputs
- `03-design.md` revision: 2026-09-11 (this /plan run)

## Task index
| ID | Title | acs_covered | depends_on | gates |
|----|-------|-------------|------------|-------|
| T-001 | POST 주소 등록 — happy path + 1:N + 무제한 | AC-001, AC-002, AC-003, AC-012 | — | unit, integration, coverage |
| T-002 | POST 주소 등록 — 검증/사용자 없음 실패 경로 | AC-004, AC-005, AC-013 | T-001 | unit, integration, coverage |
| T-003 | GET 주소 단건 조회 | AC-006, AC-010, AC-014 | T-001 | unit, integration, coverage |
| T-004 | GET 주소 목록 조회 | AC-007, AC-015 | T-001 | unit, integration, coverage |
| T-005 | PUT 주소 수정 | AC-008, AC-005, AC-013, AC-010, AC-014 | T-003 | unit, integration, coverage |
| T-006 | DELETE 주소 삭제 | AC-009, AC-010, AC-014 | T-003 | unit, integration, coverage |
| T-007 | 사용자 삭제 시 주소 cascade 삭제 | AC-011 | T-001 | unit, integration, coverage |

## Tasks

### T-001: POST 주소 등록 — happy path + 1:N + 무제한
- **acs_covered:** AC-001, AC-002, AC-003, AC-012
- **files_in_scope:**
  - `src/main/java/com/keycloak/userstorage/model/Address.java`
  - `src/main/java/com/keycloak/userstorage/repository/AddressRepository.java`
  - `src/main/java/com/keycloak/userstorage/service/AddressService.java`
  - `src/main/java/com/keycloak/userstorage/service/AddressServiceImpl.java`
  - `src/main/java/com/keycloak/userstorage/controller/AddressController.java`
  - `src/test/java/com/keycloak/userstorage/integration/AddressIntegrationTest.java`
- **depends_on:** none
- **gates:** unit, integration, coverage
- **estimated_phases:** [red, green, refactor, simplify]
- **notes:** `Address` 필드는 `id`(String, PK), `userId`(String), `postalCode`(String), `roadAddress`(String), `detailAddress`(String, nullable) — ADR-001 참조(연관관계 애노테이션 없음). id는 `AddressServiceImpl.createAddress()`에서 `"addr-" + UUID...substring(0,8)`로 생성(ADR-002, `UserServiceImpl.createUser()`와 동일 패턴). 컨트롤러는 `POST /user/{userId}/addresses` → 201 + 생성된 `Address` 그대로 반환(별도 DTO 불필요, 응답 필드가 엔티티 필드와 정확히 일치 — `01-spec.md` Q-007). 이 태스크는 검증/실패 경로는 다루지 않는다(T-002). 테스트는 한 사용자에게 주소 2건 이상 생성해 1:N(AC-001)과 무제한(AC-012)을 함께 확인.

### T-002: POST 주소 등록 — 검증/사용자 없음 실패 경로
- **acs_covered:** AC-004, AC-005, AC-013
- **files_in_scope:**
  - `src/main/java/com/keycloak/userstorage/service/AddressServiceImpl.java`
  - `src/test/java/com/keycloak/userstorage/integration/AddressIntegrationTest.java`
- **depends_on:** T-001
- **gates:** unit, integration, coverage
- **estimated_phases:** [red, green, refactor, simplify]
- **notes:** ADR-003 — Bean Validation 아님, `AddressServiceImpl`에 수동 `validate(Address)` 추가. 우편번호: null 또는 `\d{5}` 불일치 시 400 `"postal code must be exactly 5 digits"`(AC-005). 도로명주소: null 또는 blank 시 400 `"road address must not be blank"`(AC-013). 대상 `userId`가 `userRepository`에 없으면 400보다 먼저 404 `"user not found"`(AC-004) — 사용자 존재 확인이 필드 검증보다 선행.

### T-003: GET 주소 단건 조회
- **acs_covered:** AC-006, AC-010, AC-014
- **files_in_scope:**
  - `src/main/java/com/keycloak/userstorage/repository/AddressRepository.java`
  - `src/main/java/com/keycloak/userstorage/service/AddressServiceImpl.java`
  - `src/main/java/com/keycloak/userstorage/controller/AddressController.java`
  - `src/test/java/com/keycloak/userstorage/integration/AddressIntegrationTest.java`
- **depends_on:** T-001
- **gates:** unit, integration, coverage
- **estimated_phases:** [red, green, refactor, simplify]
- **notes:** `AddressRepository.findByIdAndUserId(String id, String userId)` 파생 쿼리 하나로 "주소 없음"과 "다른 사용자 소유"를 동시에 처리(둘 다 빈 `Optional` → 404 `"address not found"`) — AC-010과 AC-014가 동일 코드 경로를 공유하므로 테스트는 두 시나리오(존재하지 않는 addressId / 다른 사용자의 addressId)를 각각 작성해 두 AC를 모두 태깅.

### T-004: GET 주소 목록 조회
- **acs_covered:** AC-007, AC-015
- **files_in_scope:**
  - `src/main/java/com/keycloak/userstorage/service/AddressServiceImpl.java`
  - `src/main/java/com/keycloak/userstorage/controller/AddressController.java`
  - `src/test/java/com/keycloak/userstorage/integration/AddressIntegrationTest.java`
- **depends_on:** T-001
- **gates:** unit, integration, coverage
- **estimated_phases:** [red, green, refactor, simplify]
- **notes:** `GET /user/{userId}/addresses` → 200 + `List<Address>`(0건이면 빈 배열, `AddressRepository.findByUserId`). 대상 `userId`가 없으면 404 `"user not found"`(AC-015, `01-spec.md` 갱신분 — `/plan` 중 발견된 갭).

### T-005: PUT 주소 수정
- **acs_covered:** AC-008, AC-005, AC-013, AC-010, AC-014
- **files_in_scope:**
  - `src/main/java/com/keycloak/userstorage/service/AddressServiceImpl.java`
  - `src/main/java/com/keycloak/userstorage/controller/AddressController.java`
  - `src/test/java/com/keycloak/userstorage/integration/AddressIntegrationTest.java`
- **depends_on:** T-003
- **gates:** unit, integration, coverage
- **estimated_phases:** [red, green, refactor, simplify]
- **notes:** `PUT /user/{userId}/addresses/{addressId}` → T-003의 `findByIdAndUserId` 조회(없음/소유자 불일치 → 404, AC-010/AC-014 재사용) 후 T-002의 `validate()` 재사용(AC-005/AC-013 — "create or update" 문구 그대로) → 세 필드 전체 교체 후 204(AC-008). 부분 PATCH 아님 — `postalCode`/`roadAddress`는 매 요청 필수, `detailAddress`는 생략/`null` 시 `null`로 덮어씀(User의 PATCH null=삭제 시맨틱과 다름 — 이 엔드포인트는 PUT 전체 교체).

### T-006: DELETE 주소 삭제
- **acs_covered:** AC-009, AC-010, AC-014
- **files_in_scope:**
  - `src/main/java/com/keycloak/userstorage/service/AddressServiceImpl.java`
  - `src/main/java/com/keycloak/userstorage/controller/AddressController.java`
  - `src/test/java/com/keycloak/userstorage/integration/AddressIntegrationTest.java`
- **depends_on:** T-003
- **gates:** unit, integration, coverage
- **estimated_phases:** [red, green, refactor, simplify]
- **notes:** `DELETE /user/{userId}/addresses/{addressId}` → T-003의 `findByIdAndUserId` 재사용해 404 처리(AC-010/AC-014), 존재하면 `addressRepository.deleteById(...)` 후 204(AC-009).

### T-007: 사용자 삭제 시 주소 cascade 삭제
- **acs_covered:** AC-011
- **files_in_scope:**
  - `src/main/java/com/keycloak/userstorage/service/UserServiceImpl.java`
  - `src/main/java/com/keycloak/userstorage/repository/AddressRepository.java`
  - `src/test/java/com/keycloak/userstorage/integration/UserStorageIntegrationTest.java`
- **depends_on:** T-001
- **gates:** unit, integration, coverage
- **estimated_phases:** [red, green, refactor, simplify]
- **notes:** ADR-001 — `AddressRepository.deleteByUserId(String userId)` 파생 삭제 쿼리 추가, `UserServiceImpl.deleteUser()`에서 `userRepository.deleteById(...)` 이전에 호출. `UserServiceImpl`이 `AddressRepository`를 직접 주입받는다(서비스가 다른 서비스 아닌 리포지토리에 의존 — 레이어 규칙 준수). 기존 `DELETE /user/{id}` 회귀 여부도 이 태스크에서 함께 확인.

## Cross-cutting tasks (near the end)
- ArchUnit 규칙 추가 — 불필요(신규 패키지 경계 없음, `03-design.md` "Module boundaries" 참조). `archunit` 하네스 레이어 자체가 아직 `skipped`라 지금 실행할 규칙도 없음.
- OpenAPI contract check — 불필요(springdoc 미배선, `_onboarding.md` "미설정 레이어" 참조). `03-design.md`의 스케치가 현재의 계약 문서 역할을 한다.

## Open Questions
- (없음)

## Sign-off
- [x] Every AC from `01-spec.md` is covered by ≥1 task. (AC-001~015 전부 위 표에서 확인)
- [x] Every task touching `src/main/**` also lists a `src/test/**` file.
- [x] Task index is in dependency order.
- [x] All `Q-NNN` resolved or deferred-with-rationale.
- [ ] Reviewed by user on <YYYY-MM-DD>.
