# Design: 2026-09-10-fix-otp-seed-test-drift

> Owner: `architect` · Phase 3 · Template: `.claude/templates/design.template.md`
>
> `01-spec.md` 넘어서는 새 동작/NFR 없음. 이 feature는 **테스트 전용** — `src/main/**` 변경 0.

## Architecture overview
- Component map: **변경 없음.** `UserController` · `UserService(Impl)` · `UserRepository(Impl)` ·
  `MultiAttributeEntry` / `USER_ATTRIBUTES` 는 시드 대비 이미 정확히 동작한다 (`_baseline.json`의
  `unit` 실패 3건은 전부 테스트 단언 결함).
- New/changed packages: 없음.
- 변경 파일: `src/test/java/com/keycloak/userstorage/integration/UserStorageIntegrationTest.java` 하나.
- 신규 import: `org.junit.jupiter.api.Tag` (AC 추적용 `@Tag` 도입 — 이 파일 최초).
- ADRs: ADR-001 (시드를 source-of-truth로, 테스트 단언을 정합).

## Module boundaries (ArchUnit)
- 추가 규칙 없음. 프로덕션 구조 불변.

## API Contract (관측된 계약 — 변경 아님, 참조용)
`/user/search` · `/user/count` 의 `otpMethod` 속성 키 검색 (BL-10). 시드: `SKIP` 14, `SMS` 2 (`john`, `jane`).
```json
{
  "endpoint": "GET /user/search?otpMethod=SKIP",
  "request":  { "example": {} },
  "success_response": { "status": 200, "body": [ "…14 users…" ] },
  "error_response":   { "status": 200, "example": [] }
}
```
```json
{
  "endpoint": "GET /user/count?otpMethod=SKIP",
  "request":  { "example": {} },
  "success_response": { "status": 200, "body": 14 },
  "error_response":   { "status": 200, "example": 0 }
}
```
```json
{
  "endpoint": "GET /user/search?otpMethod=SMS",
  "request":  { "example": {} },
  "success_response": { "status": 200, "body": [ "john", "jane" ] },
  "error_response":   { "status": 200, "example": [] }
}
```
```json
{
  "endpoint": "GET /user/search?username=john&otpMethod=SMS",
  "request":  { "example": {} },
  "success_response": { "status": 200, "body": [ "john" ] },
  "error_response":   { "status": 200, "example": [] }
}
```

## Data model
- Entities touched: 없음. `User` / `USER_ATTRIBUTES` / `CredentialData` 스키마·데이터 불변.
- `src/main/resources/user-data.json` 불변 (Q-001=A).
- H2 `ddl-auto=update` — schema-affecting change: **no**.

## Error model
- n/a — 새 에러 경로 없음.

## Logging
- 없음.

## NFRs
- (none) — `01-spec.md`에 NFR 없음. AC-006(실패 0)은 기능 기준이며 `/validate` `unit` 게이트가 확인.

## TDD 적용 노트
- `block-impl-without-failing-test` hook은 `src/main/**`만 게이트한다. 이 feature는 `src/test/**`만
  건드리므로 hook 비적용.
- **Red 는 이미 존재한다** — `_baseline.json` `unit` 실패 3건이 각 태스크의 red 상태. `red_failure_excerpt`는
  실측 메시지(`expected: <16> but was: <14>` 등)를 그대로 기록.
- AC-003의 신규 테스트(`?otpMethod=SMS` → {john,jane})는 프로덕션이 이미 정확하므로 characterization —
  작성 즉시 green. `04-tasks.md`에 그렇게 표기.
- refactor/simplify phase는 단언 수정·rename에 해당 없음 (behaviour 불변, 중복 없음).

## Risks & rollback
- Risk: AC-004 교집합 테스트에서 잘못된 기대 사용자 선택 → 단일 조건만 검증. Mitigation: AC-003이
  `john`/`jane`을 `SMS`로 못박음 + AC-004 검증 시나리오를 `?username=john&otpMethod=SMS` → `john`로 고정.
- Risk: 다른 테스트가 "otpMethod 전원 SKIP"에 암묵 의존. Mitigation: 전체 grep 결과 `otpMethod` 단언은
  명시된 3건 + `getById`의 `assertNotNull`(값 무관, SMS도 통과)뿐.
- Rollback: 커밋 revert. 스키마 영향 없음. `_baseline.json`은 fix 후 `harness.sh --baseline` 재실행으로 갱신
  (revert 시 함께 되돌림).

## Open Questions
- (없음)

## Resolved Questions
- Q-001 / Q-002 / Q-003 — `01-spec.md` `## Resolved Questions` 참조. ADR-001에 근거 기록.

## Design-review sign-off
- [x] `.claude/checklists/design-review.md` 통과 (아래 체크리스트 자가 점검).
- [x] 모든 AC가 `04-tasks.md` 태스크 1개 이상에서 도달 가능 (AC-001·002·005·007→T-001, AC-003·004·006→T-002).
- [x] 미해결 `Q-NNN` 없음.
