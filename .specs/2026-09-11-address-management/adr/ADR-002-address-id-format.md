# ADR-002: Address id format `addr-XXXXXXXX`, generated in the service layer

- Status: accepted
- Date: 2026-09-11
- Feature: 2026-09-11-address-management

## Context
`01-spec.md` `Q-003`은 이미 `(A)` — `User`와 동일한 prefix+UUID8 컨벤션을 채택하기로 확정했다.
이 ADR은 그 결정을 구현 위치까지 포함해 architecturally 기록한다: `User.id`는
`@GeneratedValue` 없이 `UserServiceImpl.createUser()`에서
`"u-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)`로 수동 생성된다.

## Decision
`Address.id`도 동일한 방식으로 `AddressServiceImpl.createAddress()`에서
`"addr-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)`로 생성한다.
`@Id` 필드에 `@GeneratedValue`를 붙이지 않는다.

## Alternatives considered
- DB auto-increment(`@GeneratedValue(strategy = IDENTITY)`, `Long id`) — H2 기본 기능이라 구현은 더 간단하지만, 응답 바디에 노출되는 id 타입/포맷이 `User`(`u-XXXXXXXX`, String)와 달라져 API 일관성이 깨짐. `Q-003`에서 이미 기각됨.
- UUID 전체(36자) 그대로 사용 — `User`의 8자리 축약 컨벤션과 불일치, 응답 바디만 불필요하게 길어짐 — 기각.

## Consequences
- Positive: `User`/`Address` 두 리소스의 id 포맷·생성 위치가 대칭적이라 클라이언트와 구현자 모두에게 예측 가능.
- Negative / follow-up: 8자리 접두 랜덤이라 이론상 충돌 가능(현재 `User`도 동일 리스크를 이미 감수 중 — 신규 리스크 아님).
- Gates affected: none.
