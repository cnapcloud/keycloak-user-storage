# ADR-001: Address–User association via plain FK column, not a JPA relationship

- Status: accepted
- Date: 2026-09-11
- Feature: 2026-09-11-address-management

## Context
`01-spec.md`의 `Address`는 이 저장소에서 처음 등장하는, 자체 PK를 갖고 `User`를 참조하는
진짜 1:N 자식 엔티티다(AC-001). 기존 코드베이스에는 참고할 다건-관계 전례가 둘 있지만
둘 다 이 모양과 다르다: `User.attributes`는 `@ElementCollection` map(자체 엔티티/PK 없음)이고,
`CredentialData`는 자체 PK 없이 `User.id`를 그대로 재사용하는 1:1 관계다. `src/main/java`
전체에 `@OneToMany`/`@ManyToOne`가 단 하나도 없다.

`AC-011`(사용자 삭제 시 주소 cascade 삭제), `AC-006/010/014`(주소를 `userId` 소유 범위로
조회/검증)를 구현해야 한다.

## Decision
`Address`에 JPA 연관관계 애노테이션을 쓰지 않는다. 대신 평범한 컬럼 `private String userId`를
두고, `AddressRepository`에 파생 쿼리 메서드(`findByUserId`, `findByIdAndUserId`,
`deleteByUserId`)로 조회·소유자 검증·cascade 삭제를 전부 서비스 레이어에서 명시적으로 처리한다.
`User.java`는 전혀 건드리지 않는다.

## Alternatives considered
- `Address.user`에 `@ManyToOne` + `User.addresses`에 `@OneToMany(mappedBy="user", cascade=ALL, orphanRemoval=true)` — Hibernate가 cascade 삭제를 자동 처리해줘서 `UserServiceImpl` 수정이 줄어들지만, (1) 이 코드베이스에 실제 JPA 연관관계 전례가 전혀 없어 기존 컨벤션에서 벗어남(architect hard rule: "다른 Spring 프로젝트의 의견을 들여오지 않는다"), (2) `User.java`를 수정해야 해서 `01-spec.md` "Data Impact"의 "User 테이블/엔티티 변경 없음" 전제와 어긋남, (3) 양방향 관계의 지연로딩/직렬화 함정(Jackson 무한 재귀 등)을 새로 끌어들임 — 기각.

## Consequences
- Positive: `User.java` 무변경. `CredentialData`와 동일하게 "명시적 FK 컬럼 + 매뉴얼 쿼리" 스타일을 유지해 코드베이스 일관성 유지. cascade 로직이 `UserServiceImpl.deleteUser()`에 한 줄로 드러나 추적이 쉬움.
- Negative / follow-up: 참조 무결성이 DB FK 제약이 아니라 애플리케이션 코드로만 보장됨 — 향후 `AddressRepository`를 거치지 않는 삭제 경로가 생기면 고아 레코드 위험. 현재 범위에서는 삭제 경로가 `UserServiceImpl.deleteUser()` 하나뿐이라 위험 낮음.
- Gates affected: none (새 ArchUnit 규칙 불필요 — 기존 A1~A8이 그대로 적용됨).
