---
name: spring-integration-testing
description: Full-stack @SpringBootTest(RANDOM_PORT) integration test patterns — TestRestTemplate, PATCH support, raw-JSON null bodies — plus this project's USP domain invariants. Opt-in only per spring-unit-testing's default policy — use only when the user explicitly asks for one.
---
# Integration testing (full-stack)

Ported from the former `.claude/conventions/04`. H2 in-memory — **no Testcontainers**.

**Opt-in only.** `spring-unit-testing` is the default for the `/build` red step; write a
test with this skill only when the user explicitly asks for an integration test for the
task at hand — never automatically, not even for a "happy path" AC.

**Package**: any new integration test goes under `.../integration/` (e.g.
`src/test/java/com/keycloak/userstorage/integration/XxxIntegrationTest.java`), since it
targets a whole request flow rather than one class — see `spring-unit-testing`'s
"Package layout". Existing `UserStorageIntegrationTest`/`AddressIntegrationTest` stay at
the root package; this only applies to new files going forward.

## 통합 테스트 기본 구조 (일반 — 어떤 Spring Boot 프로젝트든 동일)
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class XxxIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @BeforeAll
    static void enablePatchSupport(@Autowired TestRestTemplate restTemplate) {
        // PATCH 지원: HttpURLConnection → Apache HttpClient5
        restTemplate.getRestTemplate().setRequestFactory(
            new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault()));
    }
}
```

**필수 의존성 (`build.gradle`):** PATCH 요청을 쓰는 프로젝트라면 어디든 필요.
```groovy
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.apache.httpcomponents.client5:httpclient5'  // PATCH 지원
```

## 테스트 원칙 (일반)
- 쓰기 테스트는 반드시 cleanup (생성 후 삭제)
- 상태 변경 테스트는 `@Order`로 순서 보장
- 읽기 전용 테스트는 어떤 순서든 무관
- 새 `@Test`마다 `@Tag("AC-NNN")` + `@DisplayName("<T-id>: given …, when …, then …")`
- 프로젝트에 seed data가 있으면 그 값을 기준으로 검증 — 이 repo는 `user-data.json`,
  `credential-data.json`이 시드 소스 (`_baseline.json`의 시드 drift 이력 참고)

## JSON 요청 헬퍼 (일반)
```java
private HttpEntity<String> jsonEntity(String json) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(json, headers);
}
```

**null 값 포함 요청 (일반 기법 — Jackson `NON_NULL` 설정을 쓰는 프로젝트라면 어디든 해당):**
`Map.of()`는 null 값을 허용하지 않고, `NON_NULL` 설정 때문에 `Map` 직렬화 시 null 값이
빠지므로 raw JSON 문자열로 요청 바디를 직접 써야 한다. 아래는 이 프로젝트의 PATCH
attributes 삭제 예시.
```java
rest.exchange("/user/" + id + "/attributes", HttpMethod.PATCH,
    jsonEntity("{\"lastLoginDate\":null}"), Void.class);
```

## 응답 파싱 패턴 (일반)
```java
// 단일 객체
ResponseEntity<Map<String, Object>> response = rest.exchange(
    "/user/" + id, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

// 배열
ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
    "/user/search?username=admin", HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
```

## Gradle 단일 테스트 실행 (일반)
```bash
./gradlew test --tests 'com.keycloak.userstorage.integration.XxxIntegrationTest.methodName'
```

---

## 프로젝트 도메인 불변식 (keycloak-user-storage 전용 — 다른 프로젝트로 옮길 때 이 표는 버리고
그 프로젝트의 도메인 규칙으로 새로 채운다)

| 항목 | 검증 방법 |
|------|-----------|
| `attributes` null 금지 | `assertNotNull(user.get("attributes"))` |
| `createdDate` ISO 8601 T 포맷 | `assertTrue(date.contains("T"))` |
| `id` 포맷 | `assertTrue(id.startsWith("u-"))` |
| `additionParameters` JSON 문자열 | `assertTrue(value instanceof String)` |
| 검색 결과 없음 → 빈 배열 | `assertTrue(list.isEmpty())` + 상태코드 200 |
| PATCH null → 키 삭제 | PATCH 후 GET, `assertFalse(attrs.containsKey(key))` |
| PATCH 미포함 키 유지 | PATCH 후 GET, `assertNotNull(attrs.get(otherKey))` |
