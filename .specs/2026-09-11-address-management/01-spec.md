# Spec: 2026-09-11-address-management — 주소 관리 기능

> Owner: `spec-author` · Phase 1 · Template: `.claude/templates/spec.template.md`
>
> **No invention.** Anything not in the request, the conversation, or the code is a `Q-NNN` — ask the user.

## Source
- Origin: user request (free text via `/spec`)
- Reference: 대화 내 `/spec` 커맨드 인자
- Snapshot date: 2026-09-11
- Snapshot summary:
  > 주소 관리 기능
  >
  > - 주소는 User와 별도의 엔티티로 관리한다.
  > - 한 사용자는 여러 개의 주소를 등록할 수 있다 (1:N).
  > - 기본 CRUD를 지원한다: 주소 등록, 조회, 수정, 삭제.
  > - 관리하는 주소 항목은 우편번호, 도로명주소, 상세주소 세 가지다. (지번주소, 별칭, 기본주소 여부는 이번 범위에 포함하지 않는다.)
  >
  > 여기까지만 진행하는거로 스펙요청
- 2026-09-11 추가 지시: "이건 예제이니 너가 일반적인 상황을 가정해서 답을 다 만들어" — 최초 `01-spec.md`에 남긴 `Q-001`~`Q-008`을 사용자가 직접 값 하나하나를 지정하는 대신, 일반적인(통상적인) 상황을 가정한 기본값으로 agent가 채우도록 명시적으로 지시함. 아래 `Resolved Questions`의 각 답은 이 지시에 따라 확정된 것으로, 티켓에 명시된 값이 아니라 "합리적인 일반값"임을 표시해 둔다.
- 2026-09-11 `/plan`(architect, design hat) 중 발견: `Q-005`가 "목록 조회도 대상 사용자 없으면 404"라고 명시했는데 이를 커버하는 AC가 없어 `AC-015`로 보완(spec-author 복귀 없이 같은 세션에서 author hat으로 직접 수정, 사용자에게 고지).

## Goal
사용자(User)가 자신의 주소를 여러 건 등록·조회·수정·삭제할 수 있도록, 주소를 User와 분리된 별도 엔티티로 관리하는 기본 CRUD 기능을 제공한다.

## Acceptance Criteria
EARS-lite (see `.claude/docs/spec-format.md`). One condition per AC. IDs stable, never reused.

- AC-001: The system shall allow a single user to have zero or more address records (a 1:N relationship between user and address).
- AC-002: The system shall manage exactly three fields per address record — postal code, road address, and detail address.
- AC-003: When a client submits `POST /user/{userId}/addresses` with a postal code, a road address, and (optionally) a detail address for an existing user, the system shall create a new address record associated with that user and respond with 201 and the created address's id, user id, postal code, road address, and detail address.
- AC-004: If a client submits `POST /user/{userId}/addresses` for a `userId` that does not exist, then the system shall not create any address record and shall respond with 404.
- AC-005: If a client submits an address create or update request whose postal code is not exactly 5 digits, then the system shall not persist the change and shall respond with 400.
- AC-006: When a client submits `GET /user/{userId}/addresses/{addressId}` for an address that exists and belongs to that user, the system shall respond with 200 and that address's id, user id, postal code, road address, and detail address.
- AC-007: When a client submits `GET /user/{userId}/addresses` for an existing user, the system shall respond with 200 and the list of all address records belonging to that user (an empty list if the user has none).
- AC-008: When a client submits `PUT /user/{userId}/addresses/{addressId}` with a postal code, a road address, and (optionally) a detail address for an existing address belonging to that user, the system shall update the stored address with the submitted values and respond with 204.
- AC-009: When a client submits `DELETE /user/{userId}/addresses/{addressId}` for an existing address belonging to that user, the system shall remove that address record and respond with 204.
- AC-010: If a client submits a `GET`, `PUT`, or `DELETE` request for an `addressId` that does not exist, then the system shall leave all address data unchanged and respond with 404.
- AC-011: When a user is deleted via `DELETE /user/{id}`, the system shall also delete all address records belonging to that user (cascade delete).
- AC-012: The system shall not enforce any limit on the number of address records a single user may register.
- AC-013: If a client submits an address create or update request whose road address is blank, then the system shall not persist the change and shall respond with 400.
- AC-014: If a client submits a `GET`, `PUT`, or `DELETE` request for an `addressId` that exists but does not belong to the `userId` given in the path, then the system shall leave all address data unchanged and respond with 404.
- AC-015: If a client submits `GET /user/{userId}/addresses` for a `userId` that does not exist, then the system shall respond with 404.

USP 계약: 신규 엔드포인트(`/user/{userId}/addresses...`)이며 기존 `/user`, `/credential` 계약과는 별도. `DELETE /user/{id}`의 기존 동작(BL-02)에 AC-011이 cascade 삭제를 추가한다.

