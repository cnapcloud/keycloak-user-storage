# Spec: 2026-09-10-fix-otp-seed-test-drift — 시드 otpMethod ↔ 통합테스트 단언 정합

> Owner: `spec-author` · Phase 1 · Template: `.claude/templates/spec.template.md`
>
> **No invention.** 요청 · 대화 · 코드에 없는 것은 `Q-NNN`으로 남긴다.

## Source
- Origin: `.specs/README.md` "진행 중 / 예정" 행 `fix-otp-seed-test-drift` + 사용자 요청 (이 세션)
- Reference: `.specs/_baseline.json` (`gates.unit` = fail, 38 중 3 실패) · 커밋 `8bdd8d6`
- Snapshot date: 2026-09-10
- Snapshot summary:
  > otpMethod=SKIP 시드가 14명인데 UserStorageIntegrationTest는 16명 기대 (커밋 8bdd8d6이
  > SKIP→SMS 2건 변경 후 단언 미갱신). 선행 실패 3건:
  > `search_byAttributeKey_otpMethod_returnsAllUsers`, `count_byAttributeKey_otpMethod_returns17`,
  > `search_combinedFieldAndAttribute_returnsIntersection`.
- 확인된 사실:
  - `src/main/resources/user-data.json`: 시드 사용자 16명 중 `otpMethod`가 `SKIP` 14명, `SMS` 2명(`john`, `jane`).
  - 커밋 `8bdd8d6` "Change test user otpMethod from SKIP to SMS" 는 `user-data.json`만 수정(+2/-2), 테스트는 미수정.
  - 실패 3건은 모두 `otpMethod` 단언이 "시드 16명 전원 SKIP"을 가정한다.
  - 프로덕션 엔드포인트(`/user/search`, `/user/count`)는 시드 대비 정확히 동작한다 — 결함은 테스트 단언 쪽.

## Goal
`UserStorageIntegrationTest`의 `otpMethod` 관련 단언이 실제 시드 데이터(`SKIP` 14명, `SMS` 2명 = `john`·`jane`)를
반영하도록 바로잡아, harness `unit` 게이트가 실패 3건 없이 통과한다. 프로덕션 동작 변경은 없다.

## Acceptance Criteria
EARS-lite. AC 하나당 조건 하나 · 결과 하나. id 불변.

- AC-001: When `GET /user/search?otpMethod=SKIP` 이 시드 데이터에 대해 호출되면, the system shall `otpMethod` 속성이 `SKIP`인 시드 사용자 14명을 반환한다.
- AC-002: When `GET /user/count?otpMethod=SKIP` 이 시드 데이터에 대해 호출되면, the system shall `14`를 반환한다.
- AC-003: When `GET /user/search?otpMethod=SMS` 이 시드 데이터에 대해 호출되면, the system shall 사용자 `john`, `jane` 두 명만 반환한다.
- AC-004: When `GET /user/search` 가 직접 필드(`username`)와 `otpMethod` 속성을 함께 필터로 받으면, the system shall 두 조건을 모두 만족하는 사용자만(교집합) 반환한다. (검증 시나리오: `?username=john&otpMethod=SMS` → `john` 1명 — Q-002 참조.)
- AC-005: The integration test suite shall `otpMethod` 개수를 시드(`SKIP` 14, `SMS` 2)와 일치하게만 단언한다.
- AC-006: When 이 변경 후 harness `unit` 게이트가 실행되면, the system shall 테스트 실패 0건을 보고한다 (기존 3건 → 0건).
- AC-007: The integration test suite shall `otpMethod` 개수를 시드(`SKIP` 14, `SMS` 2)와 모순되게 지칭하는 테스트 이름을 포함하지 않는다.

USP 계약: `/user/search`, `/user/count` 속성 키 검색은 `.specs/README.md` 완료 이력 BL-10.

## Domain Entities and Relationships
> 비즈니스 언어. 클래스/테이블/ORM 이름 금지.

- **시드 사용자** — 서비스 기동 시 미리 적재되는 계정. 속성 중 `otpMethod`(일회용 비밀번호 전달 수단)은 값이 `SKIP` 또는 `SMS`.
- **시드 데이터셋 1..\* 시드 사용자** — 총 16명. `otpMethod=SKIP` 14명, `otpMethod=SMS` 2명(`john`, `jane`).
- **속성 키 검색** — 검색/카운트 요청의 쿼리 파라미터가 직접 필드가 아니면 사용자 속성 키로 해석되어 매칭.

## Data Impact
- `User` / `USER_ATTRIBUTES` / `CredentialData` 스키마 변경: **없음**.
- 시드 파일(`user-data.json`) 변경: **없음** (Q-001=A 확정 — 시드가 source-of-truth).
- 변경 범위: 통합 테스트 파일의 단언/테스트명뿐.
- 다른 엔드포인트와 공유: 시드 데이터는 모든 읽기 엔드포인트가 공유하나, 본 작업은 테스트만 건드리므로 영향 없음.

## Non-Goals
- `/user/search` · `/user/count` 의 검색/카운트 로직 변경.
- 시드 사용자 수·구성 변경, `user-data.json` 편집.
- `otpMethod` 외 다른 속성 관련 테스트 수정.
- `otpMethod=SMS` 경로에 대한 신규 기능/엔드포인트 추가.
- `_baseline.json` 을 이 스펙에서 직접 편집 (수정은 fix 후 `/onboard` 또는 `harness.sh --baseline` 재실행으로).

## Glossary
- **otpMethod** — 시드 사용자 속성. OTP(일회용 비밀번호) 전달 수단. 관측된 값: `SKIP`, `SMS`.
- **선행 실패 (baseline failure)** — harness 도입 이전부터 `main`에 존재하던 테스트 실패. `_baseline.json`에 캡처됨.
- **drift** — 시드 데이터와 테스트 단언이 서로 어긋난 상태.

## Assumptions
> 사용자/소스가 명시한 것만.
- 커밋 `8bdd8d6`의 SKIP→SMS 변경은 의도된 것이다 (커밋 메시지 "Change test user otpMethod from SKIP to SMS", 소스가 "단언 미갱신"으로 표현 — 시드가 아니라 단언이 결함).

## Open Questions
> 모든 불확실성. 기본값을 고르지 않는다.

- (없음)

## Resolved Questions
> 사용자 답변 verbatim + 시각.

- Q-001 (2026-09-10): **(A)** — "현재 시드 데이터 기준으로." 시드(`SKIP` 14 / `SMS` 2)를 source-of-truth로 두고 테스트 단언 3건을 시드에 맞게 수정한다. `user-data.json`은 건드리지 않는다.
- Q-002 (2026-09-10): **(A)** — "현재 시드 데이터 기준으로." `search_combinedFieldAndAttribute_returnsIntersection` 는 `?username=john&otpMethod=SMS` 로 질의하고 `john` 1명을 기대하도록 수정한다 (john 의 현재 시드 속성이 `SMS`).
- Q-003 (2026-09-10): **(A)** — "stale 처리한 거 아니야." drift 정리의 일부로 스테일 테스트명도 정확하게 rename한다 (`count_byAttributeKey_otpMethod_returns17`, `search_byAttributeKey_otpMethod_returnsAllUsers` 등). → AC-005(단언)와 AC-007(테스트명)로 분리.

## Sign-off
- [x] 모든 AC atomic + 테스트 가능.
- [x] 모든 `Q-NNN` resolved 또는 근거와 함께 deferred. (Q-001~003 Resolved)
- [x] Source 기록됨.
- [ ] 사용자 리뷰: 2026-09-__
