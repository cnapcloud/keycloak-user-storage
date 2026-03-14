# Testing

## 통합 테스트 기본 구조

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

**필수 의존성 (`build.gradle`):**
```groovy
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.apache.httpcomponents.client5:httpclient5'  // PATCH 지원
```

## 테스트 원칙
- 실서버 기동 후 seed data(`user-data.json`, `credential-data.json`) 기준으로 검증
- 쓰기 테스트는 반드시 cleanup (생성 후 삭제)
- 상태 변경 테스트는 `@Order`로 순서 보장
- 읽기 전용 테스트는 어떤 순서든 무관

## JSON 요청 헬퍼

```java
private HttpEntity<String> jsonEntity(String json) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(json, headers);
}
```

**null 값 포함 요청 (PATCH attributes 삭제):**
```java
// Map.of()는 null 값 불허, NON_NULL 설정으로 직렬화 제외됨
// → raw JSON String으로 전달
rest.exchange("/user/" + id + "/attributes", HttpMethod.PATCH,
    jsonEntity("{\"lastLoginDate\":null}"), Void.class);
```

## 응답 파싱 패턴

```java
// 단일 객체
ResponseEntity<Map<String, Object>> response = rest.exchange(
    "/user/" + id, HttpMethod.GET, null,
    new ParameterizedTypeReference<>() {});

// 배열
ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
    "/user/search?username=admin", HttpMethod.GET, null,
    new ParameterizedTypeReference<>() {});
```

## USP 핵심 검증 항목

| 항목 | 검증 방법 |
|------|-----------|
| `attributes` null 금지 | `assertNotNull(user.get("attributes"))` |
| `createdDate` ISO 8601 T 포맷 | `assertTrue(date.contains("T"))` |
| `id` 포맷 | `assertTrue(id.startsWith("u-"))` |
| `additionParameters` JSON 문자열 | `assertTrue(value instanceof String)` |
| 검색 결과 없음 → 빈 배열 | `assertTrue(list.isEmpty())` + 상태코드 200 확인 |
| PATCH null → 키 삭제 | PATCH 후 GET, `assertFalse(attrs.containsKey(key))` |
| PATCH 미포함 키 유지 | PATCH 후 GET, `assertNotNull(attrs.get(otherKey))` |