## Domain Entities and Relationships
> Business language only — no class/table/ORM names. Unknown → `Q-NNN`.

- **Address** — purpose: 사용자의 주소 정보를 저장; key attributes: 우편번호(필수, 5자리 숫자), 도로명주소(필수), 상세주소(선택).
- **User 1..* Address** — 한 사용자는 여러 개의 주소를 등록할 수 있다 (1:N). 주소는 반드시 하나의 사용자에 속하며, 사용자가 삭제되면 그 사용자의 주소도 함께 삭제된다.

## Data Impact
- New table/column, or change to `User` / `USER_ATTRIBUTES` / `CredentialData`: Address는 신규 엔티티/테이블(자체 식별자 보유, User를 참조하는 진짜 1:N 자식 — 이 저장소 최초 사례). `User` 테이블 자체에는 컬럼 변경 없음. 기존 `DELETE /user/{id}` 흐름에 "소속 주소 cascade 삭제"가 추가됨(AC-011).
- Shared with another endpoint: no. 신규 엔드포인트(`/user/{userId}/addresses`, `/user/{userId}/addresses/{addressId}`)로 도입.

## Non-Goals
- 지번주소 관리는 이번 범위에 포함하지 않는다.
- 별칭(주소 이름표) 관리는 이번 범위에 포함하지 않는다.
- 기본주소 여부 관리는 이번 범위에 포함하지 않는다.
- 사용자 1명당 주소 개수 상한(무제한으로 확정, Q-008).
- 생성/수정 일시 등 타임스탬프 필드 노출 (응답 바디에는 id · userId · 세 주소 필드만 포함, Q-007).

## Glossary
- **우편번호** — postal code. 5자리 숫자로 검증.
- **도로명주소** — road-name address (한국 도로명주소 체계). 필수, 공백 불가.
- **상세주소** — detail/secondary address line (동/호수 등). 선택 입력.
- **addr-XXXXXXXX** — Address 식별자 포맷. `User`의 `u-XXXXXXXX` 컨벤션과 동일하게 prefix + UUID 8자리.

## Assumptions
> Only assumptions stated by the user or the source. The agent never adds one silently.
- (none — `Q-001`~`Q-008`은 추측이 아니라 사용자가 명시적으로 요청한 "일반적인 상황을 가정한 기본값" 지시에 따라 `Resolved Questions`에서 확정함.)

## Open Questions
> Every uncertainty. The agent does not pick a default.

- (없음)

## Resolved Questions
> Verbatim user answer + timestamp.

- Q-001 (2026-09-11): **(A, nested)** — 사용자 지시("일반적인 상황을 가정") 기준 통상적인 REST 하위 리소스 설계를 채택. 컬렉션·단건 모두 User 하위로 nest: `POST/GET /user/{userId}/addresses`, `GET/PUT/DELETE /user/{userId}/addresses/{addressId}`. `/credential/{id}`처럼 flat으로 두지 않는 이유: 주소는 User에 완전히 종속된 자식 리소스라 소유자 검증이 경로 자체에 드러나는 편이 일반적이라고 판단.
- Q-002 (2026-09-11): **(C, 둘 다)** — 단건 조회(`GET .../addresses/{addressId}`)와 사용자별 목록 조회(`GET .../addresses`) 모두 제공.
- Q-003 (2026-09-11): **(A)** — `User`와 동일한 컨벤션으로 `addr-XXXXXXXX`(prefix + UUID 8자리) 채택.
- Q-004 (2026-09-11): **(B, 구체적 규칙)** — 우편번호: 필수, 숫자 5자리 고정(현행 한국 우편번호 표준). 도로명주소: 필수, 공백 불가. 상세주소: 선택(공백/누락 허용). 위반 시 400, 데이터 변경 없음(AC-005).
- Q-005 (2026-09-11): **(A)** — 기존 `User` 엔드포인트 컨벤션(404 + `{"error": "..."}`)을 그대로 적용. 등록·목록 조회 모두 대상 `userId`가 없으면 404(AC-004, AC-015).
- Q-006 (2026-09-11): **(A, cascade 삭제)** — 사용자 삭제 시 그 사용자의 주소를 모두 함께 삭제(AC-011). 고아 레코드를 남기지 않는 편이 일반적인 기본 동작이라 판단.
- Q-007 (2026-09-11): **(B 확장)** — 응답 바디는 id + userId + 세 주소 필드(우편번호, 도로명주소, 상세주소)로 통일. 타임스탬프 등은 포함하지 않음(Non-Goals).
- Q-008 (2026-09-11): **(A, 무제한)** — 사용자 1명당 주소 개수 상한 없음(AC-012).

## Sign-off
- [x] All AC atomic and testable.
- [x] All `Q-NNN` resolved or deferred-with-rationale.
- [x] Source recorded.
- [x] Reviewed by user on 2026-09-11.
