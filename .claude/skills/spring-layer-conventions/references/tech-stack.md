# Tech Stack

## Core
- **Java 17** — 언어 버전
- **Spring Boot 3.3.4** — 웹 프레임워크 (spring-boot-starter-web, spring-boot-starter-data-jpa)
- **Gradle** — 빌드 도구 (JVM args: `-Xmx2048m`)

## Persistence
- **H2 (in-memory)** — `jdbc:h2:mem:testdb`, DDL: `hibernate.ddl-auto=update`
- **Spring Data JPA** — 기본 CRUD
- **JPA Criteria API** — 동적 쿼리 (`UserRepositoryImpl`)

## Utilities
- **Lombok** — `@Data`, `@Slf4j` 등 보일러플레이트 제거
- **Jackson 2.15.2** — JSON 직렬화, `FAIL_ON_UNKNOWN_PROPERTIES=false`, `NON_NULL` 포함만 출력

## 날짜/시간
- **타입**: `LocalDateTime` (`java.sql.Timestamp` 사용 금지)
- **직렬화 포맷**: `yyyy-MM-dd'T'HH:mm:ss` (ISO 8601, T 구분자)
- **직렬화 클래스**: `CustomLocalDateTimeSerializer` — `JacksonConfig`에 전역 등록
- **역직렬화 패턴**: `DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")`

## 테스트
- **`@SpringBootTest(webEnvironment = RANDOM_PORT)`** — 실서버 기동 통합 테스트
- **`TestRestTemplate`** — HTTP 클라이언트 (`spring-boot-starter-test` 포함)
- **PATCH 지원 필수 설정**: 기본 `HttpURLConnection`은 PATCH 미지원
  → `testImplementation 'org.apache.httpcomponents.client5:httpclient5'` 추가 후
  → `@BeforeAll`에서 `HttpComponentsClientHttpRequestFactory`로 교체
  ```java
  @BeforeAll
  static void enablePatchSupport(@Autowired TestRestTemplate restTemplate) {
      restTemplate.getRestTemplate().setRequestFactory(
          new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault()));
  }
  ```
- **null 값 포함 요청 바디**: Jackson `NON_NULL` 설정으로 Map null 값이 직렬화 제외됨
  → `{"key": null}` 전송이 필요한 경우 raw JSON String으로 직접 전달

## Infra
- **Docker** — `openjdk:17-slim` 기반, `spring` 사용자 계정으로 실행
- **Harbor** — 내부 레지스트리 `harbor.kind.internal/library/keycloak-user-storage`
- **이미지 태그** — git short SHA (7자)

## 내부 저장소
- `http://reposilite.kind.internal/releases` — 외부망에서는 의존성 해결 불가
