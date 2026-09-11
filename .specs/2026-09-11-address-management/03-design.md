# Design: 2026-09-11-address-management

> Owner: `architect` · Phase 3 · Template: `.claude/templates/design.template.md`
>
> No new behaviour or NFR beyond `01-spec.md`. A gap → `Q-NNN` here, bounce to `spec-author`.

## Architecture overview
- Component map: `AddressController` → `AddressService`(iface) / `AddressServiceImpl` → `AddressRepository` → `Address`. 기존 `UserServiceImpl.deleteUser()`가 `AddressRepository`를 직접 주입받아 cascade 삭제를 수행한다(서비스가 다른 서비스가 아니라 리포지토리에 의존 — 레이어 규칙 A3 준수).
- New/changed packages: 새 클래스는 기존 최상위 패키지(`model`, `repository`, `service`, `controller`)에 추가된다. 새 패키지 경계 없음.
- ADRs: [ADR-001](adr/ADR-001-address-user-plain-fk.md) (FK를 JPA 연관관계 대신 평범한 컬럼으로), [ADR-002](adr/ADR-002-address-id-format.md) (`addr-XXXXXXXX` id 포맷), [ADR-003](adr/ADR-003-manual-validation-not-bean-validation.md) (Bean Validation 대신 수동 검증)

## Module boundaries (ArchUnit)
- 신규 패키지 경계 없음 — 새 클래스가 기존 A1~A8 규칙 범위 안에 자연히 들어간다: `AddressController`(컨트롤러, 리포지토리 미의존), `AddressService`/`AddressServiceImpl`(인터페이스+구현 네이밍), `AddressRepository`(리포지토리 계층에서만 의존), reflection 미사용, `System.out` 미사용.
- 단, 이 repo의 `archunit` 하네스 레이어는 아직 `skipped`(`_onboarding.md` 미설정 레이어) — 지금은 규칙을 코드로 실행할 수 없다. `harness-gradle.md`로 배선되면 A1~A8이 자동으로 신규 클래스에도 적용되므로 Address 전용 규칙 추가는 불필요.

## API Contract (OpenAPI sketch)
모든 신규 엔드포인트. 성공/에러 예시 + 상태코드만(전체 OpenAPI 문서 아님).

```json
{
  "endpoint": "POST /user/{userId}/addresses",
  "request": { "example": { "postalCode": "06236", "roadAddress": "서울특별시 강남구 테헤란로 123", "detailAddress": "10층 1001호" } },
  "success_response": { "status": 201, "body": { "id": "addr-3f9c1001", "userId": "u-092d66b8", "postalCode": "06236", "roadAddress": "서울특별시 강남구 테헤란로 123", "detailAddress": "10층 1001호" } },
  "error_responses": [
    { "status": 404, "example": { "error": "user not found" } },
    { "status": 400, "example": { "error": "postal code must be exactly 5 digits" } },
    { "status": 400, "example": { "error": "road address must not be blank" } }
  ]
}
```
```json
{
  "endpoint": "GET /user/{userId}/addresses",
  "request": { "example": null },
  "success_response": { "status": 200, "body": [ { "id": "addr-3f9c1001", "userId": "u-092d66b8", "postalCode": "06236", "roadAddress": "서울특별시 강남구 테헤란로 123", "detailAddress": "10층 1001호" } ] },
  "error_response": { "status": 404, "example": { "error": "user not found" } }
}
```
```json
{
  "endpoint": "GET /user/{userId}/addresses/{addressId}",
  "request": { "example": null },
  "success_response": { "status": 200, "body": { "id": "addr-3f9c1001", "userId": "u-092d66b8", "postalCode": "06236", "roadAddress": "서울특별시 강남구 테헤란로 123", "detailAddress": "10층 1001호" } },
  "error_response": { "status": 404, "example": { "error": "address not found" } }
}
```
```json
{
  "endpoint": "PUT /user/{userId}/addresses/{addressId}",
  "request": { "example": { "postalCode": "06236", "roadAddress": "서울특별시 강남구 테헤란로 456", "detailAddress": null } },
  "success_response": { "status": 204, "body": null },
  "error_responses": [
    { "status": 404, "example": { "error": "address not found" } },
    { "status": 400, "example": { "error": "postal code must be exactly 5 digits" } }
  ]
}
```
```json
{
  "endpoint": "DELETE /user/{userId}/addresses/{addressId}",
  "request": { "example": null },
  "success_response": { "status": 204, "body": null },
  "error_response": { "status": 404, "example": { "error": "address not found" } }
}
```

