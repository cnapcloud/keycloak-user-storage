# ADR-003: Manual service-layer validation for postal code / road address, not Bean Validation

- Status: accepted
- Date: 2026-09-11
- Feature: 2026-09-11-address-management

## Context
`AC-005`(우편번호 5자리 미검증 시 400), `AC-013`(도로명주소 공백 시 400)은 입력 검증을 요구한다.
`src/main/java` 전체를 검색한 결과 `@NotNull`/`@NotBlank`/`@Size`/`@Pattern`/`jakarta.validation`/
`@Valid` 사용 사례가 **전무**하다. 기존 검증(예: `UserServiceImpl.createUser()`의 username 중복
체크)은 전부 Service 메서드 안에서 수동 `if` + `ResponseStatusException`으로 이뤄진다.
architect hard rule은 "이 repo의 기존 컨벤션을 따른다 ... 다른 Spring 프로젝트의 의견을
들여오지 않는다"고 명시한다.

## Decision
`AddressServiceImpl`에 `validate(Address)` private 메서드를 두고, 우편번호가
`\\d{5}` 정규식에 맞지 않으면(null 포함) `ResponseStatusException(BAD_REQUEST, "...")`,
도로명주소가 `null`이거나 `isBlank()`면 각각 별도 예외를 던진다. Bean Validation
애노테이션(`@NotBlank`, `@Pattern`)과 컨트롤러의 `@Valid`는 도입하지 않는다.

## Alternatives considered
- Jakarta Bean Validation(`spring-boot-starter-validation` + 애노테이션 + `@Valid`) — 더 선언적이고 Spring 생태계 표준이지만, 이 코드베이스에 전례가 전무해 hard rule("다른 Spring 프로젝트의 의견을 들여오지 않는다")에 정면으로 위배. 신규 의존성 추가도 필요(내부 Maven 저장소 확인 필요) — 이번 스펙 범위를 벗어남.

## Consequences
- Positive: 기존 `UserServiceImpl` 검증 스타일과 완전히 일관됨. 신규 의존성 없음.
- Negative / follow-up: 검증 규칙이 늘어나면(향후 필드 추가 등) `if` 체인이 길어질 수 있음 — 그 시점에 Bean Validation 도입을 재검토할 수 있으나, 그때도 이 코드베이스 전체에 걸친 별도 ADR + 하위호환 검토가 필요.
- Gates affected: none.