`{"error": "user not found"}` 대 `{"error": "address not found"}`: 대상 `userId` 자체가 없을 때(AC-004, AC-015)는 전자, `userId`는 존재하나 그 `addressId`가 없거나 다른 사용자 소유일 때(AC-010, AC-014)는 후자 — 둘 다 404지만 메시지로 원인을 구분한다(기존 `"user not found"` 컨벤션 그대로 재사용 + 신규 `"address not found"` 메시지 추가).

## Data model
- Entities touched:
  - `Address` (신규 엔티티/테이블, `ddl-auto=update`로 자동 생성): `id`(PK, String, `addr-XXXXXXXX`), `userId`(String, not null, FK 없음 — ADR-001), `postalCode`(String, not null), `roadAddress`(String, not null), `detailAddress`(String, nullable).
  - `User` — **무변경** (클래스도, 컬럼도).
  - `USER_ATTRIBUTES`, `CredentialData` — 무변경.
  - 기존 `UserServiceImpl.deleteUser()`의 **동작**이 바뀐다(AC-011): `userRepository.deleteById(...)` 전에 `addressRepository.deleteByUserId(user.getId())` 호출 추가. 스키마 변경 아님, 서비스 로직 변경.
- H2 `ddl-auto=update` — no migration file. `Address` 신규 테이블은 애플리케이션 기동 시 자동 생성됨. 스키마 마이그레이션 파일 불필요(기존 컨벤션대로).

## Error model
Per `spring-error-handling`:

| 상황 | 코드 | 관련 AC |
|---|---|---|
| GET(단건/목록) 성공 | 200 | AC-006, AC-007 |
| POST 생성 성공 | 201 | AC-003 |
| PUT/DELETE 성공 | 204 | AC-008, AC-009 |
| 대상 `userId` 없음 | 404 `{"error":"user not found"}` | AC-004, AC-015 |
| `addressId` 없음 또는 다른 사용자 소유 | 404 `{"error":"address not found"}` | AC-010, AC-014 |
| 우편번호 형식 오류 | 400 `{"error":"postal code must be exactly 5 digits"}` | AC-005 |
| 도로명주소 공백 | 400 `{"error":"road address must not be blank"}` | AC-013 |

전부 `ResponseStatusException` + 기존 `GlobalExceptionHandler`(`@RestControllerAdvice`)를 그대로 재사용. 신규 예외 클래스나 핸들러 불필요.

## Logging
- `01-spec.md`가 요구하는 로깅 NFR 없음 — 신규 `log.*` 호출을 이 기능 때문에 의무화하지 않는다. (참고: 이 코드베이스는 `@Slf4j`를 한 곳에 붙여만 두고 실제로는 어디서도 `log.*`를 호출하지 않는다 — 기존 관행 그대로 따름.)

## NFRs
> Only what `01-spec.md` already requires.
- (none)

## Risks & rollback
- Risk: `UserServiceImpl.deleteUser()`에 cascade 삭제를 추가하는 것은 기존 `DELETE /user/{id}` 동작을 변경하는 것이라 회귀 위험이 있다. Mitigation: 기존 `UserStorageIntegrationTest`의 `DELETE /user/{id}` 관련 테스트를 재실행해 회귀가 없는지 T-007에서 확인하고, cascade 자체를 검증하는 신규 테스트를 추가한다.
- Risk: `Address` 신규 테이블이 `ddl-auto=update`로 조용히 생성됨 — 스키마 검증 도구 없음. Mitigation: 기존 컨벤션과 동일(신규 위험 아님).
- Rollback: 커밋 revert. `Address` 테이블은 다음 기동 시 `ddl-auto=update`가 자동 재생성/유지하므로 별도 마이그레이션 롤백 불필요(H2 in-memory, 재기동 시 초기화).

## Open Questions
- (없음)

## Resolved Questions
- (해당 사항 없음 — `01-spec.md`의 `Q-005` 커버리지 공백은 새 `Q-NNN`을 만들지 않고 `AC-015` 추가로 즉시 해소함, 상세는 `01-spec.md` Source 로그 참조)

## Design-review sign-off
- [x] `.claude/checklists/design-review.md` passes.
- [x] Every AC reachable from ≥1 task in `04-tasks.md`.
- [x] No unresolved `Q-NNN`.
